package de.atennert.lcarswm.events.old

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.windowactions.addWindow
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import xlib.Window
import xlib.XEvent

/**
 *
 */
fun handleMapRequest(
    system: SystemApi,
    logger: Logger,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    rootWindow: Window
): Boolean {
    val mapEvent = xEvent.xmaprequest
    val window = mapEvent.window

    logger.logDebug("::handleMapRequest::map request for window $window, parent: ${mapEvent.parent}")
    if (windowManagerState.getWindowMonitor(window) != null) {
        return false
    }

    addWindow(system, logger, windowManagerState, rootWindow, window, false)

    return false
}
