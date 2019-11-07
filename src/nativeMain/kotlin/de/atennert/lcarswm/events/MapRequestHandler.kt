package de.atennert.lcarswm.events

import xlib.MapRequest
import xlib.XEvent

/**
 *
 */
class MapRequestHandler : XEventHandler{
    override val xEventType = MapRequest

    override fun handleEvent(event: XEvent): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
