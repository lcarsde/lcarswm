package de.atennert.lcarswm.events

import xlib.KeyRelease
import xlib.XEvent

/**
 *
 */
class KeyReleaseHandler : XEventHandler {
    override val xEventType = KeyRelease

    override fun handleEvent(event: XEvent): Boolean {
        return true
    }
}
