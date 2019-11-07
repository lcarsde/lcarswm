package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.system.api.SystemApi
import xlib.MapRequest
import xlib.XEvent

/**
 *
 */
class MapRequestHandler(
    private val systemApi: SystemApi,
    private val windowManagerState: WindowManagerStateHandler
) : XEventHandler{
    override val xEventType = MapRequest

    override fun handleEvent(event: XEvent): Boolean {
        return false
    }
}
