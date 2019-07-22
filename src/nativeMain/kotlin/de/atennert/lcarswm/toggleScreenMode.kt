package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.CPointer

/**
 * Toggle screen mode including window resizing/repositioning
 */
fun toggleScreenMode(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState) {
    when (windowManagerState.screenMode) {
        ScreenMode.NORMAL -> windowManagerState.screenMode = ScreenMode.MAXIMIZED
        ScreenMode.MAXIMIZED -> windowManagerState.screenMode = ScreenMode.FULLSCREEN
        ScreenMode.FULLSCREEN -> windowManagerState.screenMode = ScreenMode.NORMAL
    }

    val windowMeasurements = windowManagerState.currentWindowMeasurements
    windowManagerState.windows.keys
        .forEach { adjustWindowPositionAndSize(xcbConnection, windowMeasurements, it) }
}