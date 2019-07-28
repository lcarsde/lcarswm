package de.atennert.lcarswm

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.toCValues
import xcb.*

/**
 *
 */
fun addWindow(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState, windowId: UInt) {
    // TODO reparent window
    val windowMonitor = windowManagerState.addWindow(Window(windowId))

    println(
        "::addWindow::monitor: ${windowMonitor.name} position: ${windowMonitor.getCurrentWindowMeasurements(
            windowManagerState.screenMode
        )}"
    )

    xcb_change_save_set(xcbConnection, XCB_SET_MODE_INSERT.convert(), windowId)

    adjustWindowPositionAndSize(
        xcbConnection,
        windowMonitor.getCurrentWindowMeasurements(windowManagerState.screenMode),
        windowId
    )

    val data = UIntArray(2)
    data[0] = XCB_ICCCM_WM_STATE_NORMAL
    data[1] = XCB_NONE.convert()

    xcb_change_property(
        xcbConnection, XCB_PROP_MODE_REPLACE.convert(), windowId,
        windowManagerState.wmState, windowManagerState.wmState,
        32.convert(), 2.convert(), data.toCValues()
    )
}