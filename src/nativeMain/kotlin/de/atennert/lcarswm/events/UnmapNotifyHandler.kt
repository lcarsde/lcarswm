package de.atennert.lcarswm.events

import xlib.UnmapNotify
import xlib.XEvent

/**
 *
 */
class UnmapNotifyHandler : XEventHandler {
    override val xEventType = UnmapNotify

    override fun handleEvent(event: XEvent): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}