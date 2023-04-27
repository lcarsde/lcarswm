package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import xlib.DestroyNotify
import xlib.XEvent

/**
 * The DestroyNotify event is triggered when a window was destroyed. If we (still) know the destroyed window, then we
 * need to clean up after it here.
 */
class DestroyNotifyHandler(
    private val logger: Logger,
    private val eventStore: EventStore,
) : XEventHandler {
    override val xEventType = DestroyNotify

    override fun handleEvent(event: XEvent): Boolean {
        val destroyedWindow = event.xdestroywindow.window
        logger.logDebug("DestroyNotifyHandler::handleEvent::clean up after destroyed window: $destroyedWindow")

        eventStore.destroySj.next(destroyedWindow)

        return false
    }
}