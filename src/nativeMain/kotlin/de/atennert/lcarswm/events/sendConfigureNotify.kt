package de.atennert.lcarswm.events

import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.monitor.WindowMeasurements
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun sendConfigureNotify(
    eventApi: EventApi,
    window: Window,
    measurements: WindowMeasurements
) {
    val e = nativeHeap.alloc<XEvent>()
    e.type = ConfigureNotify
    e.xconfigure.display = eventApi.getDisplay()
    e.xconfigure.event = window
    e.xconfigure.window = window
    e.xconfigure.x = measurements.x
    e.xconfigure.y = measurements.y
    e.xconfigure.width = measurements.width
    e.xconfigure.height = measurements.height
    e.xconfigure.border_width = 0
    e.xconfigure.above = None.convert()
    e.xconfigure.override_redirect = X_FALSE
    eventApi.sendEvent(window, false, StructureNotifyMask, e.ptr)
}
