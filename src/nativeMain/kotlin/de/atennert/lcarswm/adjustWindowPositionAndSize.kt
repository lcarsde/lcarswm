package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.toCValues
import xcb.*

/**
 * Adjust window position and size according to the current window manager setting.
 */
fun adjustWindowPositionAndSize(
    xcbConnection: CPointer<xcb_connection_t>,
    windowMeasurements: List<Int>,
    windowId: UInt
) {
    val mask = XCB_CONFIG_WINDOW_X or XCB_CONFIG_WINDOW_Y or
            XCB_CONFIG_WINDOW_WIDTH or XCB_CONFIG_WINDOW_HEIGHT

    val configData = UIntArray(4) { windowMeasurements[it].convert() }

    xcb_configure_window(xcbConnection, windowId, mask.convert(), configData.toCValues())
    xcb_flush(xcbConnection)
}
