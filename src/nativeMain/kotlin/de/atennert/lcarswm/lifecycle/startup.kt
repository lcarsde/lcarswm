package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.AppMenuMessageHandler
import de.atennert.lcarswm.HOME_CONFIG_DIR_PROPERTY
import de.atennert.lcarswm.RootWindowPropertyHandler
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.TextAtomReader
import de.atennert.lcarswm.drawing.*
import de.atennert.lcarswm.events.*
import de.atennert.lcarswm.keys.KeyConfiguration
import de.atennert.lcarswm.keys.KeyManager
import de.atennert.lcarswm.keys.KeySessionManager
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorManagerImpl
import de.atennert.lcarswm.settings.SettingsReader
import de.atennert.lcarswm.signal.Signal
import de.atennert.lcarswm.signal.SignalHandler
import de.atennert.lcarswm.system.MessageQueue
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.window.*
import exitState
import kotlinx.atomicfu.AtomicRef
import kotlinx.cinterop.*
import platform.posix.waitpid
import staticLogger
import xlib.*

private var wmDetected = false

const val ROOT_WINDOW_MASK = SubstructureRedirectMask or StructureNotifyMask or PropertyChangeMask or
        FocusChangeMask or KeyPressMask or KeyReleaseMask

private const val XRANDR_MASK = RRScreenChangeNotifyMask or RROutputChangeNotifyMask or
        RRCrtcChangeNotifyMask or RROutputPropertyNotifyMask

fun startup(system: SystemApi, logger: Logger): RuntimeResources? {
    val signalHandler = SignalHandler(system)

    wmDetected = false
    if (!system.openDisplay()) {
        logger.logError("::runWindowManager::got no display")
        return null
    }
    system.closeWith { closeDisplay() }

    val randrHandlerFactory = RandrHandlerFactory(system, logger)

    val atomLibrary = AtomLibrary(system)

    val keyManager = KeyManager(system)

    val eventBuffer = EventBuffer(system)

    listOf(Signal.USR1, Signal.USR2, Signal.TERM, Signal.INT, Signal.HUP, Signal.PIPE, Signal.CHLD, Signal.TTIN, Signal.TTOU)
        .forEach { signalHandler.addSignalCallback(it, staticCFunction { signal ->
            handleSignal(
                signal,
                exitState
            )
        }) }

    val screen = system.defaultScreenOfDisplay()?.pointed
    if (screen == null) {
        logger.logError("::runWindowManager::got no screen")
        return null
    }

    system.synchronize(false)

    setDisplayEnvironment(system)

    val rootWindowPropertyHandler = RootWindowPropertyHandler(
        logger,
        system,
        screen.root,
        atomLibrary,
        eventBuffer
    )

    val eventTime = EventTime(system, eventBuffer, atomLibrary, rootWindowPropertyHandler)

    if (!rootWindowPropertyHandler.becomeScreenOwner(eventTime)) {
        logger.logError("::runWindowManager::Detected another active window manager")
        return null
    }

    system.sync(false)
    system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

    system.selectInput(screen.root, ROOT_WINDOW_MASK)

    system.sync(false)

    if (wmDetected) {
        logger.logError("::runWindowManager::Detected another active window manager")
        return null
    }
    system.closeWith { selectInput(screen.root, NoEventMask) }

    system.setErrorHandler(staticCFunction { _, err -> staticLogger?.logError("::runWindowManager::error code: ${err?.pointed?.error_code}"); 0 })

    rootWindowPropertyHandler
        .closeWith(RootWindowPropertyHandler::unsetWindowProperties)
        .setSupportWindowProperties()

    eventTime.resetEventTime()

    val settings = loadSettings(logger, system)
    if (settings == null) {
        logger.logError("::runWindowManager::unable to load settings")
        return null
    }

    logger.logDebug("::runWindowManager::Screen size: ${screen.width}/${screen.height}, root: ${screen.root}")

    val monitorManager = MonitorManagerImpl(system, screen.root)

    val fontProvider = FontProvider(system, settings.generalSettings, system.defaultScreenNumber())
    val colorHandler = Colors(system, screen)
    val uiDrawer =
        RootWindowDrawer(system, system, monitorManager, screen, colorHandler, settings.generalSettings, fontProvider)

    keyManager.ungrabAllKeys(screen.root)

    val toggleSessionManager = KeySessionManager(logger, system)

    val keyConfiguration = KeyConfiguration(
        system,
        settings.keyBindings,
        keyManager,
        toggleSessionManager,
        screen.root
    )

    system.sync(false)

    val focusHandler = WindowFocusHandler()

    val frameDrawer = FrameDrawer(system, system, focusHandler, fontProvider, colorHandler, screen)

    val windowCoordinator = ActiveWindowCoordinator(system, monitorManager, frameDrawer)

    val screenChangeHandler =
        setupRandr(
            system,
            randrHandlerFactory,
            monitorManager,
            windowCoordinator,
            uiDrawer,
            screen.root
        )

    val windowNameReader = TextAtomReader(system, atomLibrary)

    val appMenuHandler = AppMenuHandler(system, atomLibrary, monitorManager, screen.root)
    val statusBarHandler = StatusBarHandler(system, atomLibrary, monitorManager, screen.root)

    val appMenuMessageQueue = MessageQueue(system, "/lcarswm-app-menu-messages", MessageQueue.Mode.READ)

    val windowList = WindowList()

    val appMenuMessageHandler = AppMenuMessageHandler(
        logger,
        system,
        atomLibrary,
        windowList,
        focusHandler
    )

    val windowRegistration = WindowHandler(
        system,
        logger,
        windowCoordinator,
        focusHandler,
        atomLibrary,
        screen,
        windowNameReader,
        appMenuHandler,
        statusBarHandler,
        windowList
    )

    val keyboardGrabber = object : FocusObserver {
        private var grabActive = false
        private var grabTime: Time = CurrentTime.convert()

        override fun invoke(activeWindow: Window?, oldWindow: Window?, toggleSessionActive: Boolean) {
            if (toggleSessionActive != grabActive) {
                grabActive = toggleSessionActive

                if (toggleSessionActive) {
                    grabTime = eventTime.lastEventTime
                    system.grabKeyboard(rootWindowPropertyHandler.ewmhSupportWindow, grabTime)
                } else {
                    system.ungrabKeyboard(
                        if (eventTime.lastEventTime > grabTime)
                            eventTime.lastEventTime
                        else
                            CurrentTime.convert()
                    )
                }
            }
        }
    }

    focusHandler.registerObserver(keyboardGrabber)

    focusHandler.registerObserver { activeWindow, _, toggleSessionActive ->
        if (!toggleSessionActive) {
            if (activeWindow != null) {
                logger.logDebug("::startup::set input focus to $activeWindow")
                system.setInputFocus(activeWindow, RevertToNone, eventTime.lastEventTime)
            } else {
                logger.logDebug("::startup::set input focus to root")
                system.setInputFocus(screen.root, RevertToPointerRoot, eventTime.lastEventTime)
            }
        }
    }

    focusHandler.registerObserver { activeWindow, oldWindow, _ ->
        listOf(oldWindow, activeWindow).forEach {
            it?.let { ow ->
                windowRegistration[ow]?.let { fw ->
                    frameDrawer.drawFrame(fw, windowCoordinator.getMonitorForWindow(ow))
                }
            }
        }
    }

    focusHandler.registerObserver { activeWindow, _, _ ->
        activeWindow?.let { windowCoordinator.stackWindowToTheTop(it) }
    }

    focusHandler.registerObserver(appMenuHandler.focusObserver)

    monitorManager.registerObserver(appMenuHandler)
    monitorManager.registerObserver(statusBarHandler)

    windowList.registerObserver(appMenuHandler.windowListObserver)

    toggleSessionManager.addListener(focusHandler.keySessionListener)

    val eventManager = createEventManager(
        system,
        logger,
        monitorManager,
        windowRegistration,
        windowCoordinator,
        focusHandler,
        keyManager,
        toggleSessionManager,
        uiDrawer,
        atomLibrary,
        screenChangeHandler,
        keyConfiguration,
        windowNameReader,
        appMenuHandler,
        statusBarHandler,
        frameDrawer,
        windowList,
        screen.root
    )

    setupScreen(
        system,
        screen.root,
        rootWindowPropertyHandler,
        windowRegistration
    )

    val xEventResources =
        XEventResources(eventManager, eventTime, eventBuffer)
    val appMenuResources =
        AppMenuResources(appMenuMessageHandler, appMenuMessageQueue)

    return RuntimeResources(xEventResources, appMenuResources)
}

/**
 * Signal handler for usual stuff.
 */
private fun handleSignal(signalValue: Int, exitState: AtomicRef<Int?>) {
    val signal = Signal.values().single { it.signalValue == signalValue }
    staticLogger?.logDebug("::handleSignal::signal: $signal")
    when (signal) {
        Signal.USR1 -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.USR2 -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.TTIN -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.TTOU -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.CHLD -> while (waitpid(-1, null, platform.posix.WNOHANG) > 0);
        Signal.TERM -> exitState.value = 0
        Signal.INT -> exitState.value = 0
        else -> exitState.value = 1
    }
}

private fun setDisplayEnvironment(system: SystemApi) {
    val displayString = system.getDisplayString()
    system.setenv("DISPLAY", displayString)
}

private fun setupScreen(
    system: SystemApi,
    rootWindow: Window,
    rootWindowPropertyHandler: RootWindowPropertyHandler,
    windowRegistration: WindowHandler
) {
    system.grabServer()

    val returnedWindows = ULongArray(1)
    val returnedParent = ULongArray(1)
    val topLevelWindows = nativeHeap.allocPointerTo<ULongVarOf<Window>>()
    val topLevelWindowCount = UIntArray(1)

    system.queryTree(
        rootWindow, returnedWindows.toCValues(), returnedParent.toCValues(),
        topLevelWindows.ptr,
        topLevelWindowCount.toCValues()
    )

    ULongArray(topLevelWindowCount[0].convert()) { topLevelWindows.value!![it] }
        .filter { childId -> childId != rootWindow }
        .filter { childId -> childId != rootWindowPropertyHandler.ewmhSupportWindow }
        .forEach { childId ->
            windowRegistration.addWindow(childId, true)
        }

    nativeHeap.free(topLevelWindows)
    system.ungrabServer()
}

/**
 * Load the key configuration from the users key configuration file.
 */
private fun loadSettings(logger: Logger, systemApi: SystemApi): SettingsReader? {
    val configPathBytes = systemApi.getenv(HOME_CONFIG_DIR_PROPERTY) ?: return null
    val configPath = configPathBytes.toKString()

    return SettingsReader(logger, systemApi, configPath)
}

/**
 * @return RANDR base value
 */
private fun setupRandr(
    system: SystemApi,
    randrHandlerFactory: RandrHandlerFactory,
    monitorManager: MonitorManager,
    windowCoordinator: WindowCoordinator,
    uiDrawer: UIDrawing,
    rootWindowId: Window
): XEventHandler {
    val screenChangeHandler = randrHandlerFactory.createScreenChangeHandler(monitorManager, windowCoordinator, uiDrawer)
    val fakeEvent = nativeHeap.alloc<XEvent>()
    screenChangeHandler.handleEvent(fakeEvent)
    nativeHeap.free(fakeEvent)

    system.rSelectInput(rootWindowId, XRANDR_MASK.convert())

    return screenChangeHandler
}

/**
 * Create the event handling for the event loop.
 */
private fun createEventManager(
    system: SystemApi,
    logger: Logger,
    monitorManager: MonitorManager,
    windowRegistration: WindowRegistration,
    windowCoordinator: WindowCoordinator,
    focusHandler: WindowFocusHandler,
    keyManager: KeyManager,
    toggleSessionManager: KeySessionManager,
    uiDrawer: UIDrawing,
    atomLibrary: AtomLibrary,
    screenChangeHandler: XEventHandler,
    keyConfiguration: KeyConfiguration,
    textAtomReader: TextAtomReader,
    appMenuHandler: AppMenuHandler,
    statusBarHandler: StatusBarHandler,
    frameDrawer: FrameDrawer,
    windowList: WindowList,
    rootWindowId: Window
): EventDistributor {

    return EventDistributor.Builder(logger)
        .addEventHandler(ConfigureRequestHandler(system, logger, windowRegistration, windowCoordinator, appMenuHandler, statusBarHandler))
        .addEventHandler(DestroyNotifyHandler(logger, windowRegistration, appMenuHandler, statusBarHandler))
        .addEventHandler(ButtonPressHandler(logger, system, windowList, focusHandler))
        .addEventHandler(KeyPressHandler(logger, keyManager, keyConfiguration, toggleSessionManager, monitorManager, windowCoordinator, focusHandler, uiDrawer))
        .addEventHandler(KeyReleaseHandler(logger, system, focusHandler, keyManager, keyConfiguration, toggleSessionManager, atomLibrary))
        .addEventHandler(MapRequestHandler(logger, windowRegistration))
        .addEventHandler(UnmapNotifyHandler(logger, windowRegistration, uiDrawer))
        .addEventHandler(screenChangeHandler)
        .addEventHandler(ReparentNotifyHandler(logger, windowRegistration))
        .addEventHandler(ClientMessageHandler(logger, atomLibrary))
        .addEventHandler(SelectionClearHandler(logger))
        .addEventHandler(MappingNotifyHandler(logger, keyManager, keyConfiguration, rootWindowId))
        .addEventHandler(PropertyNotifyHandler(atomLibrary, windowRegistration, textAtomReader, frameDrawer, windowCoordinator, rootWindowId))
        .build()
}
