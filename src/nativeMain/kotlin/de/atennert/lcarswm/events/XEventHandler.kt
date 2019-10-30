package de.atennert.lcarswm.events

import xlib.XEvent

interface XEventHandler {
    /** The handled X event type */
    val xEventType: Int

    /**
     * @return true if event evaluation and WM shall stop, false otherwise
     */
    fun handleEvent(event: XEvent): Boolean
}