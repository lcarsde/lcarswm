package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.*
import xcb.*

/**
 * Map of event types to event handlers. DON'T EDIT THE MAPS CONTENT!!!
 */
val EVENT_HANDLERS =
    hashMapOf<XcbEvent, Function3<CPointer<xcb_connection_t>, WindowManagerState, CPointer<xcb_generic_event_t>, Boolean>>(
        Pair(XcbEvent.XCB_KEY_PRESS, { _, _, e -> handleKeyPress(e) }),
        Pair(XcbEvent.XCB_KEY_RELEASE, ::handleKeyRelease),
        Pair(XcbEvent.XCB_BUTTON_PRESS, { _, _, e -> handleButtonPress(e) }),
        Pair(XcbEvent.XCB_BUTTON_RELEASE, { _, _, e -> handleButtonRelease(e) }),
        Pair(XcbEvent.XCB_CONFIGURE_REQUEST, ::handleConfigureRequest),
        Pair(XcbEvent.XCB_MAP_REQUEST, ::handleMapRequest),
        Pair(XcbEvent.XCB_DESTROY_NOTIFY, { _, w, e -> handleDestroyNotify(w, e) }),
        Pair(XcbEvent.XCB_UNMAP_NOTIFY, { _, w, e -> handleUnmapNotify(w, e) })
    )

private val ROOT_WINDOW_ID = 0.toUInt()

private fun handleKeyPress(xEvent: CPointer<xcb_generic_event_t>): Boolean {
    @Suppress("UNCHECKED_CAST")
    val pressEvent = (xEvent as CPointer<xcb_key_press_event_t>).pointed
    val key = pressEvent.detail
    println("::handleKeyPress::Key pressed: $key")
    return false
}

private fun handleKeyRelease(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val releasedEvent = (xEvent as CPointer<xcb_key_release_event_t>).pointed
    val key = releasedEvent.detail
    println("::handleKeyRelease::Key released: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_Tab -> toggleScreenMode(xcbConnection, windowManagerState)
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
    println("::handleMapRequest::map request")
    @Suppress("UNCHECKED_CAST")
    val mapEvent = xEvent as CPointer<xcb_map_request_event_t>
    val windowId = mapEvent.pointed.window

    if (windowManagerState.getWindowMonitor(windowId) != null) {
        return false
    }

    // TODO setup window
    // TODO add window to workspace
    // TODO find monitor for window

    val windowMonitor = windowManagerState.addWindow(windowId, Window(windowId))

    println(
        "::handleMapRequest::monitor: ${windowMonitor.name} position: ${windowMonitor.getCurrentWindowMeasurements(
            windowManagerState.screenMode
        )}"
    )
    adjustWindowPositionAndSize(
        xcbConnection,
        windowMonitor.getCurrentWindowMeasurements(windowManagerState.screenMode),
        windowId
    )

    xcb_map_window(xcbConnection, mapEvent.pointed.window)

    val data = UIntArray(2)
    data[0] = XCB_ICCCM_WM_STATE_NORMAL
    data[1] = XCB_NONE.convert()

    xcb_change_property(
        xcbConnection, XCB_PROP_MODE_REPLACE.convert(), windowId,
        windowManagerState.wmState, windowManagerState.wmState,
        32.convert(), 2.convert(), data.toCValues()
    )

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
    println("::handleConfigureRequest::configure request")
    @Suppress("UNCHECKED_CAST")
    val configureEvent = (xEvent as CPointer<xcb_configure_request_event_t>).pointed

    val windowMonitor = windowManagerState.getWindowMonitor(configureEvent.window)
    if (windowMonitor != null) {
        adjustWindowPositionAndSize(
            xcbConnection,
            windowMonitor.getCurrentWindowMeasurements(windowManagerState.screenMode),
            configureEvent.window
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
    windowManagerState: WindowManagerState,
    xEvent: CPointer<xcb_generic_event_t>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val unmapEvent = (xEvent as CPointer<xcb_unmap_notify_event_t>).pointed
    windowManagerState.removeWindow(unmapEvent.window)
    return false
}

/**
 * Get RANDR information and update window management accordingly.
 */
fun handleRandrEvent(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState) {
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

    windowManagerState.updateMonitors(activeMonitors)
    { measurements, windowId -> adjustWindowPositionAndSize(xcbConnection, measurements, windowId) }

    nativeHeap.free(resourcesReply)
}

fun addMeasurementToMonitor(
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
