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
    private val system: SystemApi,
    private val windowRegistration: WindowRegistrationApi,
    private val rootWindowDrawer: UIDrawing,
    private val rootWindowId: Window
) : XEventHandler {
    override val xEventType = UnmapNotify

    override fun handleEvent(event: XEvent): Boolean {
        val unmapEvent = event.xunmap

        system.reparentWindow(unmapEvent.window, rootWindowId, 0, 0)
        system.removeFromSaveSet(unmapEvent.window)

        windowRegistration.removeWindow(unmapEvent.window)

        rootWindowDrawer.drawWindowManagerFrame()

        return false
    }
}