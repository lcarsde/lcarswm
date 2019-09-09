package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.*
import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.GC
import xlib.XImage

fun moveActiveWindow(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: ULong,
    graphicsContexts: List<GC>,
    windowMoveFunction: Function1<Window, Monitor>
) {
    val activeWindow = windowManagerState.activeWindow ?: return
    val newMonitor = windowMoveFunction(activeWindow)
    val measurements = newMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(newMonitor))

    adjustWindowPositionAndSize(
        display,
        measurements,
        activeWindow
    )

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
