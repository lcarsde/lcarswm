package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.DRAW_FUNCTIONS
import de.atennert.lcarswm.WindowManagerState
import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.GC
import xlib.Window
import xlib.XImage

/**
 * Draw the window manager frames on the root window.
 */
fun redrawRootWindow(
    windowManagerState: WindowManagerState,
    graphicsContexts: List<GC>,
    rootWindow: Window,
    display: CPointer<Display>,
    image: CPointer<XImage>
) {
    windowManagerState.monitors.forEach { monitor ->
        val monitorScreenMode = windowManagerState.getScreenModeForMonitor(monitor)
        val drawFunction = DRAW_FUNCTIONS[monitorScreenMode]!!
        drawFunction(
            graphicsContexts,
            rootWindow,
            display,
            monitor,
            image
        )
    }
}
