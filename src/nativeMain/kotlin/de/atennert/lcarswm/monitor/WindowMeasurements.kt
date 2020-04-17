package de.atennert.lcarswm.monitor

/**
 * Container class for window measurements.
 */
data class WindowMeasurements(val x: Int, val y: Int, val width: Int, val height: Int, val frameHeight: Int) {

    companion object {
        /** Create window measurements for normal mode windows (primary screen exclusive mode) */
        fun createNormal(monitorX: Int, monitorY: Int, monitorWidth: Int, monitorHeight: Int): WindowMeasurements {
            return WindowMeasurements(
                monitorX + 208,
                monitorY + 242,
                monitorWidth - 248,
                monitorHeight - 308,
                monitorHeight - 242
            )
        }

        /** Create window measurements for maximized windows */
        fun createMaximized(monitorX: Int, monitorY: Int, monitorWidth: Int, monitorHeight: Int): WindowMeasurements {
            return WindowMeasurements(
                monitorX + 40,
                monitorY + 48,
                monitorWidth - 80,
                monitorHeight - 96,
                monitorHeight - 48
            )
        }

        /** Create window measurements for fullscreen windows */
        fun createFullscreen(monitorX: Int, monitorY: Int, monitorWidth: Int, monitorHeight: Int): WindowMeasurements {
            return WindowMeasurements(monitorX, monitorY, monitorWidth, monitorHeight, monitorHeight)
        }
    }
}