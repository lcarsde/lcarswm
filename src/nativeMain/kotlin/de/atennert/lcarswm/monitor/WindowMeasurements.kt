package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.*

/**
 * Container class for window measurements.
 */
data class WindowMeasurements(val x: Int, val y: Int, val width: Int, val height: Int, val frameHeight: Int) {

    companion object {
        /** Create window measurements for normal mode windows (primary screen exclusive mode) */
        fun createNormal(monitorX: Int, monitorY: Int, monitorWidth: Int, monitorHeight: Int): WindowMeasurements {
            val leftWindowOffset = SIDE_BAR_WIDTH + INNER_CORNER_RADIUS + BAR_GAP_SIZE
            val upperWindowOffset = 2 * BAR_HEIGHT + 3 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_AREA_HEIGHT

            return WindowMeasurements(
                monitorX + leftWindowOffset,
                monitorY + upperWindowOffset,
                monitorWidth - leftWindowOffset - BAR_GAP_SIZE - BAR_END_WIDTH,
                monitorHeight - upperWindowOffset - BAR_GAP_SIZE - INNER_CORNER_RADIUS - BAR_HEIGHT,
                monitorHeight - upperWindowOffset
            )
        }

        /** Create window measurements for maximized windows */
        fun createMaximized(monitorX: Int, monitorY: Int, monitorWidth: Int, monitorHeight: Int): WindowMeasurements {
            val leftWindowOffset = BAR_GAP_SIZE + BAR_END_WIDTH
            val upperWindowOffset = BAR_HEIGHT + BAR_GAP_SIZE

            return WindowMeasurements(
                monitorX + leftWindowOffset,
                monitorY + upperWindowOffset,
                monitorWidth - 2 * leftWindowOffset,
                monitorHeight - 2 * upperWindowOffset,
                monitorHeight - upperWindowOffset
            )
        }

        /** Create window measurements for fullscreen windows */
        fun createFullscreen(monitorX: Int, monitorY: Int, monitorWidth: Int, monitorHeight: Int): WindowMeasurements {
            return WindowMeasurements(monitorX, monitorY, monitorWidth, monitorHeight, monitorHeight)
        }
    }
}