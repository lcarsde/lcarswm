package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.moveNextWindowToTopOfStack
import de.atennert.lcarswm.system.xEventApi
import de.atennert.lcarswm.system.xInputApi
import de.atennert.lcarswm.system.xWindowUtilApi
import de.atennert.lcarswm.windowactions.redrawRootWindow
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import xlib.*

/**
 * Remove the window from the wm data on window unmap.
 */
fun handleUnmapNotify(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Boolean {
    val unmapEvent = xEvent.xunmap
    println("::handleUnmapNotify::unmapped window: ${unmapEvent.window}")
    // only the active window can be closed, so make a new window active
    if (windowManagerState.hasWindow(unmapEvent.window) && unmapEvent.event != rootWindow) {
        val window = windowManagerState.windows.map { it.first }.single { it.id == unmapEvent.window }
        xEventApi().unmapWindow(display, window.frame)
        xEventApi().reparentWindow(display, unmapEvent.window, rootWindow, 0, 0)
        xWindowUtilApi().removeFromSaveSet(display, unmapEvent.window)
        xEventApi().destroyWindow(display, window.frame)

        windowManagerState.removeWindow(unmapEvent.window)
        moveNextWindowToTopOfStack(display, windowManagerState)
    } else if (windowManagerState.activeWindow != null) {
        xInputApi().setInputFocus(display, windowManagerState.activeWindow!!.id, RevertToNone, CurrentTime.convert())
    }

    redrawRootWindow(windowManagerState, graphicsContexts, rootWindow, display, image)
    return false
}
