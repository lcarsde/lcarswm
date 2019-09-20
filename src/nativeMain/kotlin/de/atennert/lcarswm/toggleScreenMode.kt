package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.redrawRootWindow
import kotlinx.cinterop.CPointer
import xlib.GC
import xlib.Window
import xlib.XImage

/**
 * Toggle screen mode including window resizing/repositioning
 */
fun toggleScreenMode(
    system: SystemApi,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
) {
    val screenMode = when (windowManagerState.screenMode) {
        ScreenMode.NORMAL -> ScreenMode.MAXIMIZED
        ScreenMode.MAXIMIZED -> ScreenMode.FULLSCREEN
        ScreenMode.FULLSCREEN -> ScreenMode.NORMAL
    }

    windowManagerState.updateScreenMode(screenMode)
    { measurements, window -> adjustWindowPositionAndSize(system, measurements, window) }

    redrawRootWindow(windowManagerState, graphicsContexts, rootWindow, system, image)
}