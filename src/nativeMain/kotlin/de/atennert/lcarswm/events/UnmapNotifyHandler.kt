package de.atennert.lcarswm.events

import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.windowactions.WindowRegistrationApi
import xlib.UnmapNotify
import xlib.XEvent

/**
 *
 */
class UnmapNotifyHandler(
    private val windowRegistration: WindowRegistrationApi,
    private val rootWindowDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = UnmapNotify

    override fun handleEvent(event: XEvent): Boolean {
        val window = event.xunmap.window
        val isWindowKnown = windowRegistration.isWindowManaged(window)

        if (isWindowKnown) {
            windowRegistration.removeWindow(window)
        }

        rootWindowDrawer.drawWindowManagerFrame()

        return false
    }
}