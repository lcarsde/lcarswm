import de.atennert.lcarswm.*
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

        XSelectInput(display, rootWindow, SubstructureRedirectMask or SubstructureNotifyMask)
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

        val lcarsWindow = setupLcarsWindow(display, screen, windowManagerConfig)

        println("::main::wm window initialized: $lcarsWindow")

        val logoImage = allocArrayOfPointersTo(alloc<XImage>())

        XpmReadFileToImage(display, "/usr/share/pixmaps/lcarswm.xpm", logoImage, null, null)

        println("::main::logo loaded")

        val randrBase = setupRandr(display, windowManagerConfig, logoImage[0]!!, rootWindow, lcarsWindow, graphicsContexts)

        println("::main::set up randr")

        setupScreen(display, rootWindow, lcarsWindow, windowManagerConfig)

        println("::main::loaded window tree")

        runProgram("/usr/bin/VBoxClient-all")

        println("::main::started vbox client")

        eventLoop(display, windowManagerConfig, randrBase, logoImage[0]!!, rootWindow, lcarsWindow, graphicsContexts)

        cleanupColorMap(display, colorMap)

        XCloseDisplay(display)
    }

    println("::main::lcarswm stopped")
}

fun setupScreen(display: CPointer<Display>, rootWindow: ULong, lcarsWindow: ULong, windowManagerConfig: WindowManagerState) {
    XGrabServer(display)

    val returnedWindows = ULongArray(1)
    val returnedParent = ULongArray(1)
    val topLevelWindows = nativeHeap.allocPointerTo<ULongVarOf<ULong>>()
    val topLevelWindowCount = UIntArray(1)

    XQueryTree(display, rootWindow, returnedWindows.toCValues(), returnedParent.toCValues(),
        topLevelWindows.ptr,
        topLevelWindowCount.toCValues())

    ULongArray(topLevelWindowCount[0].toInt()) {topLevelWindows.value!![it]}
        .filter { childId -> childId != lcarsWindow }
        .forEach { childId ->
            addWindow(display, windowManagerConfig, lcarsWindow, childId, true)
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
    lcarsWindow: ULong,
    graphicsContexts: List<GC>
): Int {
    val eventBase = IntArray(1).pin()
    val errorBase = IntArray(1).pin()

    if (XRRQueryExtension(display, eventBase.addressOf(0), errorBase.addressOf(0)) == X_FALSE) {
        println("::setupRandr::no RANDR extension")
        return NO_RANDR_BASE
    }

    handleRandrEvent(display, windowManagerState, image, rootWindow, lcarsWindow, graphicsContexts)

    XRRSelectInput(display, rootWindow,
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
    lcarsWindow: ULong,
    graphicsContexts: List<GC>
) {
    val randrEventValue = randrBase + RRScreenChangeNotify

    while (true) {
        val xEvent = nativeHeap.alloc<XEvent>()
        XNextEvent(display, xEvent.ptr)
        val eventValue = xEvent.type

        if (eventValue == randrEventValue) {
            println("::eventLoop::received randr event")
            handleRandrEvent(display, windowManagerState, image, rootWindow, lcarsWindow, graphicsContexts)
            nativeHeap.free(xEvent)
            continue
        }

        if (EVENT_HANDLERS.containsKey(xEvent.type)) {
            val stop = EVENT_HANDLERS[xEvent.type]!!.invoke(display, windowManagerState, xEvent, image, rootWindow, lcarsWindow, graphicsContexts)
            if (stop) {
                break
            }
        } else {
            println("::eventLoop::unhandled event: ${xEvent.type}")
        }

        nativeHeap.free(xEvent)
    }
}
