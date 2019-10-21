package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import xlib.XEvent

/**
 * Remove window from the wm data on window destroy.
 */
fun handleDestroyNotify(
    system: SystemApi,
    logger: Logger,
    windowManagerState: WindowManagerStateHandler,
    xEvent: XEvent
): Boolean {
    val destroyEvent = xEvent.xdestroywindow
    logger.logDebug("::handleDestroyNotify::destroy window: ${destroyEvent.window}")
    if (windowManagerState.hasWindow(destroyEvent.window)) {
        val window = windowManagerState.windows.map { it.first }.single { it.id == destroyEvent.window }
        system.unmapWindow(window.frame)
        system.removeFromSaveSet(destroyEvent.window)
        system.destroyWindow(window.frame)

        windowManagerState.removeWindow(destroyEvent.window)
    }
    return false
}
