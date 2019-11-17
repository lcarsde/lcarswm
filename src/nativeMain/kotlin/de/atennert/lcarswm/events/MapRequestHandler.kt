package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.windowactions.WindowRegistration
import xlib.MapRequest
import xlib.XEvent

/**
 *
 */
class MapRequestHandler(
    private val logger: Logger,
    private val windowRegistration: WindowRegistration
) : XEventHandler{
    override val xEventType = MapRequest

    override fun handleEvent(event: XEvent): Boolean {
        val mapEvent = event.xmaprequest

        val isWindowKnown = windowRegistration.isWindowManaged(mapEvent.window)

        logger.logDebug("::handleMapRequest::map request for window ${mapEvent.window}, parent: ${mapEvent.parent}, is known: $isWindowKnown")

        if (!isWindowKnown) {
            windowRegistration.addWindow(mapEvent.window, false)
        }

        return false
    }
}
