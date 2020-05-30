package de.atennert.lcarswm.events

import de.atennert.lcarswm.system.api.InputApi
import de.atennert.lcarswm.window.WindowList
import xlib.ButtonPress
import xlib.ReplayPointer
import xlib.XEvent

class ButtonPressHandler(private val inputApi: InputApi, private val eventTime: EventTime, private val windowList: WindowList) : XEventHandler {
    override val xEventType = ButtonPress

    override fun handleEvent(event: XEvent): Boolean {
        if (windowList.isManaged(event.xbutton.window)) {
            inputApi.allowEvents(ReplayPointer, eventTime.lastEventTime)
        }
        return false
    }
}