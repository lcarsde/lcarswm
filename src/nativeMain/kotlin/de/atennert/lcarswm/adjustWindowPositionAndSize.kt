package de.atennert.lcarswm

import de.atennert.lcarswm.events.sendConfigureNotify
import de.atennert.lcarswm.system.xEventApi
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
    xEventApi().moveResizeWindow(
        display,
        window.frame,
        windowMeasurements[0],
        windowMeasurements[1],
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )

    xEventApi().resizeWindow(
        display,
        window.id,
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )

    sendConfigureNotify(display, window.id, windowMeasurements)
}
