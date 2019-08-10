package de.atennert.lcarswm

import kotlinx.cinterop.*
import xcb.*

/**
 *
 */
fun addWindow(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState, windowId: UInt, isSetup: Boolean) {
    val windowAttributeCookie = xcb_get_window_attributes(xcbConnection, windowId)
    val windowAttributes = xcb_get_window_attributes_reply(xcbConnection, windowAttributeCookie, null)

    if (windowAttributes == null) {
        println("::addWindow::no attributes")
        return
    }

    if (windowAttributes.pointed.override_redirect.toInt() != 0 || (isSetup &&
            windowAttributes.pointed.map_state.toUInt() != XCB_MAP_STATE_VIEWABLE)) {
        println("::addWindow::skipping window $windowId")
        nativeHeap.free(windowAttributes)
        return
    }
    val wap = windowAttributes.pointed

    val windowMonitor = windowManagerState.addWindow(Window(windowId))

    val measurements = windowMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowMonitor))

    println(
        "::addWindow::monitor: ${windowMonitor.name}, setup: $isSetup, position: $measurements, class: ${wap._class}, b-grav: ${wap.bit_gravity}, propagate mask: ${wap.do_not_propagate_mask}, map state: ${wap.map_state}, redirect: ${wap.override_redirect}, sequence: ${wap.sequence}, map installed: ${wap.map_is_installed}, visual: ${wap.visual}, w-grav: ${wap.win_gravity}"
    )

    xcb_change_save_set(xcbConnection, XCB_SET_MODE_INSERT.convert(), windowId)

    xcb_reparent_window(xcbConnection, windowId, windowManagerState.lcarsWindowId, measurements[0].convert(), measurements[1].convert())

    adjustWindowPositionAndSize(
        xcbConnection,
        measurements,
        windowId,
        false
    )

    val data = UIntArray(2)
    data[0] = XCB_ICCCM_WM_STATE_NORMAL
    data[1] = XCB_NONE.convert()

    xcb_change_property(
        xcbConnection, XCB_PROP_MODE_REPLACE.convert(), windowId,
        windowManagerState.wmState, windowManagerState.wmState,
        32.convert(), 2.convert(), data.toCValues()
    )

    xcb_flush(xcbConnection)
    nativeHeap.free(windowAttributes)
}