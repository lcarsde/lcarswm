package de.atennert.lcarswm.events

import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.WindowRegistrationApi
import xlib.UnmapNotify
import xlib.Window
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
        val unmapEvent = event.xunmap

        windowRegistration.removeWindow(unmapEvent.window)

        rootWindowDrawer.drawWindowManagerFrame()

        return false
    }
}