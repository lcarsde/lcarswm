package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.InputApi
import de.atennert.lcarswm.window.WindowFocusHandler
import de.atennert.lcarswm.window.WindowList
import xlib.ButtonPress
import xlib.ReplayPointer
import xlib.XEvent

class ButtonPressHandler(
    private val logger: Logger,
    private val inputApi: InputApi,
    private val windowList: WindowList,
    private val focusHandler: WindowFocusHandler
) : XEventHandler {
    override val xEventType = ButtonPress

    override fun handleEvent(event: XEvent): Boolean {
        val windowId = event.xbutton.window
        val subWindowId = event.xbutton.subwindow
        logger.logDebug("ButtonPressHandler::handleEvent::windowId: $windowId, sub window: $subWindowId")

        val window = windowList.getByFrame(windowId) ?: return false

        focusHandler.setFocusedWindow(window.id)

        inputApi.allowEvents(ReplayPointer, event.xbutton.time)
        return false
    }
}