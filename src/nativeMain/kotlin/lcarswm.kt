import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.TextAtomReader
import de.atennert.lcarswm.drawing.*
import de.atennert.lcarswm.events.*
import de.atennert.lcarswm.keys.KeyConfiguration
import de.atennert.lcarswm.keys.KeyManager
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorManagerImpl
import de.atennert.lcarswm.settings.SettingsReader
import de.atennert.lcarswm.signal.Signal
import de.atennert.lcarswm.signal.SignalHandler
import de.atennert.lcarswm.system.MessageQueue
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.system.api.WindowUtilApi
import de.atennert.lcarswm.window.*
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.WNOHANG
import platform.posix.waitpid
import xlib.*

private var wmDetected = false

// this is a somewhat dirty hack to hand the logger to staticCFunction as error handler
private var staticLogger: Logger? = null

private val exitState = atomic<Int?>(null)

const val ROOT_WINDOW_MASK = SubstructureRedirectMask or StructureNotifyMask or PropertyChangeMask or
        FocusChangeMask or KeyPressMask or KeyReleaseMask

const val XRANDR_MASK = RRScreenChangeNotifyMask or RROutputChangeNotifyMask or
        RRCrtcChangeNotifyMask or RROutputPropertyNotifyMask

// the main method apparently must not be inside of a package so it can be compiled with Kotlin/Native
fun main() = runBlocking {
    val system = SystemFacade()
    val cacheDirPath = system.getenv(HOME_CACHE_DIR_PROPERTY)?.toKString() ?: error("::main::cache dir not set")
    val logger = FileLogger(system, cacheDirPath + LOG_FILE_PATH)

    runWindowManager(system, logger)

    system.exit(exitState.value ?: -1)
}

suspend fun runWindowManager(system: SystemApi, logger: Logger) = coroutineScope {
    logger.logInfo("::runWindowManager::start lcarswm initialization")
    exitState.value = null
    staticLogger = logger

    val signalHandler = SignalHandler(system)

    wmDetected = false
    if (!system.openDisplay()) {
        logger.logError("::runWindowManager::got no display")
        logger.close()
        signalHandler.cleanup()
        return@coroutineScope
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
        return@coroutineScope
    }

    system.synchronize(false)

    setDisplayEnvironment(system)

    val rootWindowPropertyHandler = RootWindowPropertyHandler(logger, system, screen.root, atomLibrary, eventBuffer)

    val eventTime = EventTime(system, eventBuffer, atomLibrary, rootWindowPropertyHandler)

    if (!rootWindowPropertyHandler.becomeScreenOwner(eventTime)) {
        logger.logError("::runWindowManager::Detected another active window manager")
        cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
        return@coroutineScope
    }

    system.sync(false)
    system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

    system.selectInput(screen.root, ROOT_WINDOW_MASK)
    system.sync(false)

    if (wmDetected) {
        logger.logError("::runWindowManager::Detected another active window manager")
        cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
        return@coroutineScope
    }

    system.setErrorHandler(staticCFunction { _, err -> staticLogger?.logError("::runWindowManager::error code: ${err?.pointed?.error_code}"); 0 })

    rootWindowPropertyHandler.setSupportWindowProperties()

    eventTime.resetEventTime()

    val settings = loadSettings(logger, system)
    if (settings == null) {
        logger.logError("::runWindowManager::unable to load settings")
        cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
        return@coroutineScope
    }

    logger.logDebug("::runWindowManager::Screen size: ${screen.width}/${screen.height}, root: ${screen.root}")

    val monitorManager = MonitorManagerImpl(system, screen.root)

    val fontProvider = FontProvider(system, settings.generalSettings, system.defaultScreenNumber())
    val colorHandler = Colors(system, screen)
    val uiDrawer =
        RootWindowDrawer(system, system, monitorManager, screen, colorHandler, settings.generalSettings, fontProvider)

    keyManager.ungrabAllKeys(screen.root)

    val keyConfiguration = KeyConfiguration(
        system,
        settings.keyBindings,
        keyManager,
        screen.root
    )

    system.sync(false)

    val focusHandler = WindowFocusHandler()

    val frameDrawer = FrameDrawer(system, system, focusHandler, fontProvider, colorHandler, screen)

    val windowCoordinator = ActiveWindowCoordinator(system, monitorManager, frameDrawer)

    val screenChangeHandler =
        setupRandr(system, randrHandlerFactory, monitorManager, windowCoordinator, uiDrawer, screen.root)

    val windowNameReader = TextAtomReader(system, atomLibrary)

    val appMenuHandler = AppMenuHandler(system, atomLibrary, monitorManager, screen.root)

    val appMenuMessageQueue = MessageQueue(system, "/lcarswm-app-menu-messages", MessageQueue.Mode.READ)

    val windowList = WindowList()

    val appMenuMessageHandler = AppMenuMessageHandler(logger, system, atomLibrary, windowList, focusHandler)

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
            system.setInputFocus(activeWindow, RevertToNone, eventTime.lastEventTime)
        } else {
            system.setInputFocus(screen.root, RevertToPointerRoot, eventTime.lastEventTime)
        }
    }

    focusHandler.registerObserver { activeWindow, oldWindow ->
        listOf(oldWindow, activeWindow).forEach {
            it?.let { ow ->
                windowRegistration[ow]?.let { fw ->
                    frameDrawer.drawFrame(fw, windowCoordinator.getMonitorForWindow(ow))
                }
            }
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
        windowList,
        screen.root
    )

    setupScreen(system, screen.root, rootWindowPropertyHandler, windowRegistration)

    runProgram(system, "lcarswm-app-menu.py", listOf())

    runEventLoops(logger, eventManager, eventTime, eventBuffer, appMenuMessageHandler, appMenuMessageQueue)

    system.sync(false)

    shutdown(
        system,
        uiDrawer,
        colorHandler,
        screen.root,
        logger,
        rootWindowPropertyHandler,
        keyManager,
        signalHandler,
        frameDrawer,
        fontProvider,
        appMenuHandler,
        appMenuMessageQueue
    )
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
        Signal.INT -> exitState.value = 0
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
    frameDrawer: FrameDrawer,
    fontProvider: FontProvider,
    appMenuHandler: AppMenuHandler,
    appMenuMessageQueue: MessageQueue
) {
    appMenuHandler.close()
    appMenuMessageQueue.close()
    rootWindowDrawer.close()
    frameDrawer.close()
    fontProvider.close()
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
fun loadSettings(logger: Logger, systemApi: SystemApi): SettingsReader? {
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
    uiDrawer: UIDrawing,
    atomLibrary: AtomLibrary,
    screenChangeHandler: XEventHandler,
    keyConfiguration: KeyConfiguration,
    textAtomReader: TextAtomReader,
    appMenuHandler: AppMenuHandler,
    frameDrawer: FrameDrawer,
    windowList: WindowList,
    rootWindowId: Window
): EventDistributor {

    return EventDistributor.Builder(logger)
        .addEventHandler(ConfigureRequestHandler(system, logger, windowRegistration, windowCoordinator, appMenuHandler))
        .addEventHandler(DestroyNotifyHandler(logger, windowRegistration, appMenuHandler))
        .addEventHandler(ButtonPressHandler(logger, system, windowList, focusHandler))
        .addEventHandler(KeyPressHandler(logger, keyManager, keyConfiguration, monitorManager, windowCoordinator, focusHandler, uiDrawer))
        .addEventHandler(KeyReleaseHandler(logger, system, focusHandler, keyManager, keyConfiguration, atomLibrary))
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

private suspend fun runEventLoops(
    logger: Logger,
    eventDistributor: EventDistributor,
    eventTime: EventTime,
    eventBuffer: EventBuffer,
    appMenuMessageHandler: AppMenuMessageHandler,
    appMenuMessageQueue: MessageQueue
) = coroutineScope {

    val appMenuJob = runAppMenuLoop(logger, appMenuMessageQueue, appMenuMessageHandler)
    val xEventJob = runXEventLoop(logger, eventBuffer, eventTime, eventDistributor)

    // When the X event loop goes down, so does everything else
    xEventJob.invokeOnCompletion {
        appMenuJob.cancel()
    }

    appMenuJob.join()
    logger.logDebug("::eventLoop::finished event loops")
}

private fun CoroutineScope.runXEventLoop(
    logger: Logger,
    eventBuffer: EventBuffer,
    eventTime: EventTime,
    eventDistributor: EventDistributor
): Job {
    val xEventJob = launch {
        logger.logDebug("::runXEventLoop::running X event loop")
        while (exitState.value == null) {
            val xEvent = eventBuffer.getNextEvent(false)?.pointed

            if (xEvent == null) {
                delay(50)
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
    xEventJob.invokeOnCompletion { logger.logDebug("::runXEventLoop::X event job completed") }
    return xEventJob
}

private fun CoroutineScope.runAppMenuLoop(
    logger: Logger,
    appMenuMessageQueue: MessageQueue,
    appMenuMessageHandler: AppMenuMessageHandler
): Job {
    val appMenuJob = launch {
        logger.logDebug("::runAppMenuLoop::running app menu job")
        while (true) {
            appMenuMessageQueue.receiveMessage()?.let {
                appMenuMessageHandler.handleMessage(it)
            }
            delay(100)
        }
    }
    appMenuJob.invokeOnCompletion { logger.logDebug("::runAppMenuLoop::app menu job completed") }
    return appMenuJob
}
