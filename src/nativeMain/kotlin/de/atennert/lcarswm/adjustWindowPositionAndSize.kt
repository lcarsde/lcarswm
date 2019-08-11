package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 * Adjust window position and size according to the current window manager setting.
 */
fun adjustWindowPositionAndSize(
    display: CPointer<Display>,
    windowMeasurements: List<Int>,
    window: ULong
) {
    XMoveResizeWindow(
        display,
        window,
        windowMeasurements[0],
        windowMeasurements[1],
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )
}
