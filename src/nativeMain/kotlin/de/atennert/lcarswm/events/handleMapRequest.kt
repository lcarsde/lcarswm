package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.addWindow
import de.atennert.lcarswm.system.api.SystemApi
import xlib.Window
import xlib.XEvent

/**
 *
 */
fun handleMapRequest(
    system: SystemApi,
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

    addWindow(system, windowManagerState, rootWindow, window, false)

    return false
}
