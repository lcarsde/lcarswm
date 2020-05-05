package de.atennert.lcarswm.events

import de.atennert.lcarswm.drawing.UIDrawing
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.window.AppMenuHandler
import de.atennert.lcarswm.window.WindowRegistration
import xlib.UnmapNotify
import xlib.XEvent

/**
 * Unregister known windows and redraw the root window on unmap notify.
 */
class UnmapNotifyHandler(
    private val logger: Logger,
    private val windowRegistration: WindowRegistration,
    private val rootWindowDrawer: UIDrawing,
    private val appMenuHandler: AppMenuHandler
) : XEventHandler {
    override val xEventType = UnmapNotify

    override fun handleEvent(event: XEvent): Boolean {
        val window = event.xunmap.window
        val isWindowKnown = windowRegistration.isWindowManaged(window)

        logger.logDebug("UnmapNotifyHandler::handleEvent::unmapped window: $window, isKnown: $isWindowKnown")

        if (isWindowKnown) {
            windowRegistration.removeWindow(window)
        } else if (appMenuHandler.isKnownAppMenu(window)) {
            appMenuHandler.removeWindow()
        }

        rootWindowDrawer.drawWindowManagerFrame()

        return false
    }
}