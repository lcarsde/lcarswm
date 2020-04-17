package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import kotlinx.cinterop.convert
import xlib.RROutput

/**
 * Resource representing a physical monitor and its settings.
 */
data class Monitor(
    private val monitorManager: MonitorManager,
    val id: RROutput,
    val name: String,
    val isPrimary: Boolean
) {
    /** x coordinate of monitor on total screen surface */
    var x = 0
        private set

    /** y coordinate of monitor on total screen surface */
    var y = 0
        private set

    /** width of monitor pixel setting */
    var width = 800
        private set

    /** height of monitor pixel setting */
    var height = 600
        private set

    private val normalMeasurements: WindowMeasurements
        get() = WindowMeasurements.createNormal(x, y, width, height)

    private val maximizedMeasurements: WindowMeasurements
        get() = WindowMeasurements.createMaximized(x, y, width, height)

    private val fullscreenMeasurements: WindowMeasurements
        get() = WindowMeasurements.createFullscreen(x, y, width, height)

    private var isFullyInitialized = false

    override fun hashCode(): Int = id.convert()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Monitor

        if (id != other.id) return false

        return true
    }

    /**
     * Update the monitor measurement settings.
     * @return true if any setting changed, false otherwise
     */
    fun setMonitorMeasurements(x: Int, y: Int, width: UInt, height: UInt) {
        if (this.isFullyInitialized) {
            throw IllegalStateException("Tried to set values on monitor ${this.id}:$this.name but values are already set!")
        }

        this.x = x
        this.y = y
        this.width = width.convert()
        this.height = height.convert()

        this.isFullyInitialized = true
    }

    /**
     * @return true when the other monitor has the same measurements as this monitor, false otherwise
     */
    fun hasDifferentMeasurements(other: Monitor): Boolean {
        return this.x != other.x || this.y != other.y || this.width != other.width || this.height != other.height
    }

    /**
     * Check if a monitor is a clone of this monitor.
     * @return true if it is a clone, false otherwise
     */
    fun isClone(other: Monitor): Boolean {
        return other.x == this.x && other.y == this.y
    }

    /**
     * @return the current window measurements in the form [x, y, width, height], depending on the current screenMode
     */
    fun getWindowMeasurements(): WindowMeasurements = when (getScreenMode()) {
        ScreenMode.NORMAL -> normalMeasurements
        ScreenMode.MAXIMIZED -> maximizedMeasurements
        ScreenMode.FULLSCREEN -> fullscreenMeasurements
    }

    /**
     * Return the monitors screen mode. Normal is converted to maximized for non-primary monitors.
     */
    fun getScreenMode(): ScreenMode = when {
        !isPrimary && monitorManager.getScreenMode() == ScreenMode.NORMAL -> ScreenMode.MAXIMIZED
        else -> monitorManager.getScreenMode()
    }

    companion object {
        private fun windowMeasurementsToList(position: Pair<Int, Int>, size: Pair<Int, Int>, frameHeight: Int): List<Int> {
            val (x, y) = position
            val (width, height) = size

            // why a list and not an array you ask? because no toCValues() on the array created with arrayOf :-(
            return listOf(x, y, width, height, frameHeight)
        }
    }
}