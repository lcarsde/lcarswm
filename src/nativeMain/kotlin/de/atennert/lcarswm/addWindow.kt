package de.atennert.lcarswm

import kotlinx.cinterop.*
import xcb.*

/**
 *
 */
fun addWindow(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState, windowId: UInt) {
    val windowAttributeCookie = xcb_get_window_attributes(xcbConnection, windowId)
    val windowAttributes = xcb_get_window_attributes_reply(xcbConnection, windowAttributeCookie, null)!!

    if (windowAttributes.pointed.override_redirect.toInt() != 0) {
        nativeHeap.free(windowAttributes)
        return
    }

    val windowMonitor = windowManagerState.addWindow(Window(windowId))

    println(
        "::addWindow::monitor: ${windowMonitor.name} position: ${windowMonitor.getCurrentWindowMeasurements(
            windowManagerState.screenMode
        )}"
    )

    xcb_change_save_set(xcbConnection, XCB_SET_MODE_INSERT.convert(), windowId)

    val measurements = windowMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowMonitor))

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