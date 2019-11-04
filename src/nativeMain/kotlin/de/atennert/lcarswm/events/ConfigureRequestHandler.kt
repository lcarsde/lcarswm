package de.atennert.lcarswm.events

import xlib.ConfigureRequest
import xlib.XEvent

/**
 *
 */
class ConfigureRequestHandler : XEventHandler {
    override val xEventType = ConfigureRequest

    override fun handleEvent(event: XEvent): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}