import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.events.*
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorManagerImpl
import de.atennert.lcarswm.signal.SignalHandler
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.PosixApi
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.system.api.WindowUtilApi
import de.atennert.lcarswm.windowactions.*
import kotlinx.cinterop.*
import xlib.*

private var wmDetected = false

// this is a somewhat dirty hack to hand the logger to staticCFunction as error handler
private var staticLogger: Logger? = null

const val ROOT_WINDOW_MASK = SubstructureRedirectMask or SubstructureNotifyMask or PropertyChangeMask or
        EnterWindowMask or LeaveWindowMask or FocusChangeMask or ButtonPressMask or ButtonReleaseMask

const val XRANDR_MASK = RRScreenChangeNotifyMask or RROutputChangeNotifyMask or
        RRCrtcChangeNotifyMask or RROutputPropertyNotifyMask

// the main method apparently must not be inside of a package so it can be compiled with Kotlin/Native
fun main() {
    val system = SystemFacade()
    val logger = FileLogger(system, LOG_FILE_PATH)

    runWindowManager(system, logger)
}

fun runWindowManager(system: SystemApi, logger: Logger) {
    logger.logInfo("::runWindowManager::start lcarswm initialization")

    memScoped {
        val signalHandler = SignalHandler(system)

        wmDetected = false
        if (!system.openDisplay()) {
            logger.logError("::runWindowManager::got no display")
            logger.close()
            signalHandler.cleanup()
            return
        }
        val screen = system.defaultScreenOfDisplay()?.pointed
        if (screen == null) {
            logger.logError("::runWindowManager::got no screen")
            logger.close()
            system.closeDisplay()
            signalHandler.cleanup()
            return
        }

        val randrHandlerFactory = RandrHandlerFactory(system, logger)

        system.synchronize(false)

        setDisplayEnvironment(system)

        val atomLibrary = AtomLibrary(system)

        val eventBuffer = EventBuffer(system)

        val rootWindowPropertyHandler = RootWindowPropertyHandler(logger, system, screen.root, atomLibrary, eventBuffer)

        val eventTime = EventTime(system, eventBuffer, atomLibrary, rootWindowPropertyHandler)

        if (!rootWindowPropertyHandler.becomeScreenOwner(eventTime)) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
            return
        }

        system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

        system.selectInput(screen.root, ROOT_WINDOW_MASK)
        system.sync(false)

        if (wmDetected) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler, signalHandler)
            return
        }

        staticLogger = logger
        system.setErrorHandler(staticCFunction { _, err -> staticLogger?.logError("::runWindowManager::error code: ${err?.pointed?.error_code}"); 0 })

        rootWindowPropertyHandler.setSupportWindowProperties()

        eventTime.resetEventTime()

        val keyConfigurationProvider = loadKeyConfiguration(system) ?: return

        logger.logDebug("::runWindowManager::Screen size: ${screen.width}/${screen.height}, root: ${screen.root}")

        val monitorManager = MonitorManagerImpl(system, screen.root)

        val uiDrawer = RootWindowDrawer(system, monitorManager, screen)

        val keyManager = KeyManager(system, screen.root)
        keyManager.grabInternalKeys()

        val focusHandler = WindowFocusHandler()

        focusHandler.registerObserver { activeWindow ->
            if (activeWindow != null) {
                system.setInputFocus(activeWindow, RevertToParent, eventTime.lastEventTime)
            } else {
                system.setInputFocus(screen.root, RevertToPointerRoot, eventTime.lastEventTime)
            }
        }

        val windowCoordinator = ActiveWindowCoordinator(system, monitorManager)

        val screenChangeHandler = setupRandr(system, randrHandlerFactory, monitorManager, windowCoordinator, uiDrawer, screen.root)

        val windowRegistration = WindowHandler(system, logger, windowCoordinator, focusHandler, atomLibrary, screen.root)

        setupScreen(system, screen.root, rootWindowPropertyHandler, windowRegistration)

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
            keyConfigurationProvider,
            screenChangeHandler
        )

        eventLoop(eventManager, eventTime, eventBuffer)

        system.sync(false)

        shutdown(system, uiDrawer, screen.root, logger, rootWindowPropertyHandler, keyManager, signalHandler)
    }
}

/**
 * Full shutdown routines.
 */
private fun shutdown(
    system: SystemApi,
    rootWindowDrawer: RootWindowDrawer,
    rootWindow: Window,
    logger: Logger,
    rootWindowPropertyHandler: RootWindowPropertyHandler,
    keyManager: KeyManager,
    signalHandler: SignalHandler
) {
    rootWindowDrawer.cleanupColorMap(system)
    rootWindowDrawer.cleanupGraphicsContexts()

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

    ULongArray(topLevelWindowCount[0].toInt()) { topLevelWindows.value!![it] }
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
    keyConfigurationProvider: ConfigurationProvider,
    screenChangeHandler: XEventHandler
): EventDistributor {

    return EventDistributor.Builder(logger)
        .addEventHandler(ConfigureRequestHandler(system, logger, windowRegistration, windowCoordinator))
        .addEventHandler(DestroyNotifyHandler(logger, windowRegistration))
        .addEventHandler(KeyPressHandler(logger, keyManager, monitorManager, windowCoordinator, focusHandler, uiDrawer))
        .addEventHandler(KeyReleaseHandler(logger, system, focusHandler, keyManager, atomLibrary, keyConfigurationProvider))
        .addEventHandler(MapRequestHandler(logger, windowRegistration))
        .addEventHandler(UnmapNotifyHandler(logger, windowRegistration, uiDrawer))
        .addEventHandler(screenChangeHandler)
        .addEventHandler(ReparentNotifyHandler(logger, windowRegistration))
        .addEventHandler(ClientMessageHandler(logger, atomLibrary))
        .addEventHandler(SelectionClearHandler(logger))
        .build()
}

private fun eventLoop(
    eventDistributor: EventDistributor,
    eventTime: EventTime,
    eventBuffer: EventBuffer
) {
    while (true) {
        val xEvent = eventBuffer.getNextEvent(true)?.pointed ?: continue

        eventTime.setTimeFromEvent(xEvent.ptr)

        if (eventDistributor.handleEvent(xEvent)) {
            break
        }

        nativeHeap.free(xEvent)
    }
}
