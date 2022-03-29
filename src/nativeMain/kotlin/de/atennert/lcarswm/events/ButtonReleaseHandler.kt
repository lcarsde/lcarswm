package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.mouse.MoveWindowManager
import xlib.ButtonRelease
import xlib.XEvent

class ButtonReleaseHandler(
    private val logger: Logger,
    private val moveWindowManager: MoveWindowManager
) : XEventHandler {
    override val xEventType = ButtonRelease

    override fun handleEvent(event: XEvent): Boolean {
        val windowId = event.xbutton.window
        val subWindowId = event.xbutton.subwindow
        logger.logDebug("ButtonReleaseHandler::handleEvent::windowId: $windowId, sub window: $subWindowId, x y: ${event.xbutton.x} ${event.xbutton.y} (${event.xbutton.x_root} ${event.xbutton.y_root})")

        moveWindowManager.release()
        return false
    }
}