package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.*
import xcb.*

/**
 * Map of event types to event handlers. DON'T EDIT THE MAPS CONTENT!!!
 */
val EVENT_HANDLERS =
    hashMapOf<XcbEvent, Function5<CPointer<xcb_connection_t>, WindowManagerState, CPointer<xcb_generic_event_t>, CPointer<Display>, CPointer<XImage>, Boolean>>(
        // TODO handle create
        Pair(XcbEvent.XCB_KEY_PRESS, { x, w, e, _, _ -> handleKeyPress(x, w, e) }),
        Pair(XcbEvent.XCB_KEY_RELEASE, ::handleKeyRelease),
        Pair(XcbEvent.XCB_BUTTON_PRESS, { _, _, e, _, _ -> handleButtonPress(e) }),
        Pair(XcbEvent.XCB_BUTTON_RELEASE, { _, _, e, _, _ -> handleButtonRelease(e) }),
        Pair(XcbEvent.XCB_CONFIGURE_REQUEST, { x, w, e, _, _ -> handleConfigureRequest(x, w, e) }),
        Pair(XcbEvent.XCB_MAP_REQUEST, { x, w, e, _, _ -> handleMapRequest(x, w, e) }),
        Pair(XcbEvent.XCB_MAP_NOTIFY, ::handleMapNotify),
        Pair(XcbEvent.XCB_DESTROY_NOTIFY, { _, w, e, _, _ -> handleDestroyNotify(w, e) }),
        Pair(XcbEvent.XCB_UNMAP_NOTIFY, { x, w, e, _, _ -> handleUnmapNotify(x, w, e) })
    )

private val ROOT_WINDOW_ID = 0.toUInt()

private fun handleKeyPress(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val pressEvent = (xEvent as CPointer<xcb_key_press_event_t>).pointed
    val key = pressEvent.detail
    println("::handleKeyPress::Key pressed: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_Up -> moveActiveWindow(xcbConnection, windowManagerState, windowManagerState::moveWindowToPreviousMonitor)
        XK_Down -> moveActiveWindow(xcbConnection, windowManagerState, windowManagerState::moveWindowToNextMonitor)
        XK_Tab -> moveNextWindowToTopOfStack(xcbConnection, windowManagerState)
        else -> println("::handleKeyRelease::unknown key: $key")
    }

    return false
}

private fun handleKeyRelease(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>,
    display: CPointer<Display>,
    image: CPointer<XImage>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val releasedEvent = (xEvent as CPointer<xcb_key_release_event_t>).pointed
    val key = releasedEvent.detail
    println("::handleKeyRelease::Key released: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_M -> toggleScreenMode(xcbConnection, windowManagerState, display, image)
        else -> println("::handleKeyRelease::unknown key: $key")
    }
    return false
}

private fun handleButtonPress(xEvent: CPointer<xcb_generic_event_t>): Boolean {
    @Suppress("UNCHECKED_CAST")
    val pressEvent = (xEvent as CPointer<xcb_button_press_event_t>).pointed
    val button = pressEvent.detail.toInt()

    println("::handleButtonPress::Button pressed: $button")
    if (button == 2 && pressEvent.child == ROOT_WINDOW_ID) {
        runProgram("/usr/bin/xterm")
    }
    return false
}

private fun handleButtonRelease(xEvent: CPointer<xcb_generic_event_t>): Boolean {
    @Suppress("UNCHECKED_CAST")
    val releaseEvent = (xEvent as CPointer<xcb_button_release_event_t>).pointed
    val button = releaseEvent.detail.toInt()

    println("::handleButtonRelease::Button released: $button")
    return button != 2 && releaseEvent.child == ROOT_WINDOW_ID // close lcarswm when right or left mouse buttons are pressed
}

private fun handleMapRequest(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val mapEvent = xEvent as CPointer<xcb_map_request_event_t>
    val windowId = mapEvent.pointed.window

    println("::handleMapRequest::map request for window $windowId")
    if (windowManagerState.getWindowMonitor(windowId) != null) {
        return false
    }

    addWindow(xcbConnection, windowManagerState, windowId)

    xcb_map_window(xcbConnection, mapEvent.pointed.window)

    xcb_flush(xcbConnection)
    return false
}

private fun handleMapNotify(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>,
    display: CPointer<Display>,
    image: CPointer<XImage>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val mapEvent = xEvent as CPointer<xcb_map_notify_event_t>
    val windowId = mapEvent.pointed.window
    println("::handleMapNotify::map notify for window $windowId")

    xcb_set_input_focus(
        xcbConnection,
        XCB_INPUT_FOCUS_POINTER_ROOT.convert(),
        windowId,
        XCB_CURRENT_TIME.convert()
    )

    if (windowId == windowManagerState.lcarsWindowId) {
        val drawFunction = DRAW_FUNCTIONS[windowManagerState.screenMode]!!
        drawFunction(
            xcbConnection,
            windowManagerState,
            display,
            image
        )
    }

    xcb_flush(xcbConnection)
    return false
}

/**
 * Filter the values that lcarswm requires and send the configuration to X.
 */
private fun handleConfigureRequest(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val configureEvent = (xEvent as CPointer<xcb_configure_request_event_t>).pointed

    println("::handleConfigureRequest::configure request for window ${configureEvent.window}")
    val windowMonitor = windowManagerState.getWindowMonitor(configureEvent.window)
    if (windowMonitor != null) {
        adjustWindowPositionAndSize(
            xcbConnection,
            windowMonitor.getCurrentWindowMeasurements(windowManagerState.screenMode),
            configureEvent.window,
            true
        )
        return false
    }

    val conf = WindowConfig(
        configureEvent.x.toLong(),
        configureEvent.y.toLong(),
        configureEvent.width.toLong(),
        configureEvent.height.toLong(),
        configureEvent.stack_mode.toLong(),
        configureEvent.sibling.toLong()
    )

    val (mask, valueList) = configureWindow(configureEvent.value_mask.toInt(), conf)

    if (!valueList.isEmpty()) {
        val uintList = valueList.map { it.toUInt() }
        val uintArray = UIntArray(7) { uintList.getOrElse(it) { 0.convert() } }

        xcb_configure_window(
            xcbConnection, configureEvent.window, mask.convert(), uintArray.toCValues()
        )
        xcb_flush(xcbConnection)
    }
    return false
}

/**
 * Remove window from the wm data on window destroy.
 */
private fun handleDestroyNotify(
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val destroyEvent = (xEvent as CPointer<xcb_destroy_notify_event_t>).pointed
    windowManagerState.removeWindow(destroyEvent.window)
    return false
}

/**
 * Remove the window from the wm data on window unmap.
 */
private fun handleUnmapNotify(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val unmapEvent = (xEvent as CPointer<xcb_unmap_notify_event_t>).pointed
    // only the active window can be closed, so make a new window active
    windowManagerState.removeWindow(unmapEvent.window)
    moveNextWindowToTopOfStack(xcbConnection, windowManagerState)
    return false
}

/**
 * Get RANDR information and update window management accordingly.
 */
fun handleRandrEvent(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    display: CPointer<Display>,
    image: CPointer<XImage>
) {
    // TODO check if we can optimize a little in here, current size change on single monitor is ~2s
    val resourcesCookie = xcb_randr_get_screen_resources_current(xcbConnection, windowManagerState.screenRoot)
    val resourcesReply = xcb_randr_get_screen_resources_current_reply(xcbConnection, resourcesCookie, null)

    if (resourcesReply == null) {
        println("::handleRandrEvent::no RANDR extension found")
        return
    }

    val timestamp = resourcesReply.pointed.config_timestamp

    val outputCount = xcb_randr_get_screen_resources_current_outputs_length(resourcesReply)
    val outputs = xcb_randr_get_screen_resources_current_outputs(resourcesReply)!!

    val sortedMonitors = Array(outputCount)
    { i -> Pair(outputs[i], xcb_randr_get_output_info(xcbConnection, outputs[i], timestamp)) }
        .asSequence()
        .map { (outputId, outputObject) ->
            Pair(outputId, xcb_randr_get_output_info_reply(xcbConnection, outputObject, null))
        }
        .filter { (_, outputObject) ->
            outputObject != null
        }
        .map { (outputId, outputObject) ->
            Triple(outputId, outputObject!!, getOutputName(outputObject))
        }
        .map { (outputId, outputObject, outputName) ->
            Triple(Monitor(outputId, outputName), outputObject.pointed.crtc, outputObject)
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
            addMeasurementToMonitor(xcbConnection, monitor, crtcReference, timestamp)
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
    val mask = XCB_CONFIG_WINDOW_WIDTH or XCB_CONFIG_WINDOW_HEIGHT

    val configDataAr = arrayOf(width, height)
    val configData = UIntArray(2) { configDataAr[it].convert() }

    xcb_configure_window(xcbConnection, windowManagerState.lcarsWindowId, mask.convert(), configData.toCValues())

    windowManagerState.screenSize = Pair(width, height)
    windowManagerState.updateMonitors(activeMonitors)
    { measurements, windowId -> adjustWindowPositionAndSize(xcbConnection, measurements, windowId, false) }

    val drawFunction = DRAW_FUNCTIONS[windowManagerState.screenMode]!!
    drawFunction(
        xcbConnection,
        windowManagerState,
        display,
        image
    )

    xcb_flush(xcbConnection)

    nativeHeap.free(resourcesReply)
}

private fun addMeasurementToMonitor(
    xcbConnection: CPointer<xcb_connection_t>,
    monitor: Monitor,
    crtcReference: xcb_randr_crtc_t,
    timestamp: xcb_timestamp_t
): Monitor {
    val crtcCookie = xcb_randr_get_crtc_info(xcbConnection, crtcReference, timestamp)
    val crtcPointer = xcb_randr_get_crtc_info_reply(xcbConnection, crtcCookie, null) ?: return monitor
    val crtc = crtcPointer.pointed

    monitor.setMeasurements(crtc.x, crtc.y, crtc.width, crtc.height)

    nativeHeap.free(crtcPointer)

    return monitor
}

/**
 * Get the name of the given output.
 */
private fun getOutputName(outputObject: CPointer<xcb_randr_get_output_info_reply_t>): String {
    val nameLength = xcb_randr_get_output_info_name_length(outputObject)
    val namePointer = xcb_randr_get_output_info_name(outputObject)!!

    val nameArray = ByteArray(nameLength) { i -> namePointer[i].toByte() }

    return nameArray.decodeToString()
}


private fun moveActiveWindow(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    windowMoveFunction: Function1<Window, Monitor>
) {
    val activeWindow = windowManagerState.activeWindow ?: return

    adjustWindowPositionAndSize(
        xcbConnection,
        windowMoveFunction(activeWindow).getCurrentWindowMeasurements(windowManagerState.screenMode),
        activeWindow.id,
        true
    )
}
