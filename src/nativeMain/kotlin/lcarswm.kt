import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.events.*
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorManagerImpl
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
        wmDetected = false
        if (!system.openDisplay()) {
            logger.logError("::runWindowManager::got no display")
            logger.close()
            return
        }
        val screen = system.defaultScreenOfDisplay()?.pointed
        if (screen == null) {
            logger.logError("::runWindowManager::got no screen")
            logger.close()
            system.closeDisplay()
            return
        }

        val randrHandlerFactory = RandrHandlerFactory(system, logger)

        system.synchronize(false)

        setDisplayEnvironment(system)

        val rootWindow = screen.root

        val atomLibrary = AtomLibrary(system)

        val eventBuffer = EventBuffer(system)

        val rootWindowPropertyHandler = RootWindowPropertyHandler(logger, system, rootWindow, atomLibrary, eventBuffer)

        val eventTime = EventTime(system, eventBuffer, atomLibrary, rootWindowPropertyHandler)

        if (!rootWindowPropertyHandler.becomeScreenOwner(eventTime)) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler)
            return
        }

        // TODO clear the event queue

        system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

        system.selectInput(rootWindow, ROOT_WINDOW_MASK)
        system.sync(false)

        if (wmDetected) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler)
            return
        }

        staticLogger = logger
        system.setErrorHandler(staticCFunction { _, err -> staticLogger!!.logError("::runWindowManager::error code: ${err?.pointed?.error_code}"); 0 })

        rootWindowPropertyHandler.setSupportWindowProperties()

        eventTime.resetEventTime()

        val keyConfigurationProvider = loadKeyConfiguration(system) ?: return

        logger.logDebug("::runWindowManager::Screen size: ${screen.width}/${screen.height}, root: $rootWindow")

        val monitorManager = MonitorManagerImpl(system, rootWindow)

        val uiDrawer = RootWindowDrawer(system, monitorManager, screen)

        val keyManager = KeyManager(system, rootWindow)
        keyManager.grabInternalKeys()

        val focusHandler = WindowFocusHandler()

        focusHandler.registerObserver { activeWindow ->
            if (activeWindow != null) {
                system.setInputFocus(activeWindow, RevertToParent, eventTime.lastEventTime)
            } else {
                system.setInputFocus(rootWindow, RevertToPointerRoot, eventTime.lastEventTime)
            }
        }

        val windowCoordinator = ActiveWindowCoordinator(system, monitorManager)

        val screenChangeHandler = setupRandr(system, randrHandlerFactory, monitorManager, windowCoordinator, uiDrawer, rootWindow)

        val windowRegistration = WindowHandler(system, logger, windowCoordinator, focusHandler, atomLibrary, rootWindow)

        setupScreen(system, rootWindow, rootWindowPropertyHandler, windowRegistration)

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

        eventLoop(eventManager, eventBuffer)

        system.sync(false)

        shutdown(system, uiDrawer, rootWindow, logger, rootWindowPropertyHandler, keyManager)
    }
}

private fun shutdown(
    system: SystemApi,
    rootWindowDrawer: RootWindowDrawer,
    rootWindow: Window,
    logger: Logger,
    rootWindowPropertyHandler: RootWindowPropertyHandler,
    keyManager: KeyManager
) {
    rootWindowDrawer.cleanupColorMap(system)
    rootWindowDrawer.cleanupGraphicsContexts()

    system.selectInput(rootWindow, NoEventMask)
    rootWindowPropertyHandler.unsetWindowProperties()

    keyManager.cleanup()

    cleanup(logger, system, rootWindowPropertyHandler)
}

fun cleanup(logger: Logger, windowUtils: WindowUtilApi, rootWindowPropertyHandler: RootWindowPropertyHandler) {
    rootWindowPropertyHandler.destroySupportWindow()

    windowUtils.closeDisplay()

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
    eventBuffer: EventBuffer
) {
    while (true) {
        val xEvent = eventBuffer.getNextEvent(true)?.pointed ?: continue

        if (eventDistributor.handleEvent(xEvent)) {
            break
        }

        nativeHeap.free(xEvent)
    }
}
