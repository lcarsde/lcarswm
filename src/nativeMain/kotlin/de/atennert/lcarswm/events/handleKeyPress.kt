package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.moveNextWindowToTopOfStack
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.moveActiveWindow
import kotlinx.cinterop.CPointer
import xlib.*

fun handleKeyPress(
    system: SystemApi,
    logger: Logger,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Boolean {
    val pressEvent = xEvent.xkey
    val key = pressEvent.keycode
    logger.logDebug("::handleKeyPress::Key pressed: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_Up -> moveActiveWindow(
            system,
            windowManagerState,
            image,
            rootWindow,
            graphicsContexts,
            windowManagerState::moveWindowToNextMonitor
        )
        XK_Down -> moveActiveWindow(
            system,
            windowManagerState,
            image,
            rootWindow,
            graphicsContexts,
            windowManagerState::moveWindowToPreviousMonitor
        )
        XK_Tab -> moveNextWindowToTopOfStack(system, logger, windowManagerState)
        else -> logger.logInfo("::handleKeyRelease::unknown key: $key")
    }

    return false
}
