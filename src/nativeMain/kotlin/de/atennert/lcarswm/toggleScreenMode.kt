package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.CPointer
import xcb.xcb_flush

/**
 * Toggle screen mode including window resizing/repositioning
 */
fun toggleScreenMode(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState) {
    val screenMode = when (windowManagerState.screenMode) {
        ScreenMode.NORMAL -> ScreenMode.MAXIMIZED
        ScreenMode.MAXIMIZED -> ScreenMode.FULLSCREEN
        ScreenMode.FULLSCREEN -> ScreenMode.NORMAL
    }

    windowManagerState.updateScreenMode(screenMode)
    { measurements, windowId -> adjustWindowPositionAndSize(xcbConnection, measurements, windowId, false) }

    val drawFunction = DRAW_FUNCTIONS[windowManagerState.screenMode]!!
    drawFunction(
        xcbConnection,
        windowManagerState
    )

    xcb_flush(xcbConnection)
}