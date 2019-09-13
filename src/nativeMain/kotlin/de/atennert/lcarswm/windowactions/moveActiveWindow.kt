package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.*
import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.GC
import xlib.Window
import xlib.XImage

fun moveActiveWindow(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>,
    windowMoveFunction: Function1<WindowContainer, Monitor>
) {
    val activeWindow = windowManagerState.activeWindow ?: return
    val newMonitor = windowMoveFunction(activeWindow)
    val measurements = newMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(newMonitor))

    adjustWindowPositionAndSize(
        display,
        measurements,
        activeWindow
    )

    redrawRootWindow(windowManagerState, graphicsContexts, rootWindow, display, image)
}
