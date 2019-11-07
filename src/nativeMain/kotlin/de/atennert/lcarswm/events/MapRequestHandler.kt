package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.log.Logger
import xlib.MapRequest
import xlib.XEvent

/**
 *
 */
class MapRequestHandler(
    private val logger: Logger,
    private val windowManagerState: WindowManagerStateHandler
) : XEventHandler{
    override val xEventType = MapRequest

    override fun handleEvent(event: XEvent): Boolean {
        val mapEvent = event.xmaprequest

        val isKnown = windowManagerState.hasWindow(mapEvent.window)

        logger.logDebug("::handleMapRequest::map request for window ${mapEvent.window}, parent: ${mapEvent.parent}, is known: $isKnown")
        return false
    }
}
