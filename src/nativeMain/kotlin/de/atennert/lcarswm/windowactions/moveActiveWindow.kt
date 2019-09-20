package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.*
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.GC
import xlib.Window
import xlib.XImage

fun moveActiveWindow(
    system: SystemApi,
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
        system,
        measurements,
        activeWindow
    )

    redrawRootWindow(windowManagerState, graphicsContexts, rootWindow, system, image)
}
