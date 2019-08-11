package de.atennert.lcarswm

import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.GC
import xlib.XImage

/**
 * Toggle screen mode including window resizing/repositioning
 */
fun toggleScreenMode(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    lcarsWindow: ULong,
    graphicsContexts: List<GC>
) {
    val screenMode = when (windowManagerState.screenMode) {
        ScreenMode.NORMAL -> ScreenMode.MAXIMIZED
        ScreenMode.MAXIMIZED -> ScreenMode.FULLSCREEN
        ScreenMode.FULLSCREEN -> ScreenMode.NORMAL
    }

    windowManagerState.updateScreenMode(screenMode)
    { measurements, windowId -> adjustWindowPositionAndSize(display, measurements, windowId) }

    windowManagerState.monitors.forEach {monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
        drawFunction(
            graphicsContexts,
            lcarsWindow,
            display,
            monitor,
            image
        )
    }
}