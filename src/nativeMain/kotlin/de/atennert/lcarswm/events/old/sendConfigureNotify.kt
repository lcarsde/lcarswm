package de.atennert.lcarswm.events.old

import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun sendConfigureNotify(
    eventApi: EventApi,
    window: Window,
    measurements: List<Int>
) {
    val e = nativeHeap.alloc<XEvent>()
    e.type = ConfigureNotify
    e.xconfigure.display = eventApi.getDisplay()
    e.xconfigure.event = window
    e.xconfigure.window = window
    e.xconfigure.x = measurements[0]
    e.xconfigure.y = measurements[1]
    e.xconfigure.width = measurements[2]
    e.xconfigure.height = measurements[3]
    e.xconfigure.border_width = 0
    e.xconfigure.above = None.convert()
    e.xconfigure.override_redirect = X_FALSE
    eventApi.sendEvent(window, false, StructureNotifyMask, e.ptr)
}
