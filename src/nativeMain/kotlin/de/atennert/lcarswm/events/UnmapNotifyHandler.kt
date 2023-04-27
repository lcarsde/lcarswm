package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import xlib.UnmapNotify
import xlib.XEvent

/**
 * Unregister known windows and redraw the root window on unmap notify.
 */
class UnmapNotifyHandler(
    private val logger: Logger,
    private val eventStore: EventStore
) : XEventHandler {
    override val xEventType = UnmapNotify

    override fun handleEvent(event: XEvent): Boolean {
        val window = event.xunmap.window

        logger.logDebug("UnmapNotifyHandler::handleEvent::unmapped window: $window")
        eventStore.unmapSj.next(window)

        return false
    }
}