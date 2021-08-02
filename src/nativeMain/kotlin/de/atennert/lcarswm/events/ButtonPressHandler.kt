package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.mouse.MoveWindowManager
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
    private val focusHandler: WindowFocusHandler,
    private val moveWindowManager: MoveWindowManager
) : XEventHandler {
    override val xEventType = ButtonPress

    override fun handleEvent(event: XEvent): Boolean {
        val windowId = event.xbutton.window
        val subWindowId = event.xbutton.subwindow
        logger.logDebug("ButtonPressHandler::handleEvent::windowId: $windowId, sub window: $subWindowId, x y: ${event.xbutton.x} ${event.xbutton.y} (${event.xbutton.x_root} ${event.xbutton.y_root})")

        windowList.getByAny(windowId)?.let { window ->
            logger.logDebug("handle focus")
            focusHandler.setFocusedWindow(window.id)

            if (windowId == window.titleBar) {
                moveWindowManager.press(window, event.xbutton.x_root, event.xbutton.y_root)
            }
        }

        windowList.get(windowId)?.let {
            logger.logDebug("replay event")
            inputApi.allowEvents(ReplayPointer, event.xbutton.time)
        }
        return false
    }
}