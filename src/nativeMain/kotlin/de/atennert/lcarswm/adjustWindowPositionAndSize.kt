package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 * Adjust window position and size according to the current window manager setting.
 */
fun adjustWindowPositionAndSize(
    display: CPointer<Display>,
    windowMeasurements: List<Int>,
    window: Window
) {
    XMoveResizeWindow(
        display,
        window.frame,
        windowMeasurements[0],
        windowMeasurements[1],
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )

    XResizeWindow(
        display,
        window.id,
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )

    val e = nativeHeap.alloc<XEvent>()
    e.type = ConfigureNotify
    e.xconfigure.display = display
    e.xconfigure.event = window.id
    e.xconfigure.window = window.id
    e.xconfigure.x = windowMeasurements[0]
    e.xconfigure.y = windowMeasurements[1]
    e.xconfigure.width = windowMeasurements[2]
    e.xconfigure.height = windowMeasurements[3]
    e.xconfigure.border_width = 0
    e.xconfigure.above = None.convert()
    e.xconfigure.override_redirect = X_FALSE
    XSendEvent(display, window.id, X_FALSE, StructureNotifyMask, e.ptr)
}
