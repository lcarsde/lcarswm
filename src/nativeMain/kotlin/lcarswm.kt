import de.atennert.lcarswm.*
import de.atennert.lcarswm.system.SystemAccess
import de.atennert.lcarswm.system.xDrawApi
import de.atennert.lcarswm.system.xInputApi
import de.atennert.lcarswm.system.xRandrApi
import kotlinx.cinterop.*
import xlib.*

private var wmDetected = false

fun main() {
    println("::main::start lcarswm initialization")

    memScoped {
        val display = XOpenDisplay(null) ?: error("::main::display from setup")
        val screen = XDefaultScreenOfDisplay(display)?.pointed ?: error("::main::got no screen")
        val rootWindow = screen.root

        XSetErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

        xInputApi().selectInput(display, rootWindow, SubstructureRedirectMask or SubstructureNotifyMask)
        XSync(display, X_FALSE)

        if (wmDetected) {
            println("::main::Detected another active window manager")
            return
        }

        XSetErrorHandler(staticCFunction { _, err -> println("::main::error code: ${err?.pointed?.error_code}"); 0 })

        println("::main::Screen size: ${screen.width}/${screen.height}, root: $rootWindow")

        val colorMap = allocateColorMap(display, screen.root_visual, rootWindow)
        val graphicsContexts = getGraphicContexts(display, rootWindow, colorMap.second)

        println("::main::graphics loaded")

        val windowManagerConfig = WindowManagerState { XInternAtom(display, it, X_FALSE) }

        println("::main::wm state initialized")

        setupLcarsWindow(display, screen, windowManagerConfig)
        windowManagerConfig.setActiveWindowListener { activeWindow ->
            if (activeWindow != null) {
                xInputApi().setInputFocus(display, activeWindow.id, RevertToNone, CurrentTime.convert())
            } else {
                xInputApi().setInputFocus(display, rootWindow, RevertToPointerRoot, CurrentTime.convert())
            }
        }

        println("::main::wm window initialized: $rootWindow")

        val logoImage = allocArrayOfPointersTo(alloc<XImage>())

        xDrawApi().readXpmFileToImage(display, "/usr/share/pixmaps/lcarswm.xpm", logoImage)

        println("::main::logo loaded")

        val randrBase = setupRandr(display, windowManagerConfig, logoImage[0]!!, rootWindow, graphicsContexts)

        println("::main::set up randr")

        setupScreen(display, rootWindow, windowManagerConfig)

        println("::main::loaded window tree")

        eventLoop(display, windowManagerConfig, randrBase, logoImage[0]!!, rootWindow, graphicsContexts)

        cleanupColorMap(display, colorMap)

        XCloseDisplay(display)
    }

    SystemAccess.clear()

    println("::main::lcarswm stopped")
}

fun setupScreen(display: CPointer<Display>, rootWindow: ULong, windowManagerConfig: WindowManagerState) {
    XGrabServer(display)

    val returnedWindows = ULongArray(1)
    val returnedParent = ULongArray(1)
    val topLevelWindows = nativeHeap.allocPointerTo<ULongVarOf<ULong>>()
    val topLevelWindowCount = UIntArray(1)

    XQueryTree(display, rootWindow, returnedWindows.toCValues(), returnedParent.toCValues(),
        topLevelWindows.ptr,
        topLevelWindowCount.toCValues())

    ULongArray(topLevelWindowCount[0].toInt()) {topLevelWindows.value!![it]}
        .filter { childId -> childId != rootWindow }
        .forEach { childId ->
            addWindow(display, windowManagerConfig, rootWindow, childId, true)
        }

    nativeHeap.free(topLevelWindows)
    XUngrabServer(display)
}

/**
 * @return RANDR base value
 */
private fun setupRandr(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: ULong,
    graphicsContexts: List<GC>
): Int {
    val eventBase = IntArray(1).pin()
    val errorBase = IntArray(1).pin()

    if (xRandrApi().rQueryExtension(display, eventBase.addressOf(0), errorBase.addressOf(0)) == X_FALSE) {
        println("::setupRandr::no RANDR extension")
        return NO_RANDR_BASE
    }

    handleRandrEvent(display, windowManagerState, image, rootWindow, graphicsContexts)

    xRandrApi().rSelectInput(display, rootWindow,
        (RRScreenChangeNotifyMask or
                RROutputChangeNotifyMask or
                RRCrtcChangeNotifyMask or
                RROutputPropertyNotifyMask).convert() )

    println("::setupRandr::RANDR base: ${eventBase.get()[0]}, error base: ${errorBase.get()[0]}")

    return eventBase.get()[0]
}

private fun eventLoop(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    randrBase: Int,
    image: CPointer<XImage>,
    rootWindow: ULong,
    graphicsContexts: List<GC>
) {
    val randrEventValue = randrBase + RRScreenChangeNotify

    while (true) {
        val xEvent = nativeHeap.alloc<XEvent>()
        XNextEvent(display, xEvent.ptr)
        val eventValue = xEvent.type

        if (eventValue == randrEventValue) {
            println("::eventLoop::received randr event")
            handleRandrEvent(display, windowManagerState, image, rootWindow, graphicsContexts)
            nativeHeap.free(xEvent)
            continue
        }

        if (EVENT_HANDLERS.containsKey(xEvent.type)) {
            val stop = EVENT_HANDLERS[xEvent.type]!!.invoke(display, windowManagerState, xEvent, image, rootWindow, graphicsContexts)
            if (stop) {
                break
            }
        } else {
            println("::eventLoop::unhandled event: ${xEvent.type}")
        }

        nativeHeap.free(xEvent)
    }
}
