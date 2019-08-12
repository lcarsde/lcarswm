package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 * Map of event types to event handlers. DON'T EDIT THE MAPS CONTENT!!!
 */
val EVENT_HANDLERS =
    hashMapOf<Int, Function7<CPointer<Display>, WindowManagerState, XEvent, CPointer<XImage>, ULong, ULong, List<GC>, Boolean>>(
        Pair(KeyPress, { d, w, e, _, _, _, _ -> handleKeyPress(d, w, e) }),
        Pair(KeyRelease, { d, w, e, l, _, lw, gc -> handleKeyRelease(d, w, e, l, lw, gc) }),
        Pair(ButtonPress, { _, _, e, _, _, _, _ -> handleButtonPress(e) }),
        Pair(ButtonRelease, { _, _, e, _, _, _, _ -> handleButtonRelease(e) }),
        Pair(ConfigureRequest, { d, w, e, _, _, _, _ -> handleConfigureRequest(d, w, e) }),
        Pair(MapRequest, { d, w, e, _, _, lw, _ -> handleMapRequest(d, w, e, lw) }),
        Pair(MapNotify, { _, _, e, _, _, _, _ -> handleMapNotify(e) }),
        Pair(DestroyNotify, { _, w, e, _, _, _, _ -> handleDestroyNotify(w, e) }),
        Pair(UnmapNotify, ::handleUnmapNotify),
        Pair(ReparentNotify, { _, _, e, _, _, _, _ -> handleReparentNotify(e) }),
        Pair(CreateNotify, { _, _, e, _, _, _, _ -> handleCreateNotify(e) }),
        Pair(ConfigureNotify, { _, _, e, _, _, _, _ -> handleConfigureNotify(e) })
    )

private fun handleCreateNotify(xEvent: XEvent): Boolean {
    @Suppress("UNCHECKED_CAST")
    val createEvent = xEvent.xcreatewindow
    println("::handleCreate::window ${createEvent.window}, o r: ${createEvent.override_redirect}")
    return false
}

private fun handleConfigureNotify(xEvent: XEvent): Boolean {
    @Suppress("UNCHECKED_CAST")
    val configureEvent = xEvent.xconfigure
    println("::handleConfigureNotify::window ${configureEvent.window}, o r: ${configureEvent.override_redirect}, above ${configureEvent.above}, event ${configureEvent.event}")
    return false
}

private fun handleKeyPress(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val pressEvent = xEvent.xkey
    val key = pressEvent.keycode
    println("::handleKeyPress::Key pressed: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_Up -> moveActiveWindow(display, windowManagerState, windowManagerState::moveWindowToPreviousMonitor)
        XK_Down -> moveActiveWindow(display, windowManagerState, windowManagerState::moveWindowToNextMonitor)
        XK_Tab -> moveNextWindowToTopOfStack(display, windowManagerState)
        else -> println("::handleKeyRelease::unknown key: $key")
    }

    return false
}

private fun handleKeyRelease(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    lcarsWindow: ULong,
    graphicsContexts: List<GC>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val releasedEvent = xEvent.xkey
    val key = releasedEvent.keycode
    println("::handleKeyRelease::Key released: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_M -> toggleScreenMode(display, windowManagerState, image, lcarsWindow, graphicsContexts)
        XK_T -> runProgram("/usr/bin/xterm")
        XK_B -> runProgram("/usr/bin/firefox")
        XK_I -> runProgram("/usr/bin/idea")
        XK_L -> runProgram("/usr/bin/lxterminal")
        XK_Q -> return true
        else -> println("::handleKeyRelease::unknown key: $key")
    }
    return false
}

private fun handleButtonPress(xEvent: XEvent): Boolean {
    @Suppress("UNCHECKED_CAST")
    val pressEvent = xEvent.xbutton
    val button = pressEvent.button

    println("::handleButtonPress::Button pressed: $button")
    return false
}

private fun handleButtonRelease(xEvent: XEvent): Boolean {
    @Suppress("UNCHECKED_CAST")
    val pressEvent = xEvent.xbutton
    val button = pressEvent.button

    println("::handleButtonRelease::Button released: $button")
    return false
}

private fun handleMapRequest(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    lcarsWindow: ULong
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val mapEvent = xEvent.xmaprequest
    val window = mapEvent.window

    println("::handleMapRequest::map request for window $window, parent: ${mapEvent.parent}")
    if (windowManagerState.getWindowMonitor(window) != null) {
        return false
    }

    addWindow(display, windowManagerState, lcarsWindow, window, false)

    XMapWindow(display, window)

    return false
}

private fun handleMapNotify(xEvent: XEvent): Boolean {
    @Suppress("UNCHECKED_CAST")
    val mapEvent = xEvent.xmap
    val window = mapEvent.window
    println("::handleMapNotify::map notify for window $window")

    return false
}

private fun handleReparentNotify(xEvent: XEvent): Boolean {
    @Suppress("UNCHECKED_CAST")
    val reparentEvent = xEvent.xreparent
    println("::handleReparentNotify::reparented window ${reparentEvent.window} to ${reparentEvent.parent}")
    return false
}

/**
 * Filter the values that lcarswm requires and send the configuration to X.
 */
private fun handleConfigureRequest(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val configureEvent = xEvent.xconfigurerequest

    println("::handleConfigureRequest::configure request for window ${configureEvent.window}, stack mode: ${configureEvent.detail}, sibling: ${configureEvent.above}, parent: ${configureEvent.parent}")
    val windowMonitor = windowManagerState.getWindowMonitor(configureEvent.window)
    if (windowMonitor != null) {
        return false
    }

    val windowChanges = nativeHeap.alloc<XWindowChanges>()
    windowChanges.x = configureEvent.x
    windowChanges.y = configureEvent.y
    windowChanges.width = configureEvent.width
    windowChanges.height = configureEvent.height
    windowChanges.sibling = configureEvent.above
    windowChanges.stack_mode = configureEvent.detail
    windowChanges.border_width = 0

    XConfigureWindow(display, configureEvent.window, configureEvent.value_mask.convert(), windowChanges.ptr)

    return false
}

/**
 * Remove window from the wm data on window destroy.
 */
private fun handleDestroyNotify(
    windowManagerState: WindowManagerState,
    xEvent: XEvent
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val destroyEvent = xEvent.xdestroywindow
    println("::handleDestroyNotify::destroy window: ${destroyEvent.window}")
    windowManagerState.removeWindow(destroyEvent.window)
    return false
}

/**
 * Remove the window from the wm data on window unmap.
 */
private fun handleUnmapNotify(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    rootWindow: ULong,
    lcarsWindow: ULong,
    graphicsContexts: List<GC>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val unmapEvent = xEvent.xunmap
    println("::handleUnmapNotify::unmapped window: ${unmapEvent.window}")
    // only the active window can be closed, so make a new window active
    if (windowManagerState.hasWindow(unmapEvent.window) && unmapEvent.event != rootWindow) {
        XReparentWindow(display, unmapEvent.window, rootWindow, 0, 0)
        XRemoveFromSaveSet(display, unmapEvent.window)

        windowManagerState.removeWindow(unmapEvent.window)
        moveNextWindowToTopOfStack(display, windowManagerState)
    }

    windowManagerState.monitors.forEach { monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
        println("::handleRandrEvent::draw monitor ${monitor.id} :: ${monitor.name}")
        drawFunction(
            graphicsContexts,
            lcarsWindow,
            display,
            monitor,
            image
        )
    }
    return false
}

/**
 * Get RANDR information and update window management accordingly.
 */
fun handleRandrEvent(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: ULong,
    lcarsWindow: ULong,
    graphicsContexts: List<GC>
) {
    println("::handleRandrEvent::handle randr")

    val resources = XRRGetScreenResources(display, rootWindow)!!
    val primary = XRRGetOutputPrimary(display, rootWindow)

    val outputs = resources.pointed.outputs

    val sortedMonitors = Array(resources.pointed.noutput)
    { i -> Pair(outputs!![i], XRRGetOutputInfo(display, resources, outputs[i])) }
        .asSequence()
        .filter { (_, outputObject) ->
            outputObject != null
        }
        .map { (outputId, outputObject) ->
            Triple(outputId, outputObject!!, getOutputName(outputObject))
        }
        .map { (outputId, outputObject, outputName) ->
            Triple(Monitor(outputId, outputName, outputId == primary), outputObject.pointed.crtc, outputObject)
        }
        .onEach { (monitor, c, _) ->
            println("::printOutput::name: ${monitor.name}, id: ${monitor.id} crtc: $c")
        }
        .map { (monitor, crtc, outputObject) ->
            nativeHeap.free(outputObject)
            Pair(monitor, crtc)
        }
        .groupBy { (_, crtc) -> crtc.toInt() != 0 }

    // unused monitors
    sortedMonitors[false]

    val activeMonitors = sortedMonitors[true].orEmpty()
        .map { (monitor, crtcReference) ->
            addMeasurementToMonitor(display, monitor, crtcReference, resources)
        }
        .filter { it.isFullyInitialized }

    println("::handleRandrEvent::used monitors: ${sortedMonitors[true]?.size}, unused monitors: ${sortedMonitors[false]?.size}")

    val (width, height) = activeMonitors
        .fold(Pair(0, 0)) { (width, height), monitor ->
            var newWidth = width
            var newHeight = height
            if (monitor.x + monitor.width > width) {
                newWidth = monitor.x + monitor.width
            }
            if (monitor.y + monitor.height > height) {
                newHeight = monitor.y + monitor.height
            }
            Pair(newWidth, newHeight)
        }

    XResizeWindow(display, lcarsWindow, width.convert(), height.convert())

    windowManagerState.screenSize = Pair(width, height)
    windowManagerState.updateMonitors(activeMonitors)
    { measurements, windowId -> adjustWindowPositionAndSize(display, measurements, windowId) }

    windowManagerState.monitors.forEach { monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
        println("::handleRandrEvent::draw monitor ${monitor.id} :: ${monitor.name}")
        drawFunction(
            graphicsContexts,
            lcarsWindow,
            display,
            monitor,
            image
        )
    }
}

private fun addMeasurementToMonitor(
    display: CPointer<Display>,
    monitor: Monitor,
    crtcReference: RRCrtc,
    resources: CPointer<XRRScreenResources>
): Monitor {
    val crtcInfo = XRRGetCrtcInfo(display, resources, crtcReference)!!.pointed

    monitor.setMeasurements(crtcInfo.x, crtcInfo.y, crtcInfo.width, crtcInfo.height)

    return monitor
}

/**
 * Get the name of the given output.
 */
private fun getOutputName(outputObject: CPointer<XRROutputInfo>): String {
    val name = outputObject.pointed.name
    val nameArray = ByteArray(outputObject.pointed.nameLen) { name!![it] }

    return nameArray.decodeToString()
}


private fun moveActiveWindow(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    windowMoveFunction: Function1<Window, Monitor>
) {
    val activeWindow = windowManagerState.activeWindow ?: return
    val newMonitor = windowMoveFunction(activeWindow)

    adjustWindowPositionAndSize(
        display,
        newMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(newMonitor)),
        activeWindow.id
    )
}
