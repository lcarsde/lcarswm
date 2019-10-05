package de.atennert.lcarswm.events

import xlib.XEvent

interface XEventHandler {
    val xEventType: Int

    fun handleEvent(event: XEvent)
}