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

    var x = 0
        private set

    var y = 0
        private set

    var width = 800
        private set

    var height = 600
        private set

    private val defaultWindowPosition get() = Pair(x + 208, y + 242)

    private val defaultWindowSize get() = Pair(width - 248, height - 308)

    private val maximizedWindowPosition get() = Pair(x + 40, y + 48)

    private val maximizedWindowSize get() = Pair(width - 80, height - 96)

    private val fullscreenWindowPosition get() = Pair(x, y)

    private val fullscreenWindowSize get() = Pair(width, height)

    var isFullyInitialized = false
        private set

    override fun hashCode(): Int {
        return id.convert()
    }

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
    fun getWindowMeasurements(): List<Int> = when (monitorManager.getScreenMode()) {
        ScreenMode.NORMAL -> windowMeasurementsToList(
            this.defaultWindowPosition,
            this.defaultWindowSize
        )
        ScreenMode.MAXIMIZED -> windowMeasurementsToList(
            this.maximizedWindowPosition,
            this.maximizedWindowSize
        )
        ScreenMode.FULLSCREEN -> windowMeasurementsToList(
            this.fullscreenWindowPosition,
            this.fullscreenWindowSize
        )
    }

    fun getScreenMode(): ScreenMode = when {
        !isPrimary && monitorManager.getScreenMode() == ScreenMode.NORMAL -> ScreenMode.MAXIMIZED
        else -> monitorManager.getScreenMode()
    }

    companion object {
        private fun windowMeasurementsToList(position: Pair<Int, Int>, size: Pair<Int, Int>): List<Int> {
            val (x, y) = position
            val (width, height) = size

            // why a list and not an array you ask? because no toCValues() on the array created with arrayOf :-(
            return listOf(x, y, width, height)
        }
    }
}