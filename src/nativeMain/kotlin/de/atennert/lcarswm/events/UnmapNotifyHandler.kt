package de.atennert.lcarswm.events

import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.windowactions.WindowRegistrationApi
import xlib.UnmapNotify
import xlib.XEvent

/**
 * Unregister known windows and redraw the root window on unmap notify.
 */
class UnmapNotifyHandler(
    private val logger: Logger,
    private val windowRegistration: WindowRegistrationApi,
    private val rootWindowDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = UnmapNotify

    override fun handleEvent(event: XEvent): Boolean {
        val window = event.xunmap.window
        val isWindowKnown = windowRegistration.isWindowManaged(window)

        logger.logDebug("UnmapNotifyHandler::handleEvent::unmapped window: $window, isKnown: $isWindowKnown")

        if (isWindowKnown) {
            windowRegistration.removeWindow(window)
        }

        rootWindowDrawer.drawWindowManagerFrame()

        return false
    }
}