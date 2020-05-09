package de.atennert.lcarswm.window

import de.atennert.lcarswm.BAR_HEIGHT_WITH_OFFSET
import de.atennert.lcarswm.events.sendConfigureNotify
import de.atennert.lcarswm.monitor.WindowMeasurements
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.convert

/**
 * Adjust window position and size according to the current window manager setting.
 */
fun adjustWindowPositionAndSize(
    eventApi: EventApi,
    windowMeasurements: WindowMeasurements,
    framedWindow: FramedWindow
) {
    eventApi.moveResizeWindow(
        framedWindow.titleBar,
        0,
        windowMeasurements.frameHeight - BAR_HEIGHT_WITH_OFFSET,
        windowMeasurements.width.convert(),
        BAR_HEIGHT_WITH_OFFSET.convert()
    )

    eventApi.moveResizeWindow(
        framedWindow.frame,
        windowMeasurements.x,
        windowMeasurements.y,
        windowMeasurements.width.convert(),
        windowMeasurements.frameHeight.convert()
    )

    eventApi.resizeWindow(
        framedWindow.id,
        windowMeasurements.width.convert(),
        windowMeasurements.height.convert()
    )

    sendConfigureNotify(eventApi, framedWindow.id, windowMeasurements)
}
