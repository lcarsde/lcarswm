package de.atennert.lcarswm

import de.atennert.lcarswm.events.sendConfigureNotify
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.convert

/**
 * Adjust window position and size according to the current window manager setting.
 */
fun adjustWindowPositionAndSize(
    eventApi: EventApi,
    windowMeasurements: List<Int>,
    framedWindow: FramedWindow
) {
    eventApi.moveResizeWindow(
        framedWindow.titleBar,
        0,
        windowMeasurements[4] - 41,
        windowMeasurements[2].convert(),
        41.convert()
    )

    eventApi.moveResizeWindow(
        framedWindow.frame,
        windowMeasurements[0],
        windowMeasurements[1],
        windowMeasurements[2].convert(),
        windowMeasurements[4].convert()
    )

    eventApi.resizeWindow(
        framedWindow.id,
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )

    sendConfigureNotify(eventApi, framedWindow.id, windowMeasurements)
}
