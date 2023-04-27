package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import xlib.MapRequest
import xlib.XEvent

/**
 *
 */
class MapRequestHandler(
    private val logger: Logger,
    private val eventStore: EventStore
) : XEventHandler{
    override val xEventType = MapRequest

    override fun handleEvent(event: XEvent): Boolean {
        val mapEvent = event.xmaprequest
        logger.logDebug("MapRequestHandler::handleEvent::map request for window ${mapEvent.window}, parent: ${mapEvent.parent}")

        eventStore.mapSj.next(mapEvent.window)

        return false
    }
}
