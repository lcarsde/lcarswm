package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import xlib.ReparentNotify
import xlib.XEvent

class ReparentNotifyHandler(
    private val logger: Logger,
    private val eventStore: EventStore,
) : XEventHandler {
    override val xEventType = ReparentNotify

    override fun handleEvent(event: XEvent): Boolean {
        val windowId = event.xreparent.window
        val parentId = event.xreparent.parent
        logger.logDebug("ReparentNotifyHandler::handleEvent::reparented $windowId to $parentId")
        eventStore.reparentSj.next(ReparentEvent(windowId, parentId))

        return false
    }
}
