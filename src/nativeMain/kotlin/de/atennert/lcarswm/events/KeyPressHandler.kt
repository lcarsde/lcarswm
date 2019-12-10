package de.atennert.lcarswm.events

import xlib.KeyPress
import xlib.XEvent

class KeyPressHandler : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        TODO()
    }
}
