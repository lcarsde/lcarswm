package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.addWindow
import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.Window
import xlib.XEvent

/**
 *
 */
fun handleMapRequest(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    rootWindow: Window
): Boolean {
    val mapEvent = xEvent.xmaprequest
    val window = mapEvent.window

    println("::handleMapRequest::map request for window $window, parent: ${mapEvent.parent}")
    if (windowManagerState.getWindowMonitor(window) != null) {
        return false
    }

    addWindow(display, windowManagerState, rootWindow, window, false)

    return false
}

