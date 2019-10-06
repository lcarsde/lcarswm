import de.atennert.lcarswm.*
import de.atennert.lcarswm.events.EVENT_HANDLERS
import de.atennert.lcarswm.events.handleRandrEvent
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

private var wmDetected = false

// this is a somewhat dirty hack to hand the logger to staticCFunction as error handler
private var staticLogger: Logger? = null

fun main() {
    val system = SystemFacade()
    val logger = FileLogger(system, LOG_FILE_PATH)

    runWindowManager(system, logger)
}

fun runWindowManager(system: SystemApi, logger: Logger) {
    logger.logInfo("::runWindowManager::start lcarswm initialization")

    memScoped {
        staticLogger = logger
        if (!system.openDisplay()) {
            logger.logError("::runWindowManager::got no display")
            return
        }
        val screen = system.defaultScreenOfDisplay()?.pointed ?: error("::runWindowManager::got no screen")
        val rootWindow = screen.root

        system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

        system.selectInput(rootWindow, SubstructureRedirectMask or SubstructureNotifyMask or PropertyChangeMask or
                EnterWindowMask or LeaveWindowMask or FocusChangeMask or ButtonPressMask or ButtonReleaseMask)
        system.sync(false)

        if (wmDetected) {
            logger.logError("::runWindowManager::Detected another active window manager")
            return
        }

        system.setErrorHandler(staticCFunction { _, err -> staticLogger!!.logError("::runWindowManager::error code: ${err?.pointed?.error_code}"); 0 })

        logger.logDebug("::runWindowManager::Screen size: ${screen.width}/${screen.height}, root: $rootWindow")

        val colorMap = allocateColorMap(system, screen.root_visual, rootWindow)
        val graphicsContexts = getGraphicContexts(system, rootWindow, colorMap.second)

        logger.logDebug("::runWindowManager::graphics loaded")

        val windowManagerConfig = WindowManagerState { system.internAtom(it, false) }

        logger.logDebug("::runWindowManager::wm state initialized")

        setupLcarsWindow(system, screen, windowManagerConfig)
        windowManagerConfig.setActiveWindowListener { activeWindow ->
            if (activeWindow != null) {
                system.setInputFocus(activeWindow.id, RevertToParent, CurrentTime.convert())
            } else {
                system.setInputFocus(rootWindow, RevertToPointerRoot, CurrentTime.convert())
            }
        }

        logger.logDebug("::runWindowManager::wm window initialized: $rootWindow")

        val logoImage = allocArrayOfPointersTo(alloc<XImage>())

        system.readXpmFileToImage("/usr/share/pixmaps/lcarswm.xpm", logoImage)

        logger.logDebug("::runWindowManager::logo loaded")

        val randrBase = setupRandr(system, logger, windowManagerConfig, logoImage[0]!!, rootWindow, graphicsContexts)

        logger.logDebug("::runWindowManager::set up randr")

        setupScreen(system, logger, rootWindow, windowManagerConfig)

        logger.logDebug("::runWindowManager::loaded window tree")

        eventLoop(system, logger, windowManagerConfig, randrBase, logoImage[0]!!, rootWindow, graphicsContexts)

        cleanupColorMap(system, colorMap)

        system.closeDisplay()

        staticLogger = null
        logger.logInfo("::runWindowManager::lcarswm stopped")
        logger.close()
    }
}

fun setupScreen(system: SystemApi, logger: Logger, rootWindow: Window, windowManagerConfig: WindowManagerState) {
    system.grabServer()

    val returnedWindows = ULongArray(1)
    val returnedParent = ULongArray(1)
    val topLevelWindows = nativeHeap.allocPointerTo<ULongVarOf<Window>>()
    val topLevelWindowCount = UIntArray(1)

    system.queryTree(rootWindow, returnedWindows.toCValues(), returnedParent.toCValues(),
        topLevelWindows.ptr,
        topLevelWindowCount.toCValues())

    ULongArray(topLevelWindowCount[0].toInt()) {topLevelWindows.value!![it]}
        .filter { childId -> childId != rootWindow }
        .forEach { childId ->
            addWindow(system, logger, windowManagerConfig, rootWindow, childId, true)
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
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Int {
    val eventBase = IntArray(1).pin()
    val errorBase = IntArray(1).pin()

    if (system.rQueryExtension(eventBase.addressOf(0), errorBase.addressOf(0)) == X_FALSE) {
        logger.logWarning("::setupRandr::no RANDR extension")
        return NO_RANDR_BASE
    }

    handleRandrEvent(system, logger, windowManagerState, image, rootWindow, graphicsContexts)

    system.rSelectInput(rootWindow,
        (RRScreenChangeNotifyMask or
                RROutputChangeNotifyMask or
                RRCrtcChangeNotifyMask or
                RROutputPropertyNotifyMask).convert() )

    logger.logDebug("::setupRandr::RANDR base: ${eventBase.get()[0]}, error base: ${errorBase.get()[0]}")

    return eventBase.get()[0]
}

private fun eventLoop(
    system: SystemApi,
    logger: Logger,
    windowManagerState: WindowManagerState,
    randrBase: Int,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
) {
    val randrEventValue = randrBase + RRScreenChangeNotify

    while (true) {
        val xEvent = nativeHeap.alloc<XEvent>()
        system.nextEvent(xEvent.ptr)
        val eventValue = xEvent.type

        if (eventValue == randrEventValue) {
            logger.logDebug("::eventLoop::received randr event")
            handleRandrEvent(system, logger, windowManagerState, image, rootWindow, graphicsContexts)
            nativeHeap.free(xEvent)
            continue
        }

        if (EVENT_HANDLERS.containsKey(xEvent.type)) {
            val stop = EVENT_HANDLERS[xEvent.type]!!.invoke(system, logger, windowManagerState, xEvent, image, rootWindow, graphicsContexts)
            if (stop) {
                logger.logInfo("::eventLoop::received stop ... exiting loop")
                break
            }
        } else {
            logger.logInfo("::eventLoop::unhandled event: ${xEvent.type}")
        }

        nativeHeap.free(xEvent)
    }
}
