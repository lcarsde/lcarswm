package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerState
import xlib.XEvent

/**
 * Remove window from the wm data on window destroy.
 */
fun handleDestroyNotify(
    windowManagerState: WindowManagerState,
    xEvent: XEvent
): Boolean {
    val destroyEvent = xEvent.xdestroywindow
    println("::handleDestroyNotify::destroy window: ${destroyEvent.window}")
    windowManagerState.removeWindow(destroyEvent.window)
    return false
}
