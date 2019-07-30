package de.atennert.lcarswm

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.toCValues
import xcb.*

/**
 * @return window ID of the generated root window
 */
fun setupLcarsWindow(
    xcbConnection: CPointer<xcb_connection_t>,
    screen: xcb_screen_t,
    windowId: UInt
) {
    val mask = XCB_CW_BACK_PIXEL
    val windowParametersArr = arrayOf(screen.black_pixel)
    val windowParameters = UIntArray(windowParametersArr.size) {windowParametersArr[it]}

    xcb_create_window(
        xcbConnection, XCB_COPY_FROM_PARENT.convert(), windowId, screen.root,
        0, 0, screen.width_in_pixels, screen.height_in_pixels, 0.convert(),
        XCB_WINDOW_CLASS_INPUT_OUTPUT.convert(), screen.root_visual, mask, windowParameters.toCValues()
    )

    xcb_map_window(xcbConnection, windowId)

    xcb_flush(xcbConnection)
}