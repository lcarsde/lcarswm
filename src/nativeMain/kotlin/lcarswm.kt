import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.events.*
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorManagerImpl
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.EventApi
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.system.api.WindowUtilApi
import de.atennert.lcarswm.windowactions.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

fun runWindowManager(system: SystemApi, logger: Logger) = runBlocking {
    logger.logInfo("::runWindowManager::start lcarswm initialization")

    memScoped {
        wmDetected = false
        if (!system.openDisplay()) {
            logger.logError("::runWindowManager::got no display")
            logger.close()
            return@runBlocking
        }
        val screen = system.defaultScreenOfDisplay()?.pointed
        if (screen == null) {
            logger.logError("::runWindowManager::got no screen")
            logger.close()
            system.closeDisplay()
            return@runBlocking
        }
        val rootWindow = screen.root

        val atomLibrary = AtomLibrary(system)

        val rootWindowPropertyHandler = RootWindowPropertyHandler(system, rootWindow, atomLibrary, screen.root_visual)

        if (!rootWindowPropertyHandler.becomeScreenOwner()) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler)
            return@runBlocking
        }

        system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

        system.selectInput(rootWindow, ROOT_WINDOW_MASK)
        system.sync(false)

        if (wmDetected) {
            logger.logError("::runWindowManager::Detected another active window manager")
            cleanup(logger, system, rootWindowPropertyHandler)
            return@runBlocking
        }

        rootWindowPropertyHandler.setSupportWindowProperties()

        setDisplayEnvironment(system)

        staticLogger = logger
        system.setErrorHandler(staticCFunction { _, err -> staticLogger!!.logError("::runWindowManager::error code: ${err?.pointed?.error_code}"); 0 })

        logger.logDebug("::runWindowManager::Screen size: ${screen.width}/${screen.height}, root: $rootWindow")

        val monitorManager = MonitorManagerImpl(system, rootWindow)

        val uiDrawer = RootWindowDrawer(system, monitorManager, screen)

        val keyManager = KeyManager(system, rootWindow)
        keyManager.grabInternalKeys()

        val focusHandler = WindowFocusHandler()

        focusHandler.registerObserver { activeWindow ->
            if (activeWindow != null) {
                system.setInputFocus(activeWindow, RevertToParent, CurrentTime.convert())
            } else {
                system.setInputFocus(rootWindow, RevertToPointerRoot, CurrentTime.convert())
            }
        }

        val windowCoordinator = ActiveWindowCoordinator(system, monitorManager)

        val screenChangeHandler = setupRandr(system, logger, monitorManager, windowCoordinator, uiDrawer, rootWindow)

        val windowRegistration = WindowHandler(system, logger, windowCoordinator, focusHandler, atomLibrary, rootWindow)

        setupScreen(system, rootWindow, windowRegistration)

        // TODO move this up
        val configPathBytes = system.getenv(HOME_CONFIG_DIR_PROPERTY) ?: return@runBlocking
        val configPath = configPathBytes.toKString()
        val keyConfiguration = "$configPath$KEY_CONFIG_FILE"

        val keyConfigurationProvider = ConfigurationProvider(system, keyConfiguration)

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

        eventLoop(system, eventManager)

//        val events = produceEvents(system)
//        coroutineScope {
//            launch {
//                for (event in events) {
//                    if (eventManager.handleEvent(event)) {
//                        nativeHeap.free(event)
//                        break
//                    }
//                    nativeHeap.free(event)
//                }
//            }
//        }
//
//        events.cancel()

        shutdown(system, uiDrawer, rootWindow, logger, rootWindowPropertyHandler)
    }
}

private fun shutdown(
    system: SystemApi,
    rootWindowDrawer: RootWindowDrawer,
    rootWindow: Window,
    logger: Logger,
    rootWindowPropertyHandler: RootWindowPropertyHandler
) {
    rootWindowDrawer.cleanupColorMap(system)
    rootWindowDrawer.cleanupGraphicsContexts()

    system.selectInput(rootWindow, NoEventMask)
    rootWindowPropertyHandler.unsetWindowProperties()

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
        .forEach { childId ->
            windowRegistration.addWindow(childId, true)
        }

    nativeHeap.free(topLevelWindows)
    system.ungrabServer()
}

/**
 * @return RANDR base value
 */
private fun setupRandr(
    system: SystemApi,
    logger: Logger,
    monitorManager: MonitorManager,
    windowCoordinator: WindowCoordinator,
    uiDrawer: UIDrawing,
    rootWindowId: Window
): XEventHandler {
    val randrHandlerFactory =
        RandrHandlerFactory(system, logger, monitorManager, windowCoordinator, uiDrawer, rootWindowId)

    val screenChangeHandler = randrHandlerFactory.createScreenChangeHandler()
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
): EventManager {

    return EventManager.Builder(logger)
        .addEventHandler(ConfigureRequestHandler(system, logger, windowRegistration, windowCoordinator))
        .addEventHandler(DestroyNotifyHandler(logger, windowRegistration))
        .addEventHandler(KeyPressHandler(logger, keyManager, monitorManager, windowCoordinator, focusHandler, uiDrawer))
        .addEventHandler(KeyReleaseHandler(system, focusHandler, keyManager, atomLibrary, keyConfigurationProvider))
        .addEventHandler(MapRequestHandler(logger, windowRegistration))
        .addEventHandler(UnmapNotifyHandler(logger, windowRegistration, uiDrawer))
        .addEventHandler(screenChangeHandler)
        .build()
}

private fun eventLoop(
    eventApi: EventApi,
    eventManager: EventManager
) {
    while (true) {
        val xEvent = nativeHeap.alloc<XEvent>()
        eventApi.nextEvent(xEvent.ptr)

        if (eventManager.handleEvent(xEvent)) {
            break
        }

        nativeHeap.free(xEvent)
    }
}
//
//private fun CoroutineScope.produceEvents(eventApi: EventApi) = produce {
//    while (true) {
//        val xEvent = nativeHeap.alloc<XEvent>()
//        eventApi.nextEvent(xEvent.ptr)
//        send(xEvent)
//    }
//}
