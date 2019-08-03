package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.CPointer
import xcb.Display
import xcb.XImage
import xcb.xcb_flush

/**
 * Toggle screen mode including window resizing/repositioning
 */
fun toggleScreenMode(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState,
                     display: CPointer<Display>, image: CPointer<XImage>) {
    val screenMode = when (windowManagerState.screenMode) {
        ScreenMode.NORMAL -> ScreenMode.MAXIMIZED
        ScreenMode.MAXIMIZED -> ScreenMode.FULLSCREEN
        ScreenMode.FULLSCREEN -> ScreenMode.NORMAL
    }

    windowManagerState.updateScreenMode(screenMode)
    { measurements, windowId -> adjustWindowPositionAndSize(xcbConnection, measurements, windowId, false) }

    windowManagerState.monitors.forEach {monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
        drawFunction(
            xcbConnection,
            windowManagerState,
            display,
            monitor,
            image
        )
    }

    xcb_flush(xcbConnection)
}