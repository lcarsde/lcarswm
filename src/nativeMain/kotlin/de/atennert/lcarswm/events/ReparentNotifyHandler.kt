package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.window.WindowRegistration
import xlib.ReparentNotify
import xlib.XEvent

class ReparentNotifyHandler(
    private val logger: Logger,
    private val windowRegistration: WindowRegistration
) : XEventHandler {
    override val xEventType = ReparentNotify

    override fun handleEvent(event: XEvent): Boolean {
        val windowId = event.xreparent.window
        val parentId = event.xreparent.parent
        logger.logDebug("ReparentNotifyHandler::handleEvent::reparented $windowId to $parentId")

        // bad reparenting ... remove the window from the registration
        if (!windowRegistration.isWindowParentedBy(windowId, parentId)) {
            windowRegistration.removeWindow(windowId)
        }
        return false
    }
}
