package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.log.Logger
import xlib.XEvent

/**
 * Remove window from the wm data on window destroy.
 */
fun handleDestroyNotify(
    logger: Logger,
    windowManagerState: WindowManagerStateHandler,
    xEvent: XEvent
): Boolean {
    val destroyEvent = xEvent.xdestroywindow
    logger.logDebug("::handleDestroyNotify::destroy window: ${destroyEvent.window}")
    if (windowManagerState.hasWindow(destroyEvent.window)) {
        windowManagerState.removeWindow(destroyEvent.window)
    }
    return false
}
