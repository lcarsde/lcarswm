import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.drawing.Colors
import de.atennert.lcarswm.drawing.FrameDrawer
import de.atennert.lcarswm.drawing.RootWindowDrawer
import de.atennert.lcarswm.drawing.UIDrawing
import de.atennert.lcarswm.events.*
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorManagerImpl
import de.atennert.lcarswm.signal.Signal
import de.atennert.lcarswm.signal.SignalHandler
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.PosixApi
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.system.api.WindowUtilApi
import de.atennert.lcarswm.window.*
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import platform.posix.WNOHANG
import platform.posix.waitpid
import xlib.*

private var wmDetected = false

// this is a somewhat dirty hack to hand the logger to staticCFunction as error handler
private var staticLogger: Logger? = null

private val exitState = atomic<Int?>(null)

const val ROOT_WINDOW_MASK = SubstructureRedirectMask or StructureNotifyMask or PropertyChangeMask or
        EnterWindowMask or LeaveWindowMask or FocusChangeMask or
        ButtonPressMask or ButtonReleaseMask or
        KeyPressMask or KeyReleaseMask

const val XRANDR_MASK = RRScreenChangeNotifyMask or RROutputChangeNotifyMask or
        RRCrtcChangeNotifyMask or RROutputPropertyNotifyMask

// the main method apparently must not be inside of a package so it can be compiled with Kotlin/Native
fun main() {
    val system = SystemFacade()
    val logger = FileLogger(system, LOG_FILE_PATH)

    runWindowManager(system, logger)

    system.exit(exitState.value ?: -1)
}

fun runWindowManager(system: SystemApi, logger: Logger) {
    logger.logInfo("::runWindowManager::start lcarswm initialization")
    exitState.value = null
    staticLogger = logger

    memScoped {
        val signalHandler = SignalHandler(system)

        wmDetected = false
        if (!system.openDisplay()) {
            logger.logError("::runWindowManager::got no display")
            logger.close()
            signalHandler.cleanup()
            return
        }

        val randrHandlerFactory = RandrHandlerFactory(system, logger)

        val atomLibrary = AtomLibrary(system)

        val keyManager = KeyManager(system)

        val eventBuffer = EventBuffer(system)

        listOf(Signal.USR1, Signal.USR2, Signal.TERM, Signal.INT, Signal.HUP, Signal.PIPE, Signal.CHLD, Signal.TTIN, Signal.TTOU)
            .forEach { signalHandler.addSignalCallback(it, staticCFunction { signal -> handleSignal(signal) }) }

        val screen = system.defaultScreenOfDisplay()?.pointed
        if (screen == null) {
            logger.logError("::runWindowManager::got no screen")
            logger.close()
            system.closeDisplay()
            signalHandler.cleanup()
            return
        }

        system.synchronize(false)

        setDisplayEnvironment(system)

        val rootWindowPropertyHandler = RootWindowPropertyHandler(logger, system, screen.root, atomLibrary, eventBuffer)

        val eventTime = EventTime(system, eventBuffer, atomLibrary, rootWindowPropertyHandler)

        if (!rootWindowPropertyHandler.becomeScreenOwner(eventTime)) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
            return
        }

        system.sync(false)
        system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

        system.selectInput(screen.root, ROOT_WINDOW_MASK)
        system.sync(false)

        if (wmDetected) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
            return
        }

        system.setErrorHandler(staticCFunction { _, err -> staticLogger?.logError("::runWindowManager::error code: ${err?.pointed?.error_code}"); 0 })

        rootWindowPropertyHandler.setSupportWindowProperties()

        eventTime.resetEventTime()

        val keyConfigurationProvider = loadKeyConfiguration(system) ?: return

        logger.logDebug("::runWindowManager::Screen size: ${screen.width}/${screen.height}, root: ${screen.root}")

        val monitorManager = MonitorManagerImpl(system, screen.root)

        val colorHandler = Colors(system, screen)
        val uiDrawer = RootWindowDrawer(system, monitorManager, screen, colorHandler)

        keyManager.ungrabAllKeys(screen.root)
        keyManager.grabInternalKeys(screen.root)

        val keyConfiguration = KeyConfiguration(system, keyConfigurationProvider, keyManager, screen.root)

        system.sync(false)

        val focusHandler = WindowFocusHandler()

        val frameDrawer = FrameDrawer(system, system, focusHandler, colorHandler, system.defaultScreenNumber(), screen)

        val windowCoordinator = ActiveWindowCoordinator(system, monitorManager, frameDrawer)

        val screenChangeHandler = setupRandr(system, randrHandlerFactory, monitorManager, windowCoordinator, uiDrawer, screen.root)

        val windowNameReader = TextAtomReader(system, atomLibrary)

        val appMenuHandler = AppMenuHandler(system, atomLibrary, monitorManager, screen.root)

        val windowList = WindowList()

        val windowRegistration = WindowHandler(
            system,
            logger,
            windowCoordinator,
            focusHandler,
            atomLibrary,
            screen,
            windowNameReader,
            appMenuHandler,
            windowList
        )

        focusHandler.registerObserver { activeWindow, _ ->
            if (activeWindow != null) {
                system.setInputFocus(activeWindow, RevertToParent, eventTime.lastEventTime)
            } else {
                system.setInputFocus(screen.root, RevertToPointerRoot, eventTime.lastEventTime)
            }
        }

        focusHandler.registerObserver { activeWindow, oldWindow ->
            listOf(oldWindow, activeWindow).forEach {
                it?.let { ow -> windowRegistration[ow]?.let { fw ->
                    frameDrawer.drawFrame(fw, windowCoordinator.getMonitorForWindow(ow))
                } }
            }
        }

        focusHandler.registerObserver { activeWindow, _ ->
            activeWindow?.let { windowCoordinator.stackWindowToTheTop(it) }
        }

        monitorManager.registerObserver(appMenuHandler)

        windowList.registerObserver(appMenuHandler.windowListObserver)

        val eventManager = createEventManager(
            system,
            logger,
            monitorManager,
            windowRegistration,
            windowCoordinator,
            focusHandler,
            keyManager,
            uiDrawer,
            atomLibrary,
            screenChangeHandler,
            keyConfiguration,
            windowNameReader,
            appMenuHandler,
            frameDrawer,
            screen.root
        )

        setupScreen(system, screen.root, rootWindowPropertyHandler, windowRegistration)

        eventLoop(system, eventManager, eventTime, eventBuffer)

        system.sync(false)

        shutdown(system, uiDrawer, colorHandler, screen.root, logger, rootWindowPropertyHandler, keyManager, signalHandler, frameDrawer)
    }
}

/**
 * Signal handler for usual stuff.
 */
private fun handleSignal(signalValue: Int) {
    val signal = Signal.values().single { it.signalValue == signalValue }
    staticLogger?.logDebug("::handleSignal::signal: $signal")
    when (signal) {
        Signal.USR1 -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.USR2 -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.TTIN -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.TTOU -> staticLogger?.logInfo("Ignoring signal $signal")
        Signal.CHLD -> while (waitpid(-1, null, WNOHANG) > 0);
        Signal.TERM -> exitState.value = 0
        Signal.INT  -> exitState.value = 0
        else -> exitState.value = 1
    }
}

/**
 * Full shutdown routines.
 */
private fun shutdown(
    system: SystemApi,
    rootWindowDrawer: RootWindowDrawer,
    colors: Colors,
    rootWindow: Window,
    logger: Logger,
    rootWindowPropertyHandler: RootWindowPropertyHandler,
    keyManager: KeyManager,
    signalHandler: SignalHandler,
    frameDrawer: FrameDrawer
) {
    rootWindowDrawer.cleanupGraphicsContexts()
    frameDrawer.close()
    colors.cleanupColorMap()

    system.selectInput(rootWindow, NoEventMask)
    rootWindowPropertyHandler.unsetWindowProperties()

    keyManager.cleanup()

    cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
}

fun cleanup(
    logger: Logger,
    windowUtils: WindowUtilApi,
    rootWindowPropertyHandler: RootWindowPropertyHandler,
    signalHandler: SignalHandler
) {
    rootWindowPropertyHandler.destroySupportWindow()

    windowUtils.closeDisplay()

    signalHandler.cleanup()

    staticLogger = null
    logger.logInfo("::runWindowManager::lcarswm stopped")
    logger.close()
}

fun setDisplayEnvironment(system: SystemApi) {
    val displayString = system.getDisplayString()
    system.setenv("DISPLAY", displayString)
}

fun setupScreen(
    system: SystemApi,
    rootWindow: Window,
    rootWindowPropertyHandler: RootWindowPropertyHandler,
    windowRegistration: WindowHandler) {
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
fun loadKeyConfiguration(posixApi: PosixApi): ConfigurationProvider? {
    val configPathBytes = posixApi.getenv(HOME_CONFIG_DIR_PROPERTY) ?: return null
    val configPath = configPathBytes.toKString()
    val keyConfiguration = "$configPath$KEY_CONFIG_FILE"

    return ConfigurationProvider(posixApi, keyConfiguration)
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
    uiDrawer: UIDrawing,
    atomLibrary: AtomLibrary,
    screenChangeHandler: XEventHandler,
    keyConfiguration: KeyConfiguration,
    textAtomReader: TextAtomReader,
    appMenuHandler: AppMenuHandler,
    frameDrawer: FrameDrawer,
    rootWindowId: Window
): EventDistributor {

    return EventDistributor.Builder(logger)
        .addEventHandler(ConfigureRequestHandler(system, logger, windowRegistration, windowCoordinator, appMenuHandler))
        .addEventHandler(DestroyNotifyHandler(logger, windowRegistration, appMenuHandler))
        .addEventHandler(KeyPressHandler(logger, keyManager, monitorManager, windowCoordinator, focusHandler, uiDrawer))
        .addEventHandler(KeyReleaseHandler(logger, system, focusHandler, keyManager, keyConfiguration, atomLibrary))
        .addEventHandler(MapRequestHandler(logger, windowRegistration))
        .addEventHandler(UnmapNotifyHandler(logger, windowRegistration, uiDrawer, appMenuHandler))
        .addEventHandler(screenChangeHandler)
        .addEventHandler(ReparentNotifyHandler(logger, windowRegistration))
        .addEventHandler(ClientMessageHandler(logger, atomLibrary))
        .addEventHandler(SelectionClearHandler(logger))
        .addEventHandler(MappingNotifyHandler(logger, keyManager, keyConfiguration, rootWindowId))
        .addEventHandler(PropertyNotifyHandler(atomLibrary, windowRegistration, textAtomReader, frameDrawer, windowCoordinator, rootWindowId))
        .build()
}

private fun eventLoop(
    posixApi: PosixApi,
    eventDistributor: EventDistributor,
    eventTime: EventTime,
    eventBuffer: EventBuffer
) {
    while (exitState.value == null) {
        val xEvent = eventBuffer.getNextEvent(false)?.pointed

        if (xEvent == null) {
            posixApi.usleep(100.convert())
            continue
        }

        eventTime.setTimeFromEvent(xEvent.ptr)

        if (eventDistributor.handleEvent(xEvent)) {
            exitState.value = 0
        }
        eventTime.unsetEventTime()

        nativeHeap.free(xEvent)
    }
}
