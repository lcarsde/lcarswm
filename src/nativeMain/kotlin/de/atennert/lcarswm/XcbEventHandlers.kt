package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toCValues
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
    val key = pressEvent.detail.toInt()
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
    val key = releasedEvent.detail.toInt()
    println("::handleKeyRelease::Key released: $key")

    toggleScreenMode(xcbConnection, windowManagerState)
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

    if (windowManagerState.windows.containsKey(windowId)) return false

    // TODO setup window
    // TODO add window to workspace
    // TODO find monitor for window

    adjustWindowPositionAndSize(xcbConnection, windowManagerState.currentWindowMeasurements, windowId)

    windowManagerState.windows[windowId] = Window(windowId)

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

    if (windowManagerState.windows.containsKey(configureEvent.window)) {
        adjustWindowPositionAndSize(
            xcbConnection,
            windowManagerState.currentWindowMeasurements,
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
    windowManagerState.windows.remove(destroyEvent.window)
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
    windowManagerState.windows.remove(unmapEvent.window)
    return false
}

// TODO RANDR handler
