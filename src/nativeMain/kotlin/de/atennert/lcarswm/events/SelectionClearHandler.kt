package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import xlib.SelectionClear
import xlib.XEvent

class SelectionClearHandler(private val logger: Logger) : XEventHandler {
    override val xEventType = SelectionClear

    override fun handleEvent(event: XEvent): Boolean {
        logger.logInfo("SelectionClearHandler::handleEvent::triggering shutdown - other WM calls")
        return true
    }
}