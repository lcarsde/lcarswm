package de.atennert.lcarswm

import de.atennert.lcarswm.events.old.sendConfigureNotify
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.convert

/**
 * Adjust window position and size according to the current window manager setting.
 */
fun adjustWindowPositionAndSize(
    eventApi: EventApi,
    windowMeasurements: List<Int>,
    window: WindowContainer
) {
    eventApi.moveResizeWindow(
        window.frame,
        windowMeasurements[0],
        windowMeasurements[1],
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )

    eventApi.resizeWindow(
        window.id,
        windowMeasurements[2].convert(),
        windowMeasurements[3].convert()
    )

    sendConfigureNotify(eventApi, window.id, windowMeasurements)
}
