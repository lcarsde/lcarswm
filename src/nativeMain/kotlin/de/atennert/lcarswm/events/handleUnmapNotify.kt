package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.moveNextWindowToTopOfStack
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.redrawRootWindow
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import xlib.*

/**
 * Remove the window from the wm data on window unmap.
 */
fun handleUnmapNotify(
    system: SystemApi,
    logger: Logger,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Boolean {
    val unmapEvent = xEvent.xunmap
    logger.logDebug("::handleUnmapNotify::unmapped window: ${unmapEvent.window}")

    if (windowManagerState.hasWindow(unmapEvent.window) && unmapEvent.event != rootWindow) {
        val window = windowManagerState.windows.map { it.first }.single { it.id == unmapEvent.window }
        system.unmapWindow(window.frame)
        system.reparentWindow(unmapEvent.window, rootWindow, 0, 0)
        system.removeFromSaveSet(unmapEvent.window)
        system.destroyWindow(window.frame)

        windowManagerState.removeWindow(unmapEvent.window)
    }

    redrawRootWindow(windowManagerState, graphicsContexts, rootWindow, system, image)
    return false
}
