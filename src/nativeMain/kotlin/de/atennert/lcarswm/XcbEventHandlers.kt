package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 * Map of event types to event handlers. DON'T EDIT THE MAPS CONTENT!!!
 */
val EVENT_HANDLERS =
    hashMapOf<Int, Function7<CPointer<Display>, WindowManagerState, XEvent, CPointer<XImage>, ULong, ULong, List<GC>, Boolean>>(
        Pair(KeyPress, { d, w, e, i, _, lw, gc -> handleKeyPress(d, w, e, i, lw, gc) }),
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
    xEvent: XEvent,
    image: CPointer<XImage>,
    lcarsWindow: ULong,
    graphicsContexts: List<GC>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val pressEvent = xEvent.xkey
    val key = pressEvent.keycode
    println("::handleKeyPress::Key pressed: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_Up -> moveActiveWindow(display, windowManagerState, image, lcarsWindow, graphicsContexts, windowManagerState::moveWindowToNextMonitor)
        XK_Down -> moveActiveWindow(display, windowManagerState, image, lcarsWindow, graphicsContexts, windowManagerState::moveWindowToPreviousMonitor)
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
        XK_T -> loadAppFromKeyBinding("Win+T")
        XK_B -> loadAppFromKeyBinding("Win+B")
        XK_I -> loadAppFromKeyBinding("Win+I")
        XF86XK_AudioMute -> loadAppFromKeyBinding("XF86AudioMute")
        XF86XK_AudioLowerVolume -> loadAppFromKeyBinding("XF86AudioLowerVolume")
        XF86XK_AudioRaiseVolume -> loadAppFromKeyBinding("XF86AudioRaiseVolume")
        XK_F4 -> closeActiveWindow(display, windowManagerState)
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

    val windowChanges = nativeHeap.alloc<XWindowChanges>()
    windowChanges.x = configureEvent.x
    windowChanges.y = configureEvent.y
    windowChanges.width = configureEvent.width
    windowChanges.height = configureEvent.height
    windowChanges.sibling = configureEvent.above
    windowChanges.stack_mode = configureEvent.detail
    windowChanges.border_width = 0

    if (windowManagerState.hasWindow(configureEvent.window)) {
        val windowPair = windowManagerState.windows.single {it.first.id == configureEvent.window}
        val measurements = windowPair.second.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowPair.second))

        val window = windowPair.first
        val e = nativeHeap.alloc<XEvent>()
        e.type = ConfigureNotify
        e.xconfigure.display = display
        e.xconfigure.event = window.id
        e.xconfigure.window = window.id
        e.xconfigure.x = measurements[0]
        e.xconfigure.y = measurements[1]
        e.xconfigure.width = measurements[2]
        e.xconfigure.height = measurements[3]
        e.xconfigure.border_width = 0
        e.xconfigure.above = None.convert()
        e.xconfigure.override_redirect = X_FALSE
        XSendEvent(display, window.id, X_FALSE, StructureNotifyMask, e.ptr)
        return false
    }

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
        val window = windowManagerState.windows.map { it.first }.single { it.id == unmapEvent.window }
        XUnmapWindow(display, window.frame)
        XReparentWindow(display, unmapEvent.window, rootWindow, 0, 0)
        XRemoveFromSaveSet(display, unmapEvent.window)
        XDestroyWindow(display, window.frame)

        windowManagerState.removeWindow(unmapEvent.window)
        moveNextWindowToTopOfStack(display, windowManagerState)
    } else if (windowManagerState.activeWindow != null) {
        XSetInputFocus(display, windowManagerState.activeWindow!!.id, RevertToNone, CurrentTime.convert())
    }

    windowManagerState.monitors.forEach { monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
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
    { measurements, window -> adjustWindowPositionAndSize(display, measurements, window) }

    windowManagerState.monitors.forEach { monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
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
    image: CPointer<XImage>,
    lcarsWindow: ULong,
    graphicsContexts: List<GC>,
    windowMoveFunction: Function1<Window, Monitor>
) {
    val activeWindow = windowManagerState.activeWindow ?: return
    val newMonitor = windowMoveFunction(activeWindow)
    val measurements = newMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(newMonitor))

    adjustWindowPositionAndSize(
        display,
        measurements,
        activeWindow
    )

    windowManagerState.monitors.forEach { monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
        drawFunction(
            graphicsContexts,
            lcarsWindow,
            display,
            monitor,
            image
        )
    }
}


private fun loadAppFromKeyBinding(keyBinding: String) {
    val programConfig = readFromConfig(KEY_CONFIG_FILE, keyBinding) ?: return
    println("::loadAppFromKeyBinding::loading app for $keyBinding ${programConfig.size}")
    runProgram(programConfig[0], programConfig)
}

private fun closeActiveWindow(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState
) {
    val activeWindow = windowManagerState.activeWindow ?: return

    val supportedProtocols = nativeHeap.allocPointerTo<ULongVarOf<ULong>>()
    val numSupportedProtocols = IntArray(1).pin()

    val protocolsResult = XGetWMProtocols(display, activeWindow.id, supportedProtocols.ptr, numSupportedProtocols.addressOf(0))
    val min = supportedProtocols.pointed!!.value
    val max = min + numSupportedProtocols.get()[0].toULong()

    if (protocolsResult != 0 && (windowManagerState.wmDeleteWindow in (min .. max))) {
        val msg = nativeHeap.alloc<XEvent>()
        msg.xclient.type = ClientMessage
        msg.xclient.message_type = windowManagerState.wmProtocols
        msg.xclient.window = activeWindow.id
        msg.xclient.format = 32
        msg.xclient.data.l[0] = windowManagerState.wmDeleteWindow.convert()
        XSendEvent(display, activeWindow.id, X_FALSE, 0, msg.ptr)
    } else {
        XKillClient(display, activeWindow.id)
    }
}

private fun isBetween(min: ULong, max: ULong, value: ULong) = value in min..max
