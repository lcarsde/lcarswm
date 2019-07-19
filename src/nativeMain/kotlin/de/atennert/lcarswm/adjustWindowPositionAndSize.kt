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
    windowManagerConfig: WindowManagerConfig,
    windowId: UInt
) {
    val (x, y) = windowManagerConfig.defaultWindowPosition
    val (width, height) = windowManagerConfig.defaultWindowSize
    val mask = XCB_CONFIG_WINDOW_X or XCB_CONFIG_WINDOW_Y or
            XCB_CONFIG_WINDOW_WIDTH or XCB_CONFIG_WINDOW_HEIGHT

    val configList = listOf(x, y, width, height)
    val configData = UIntArray(4) { configList[it].convert() }

    xcb_configure_window(xcbConnection, windowId, mask.convert(), configData.toCValues())
    xcb_flush(xcbConnection)
}
