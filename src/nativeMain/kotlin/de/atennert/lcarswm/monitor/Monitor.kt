package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.window.WindowMeasurements
import de.atennert.rx.operators.map
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

    /**
     * Provides the monitors screen mode. Normal is converted to maximized for non-primary monitors.
     */
    val screenModeObs = monitorManager.screenModeObs
        .apply( map {
            if (!isPrimary && it == ScreenMode.NORMAL) {
                ScreenMode.MAXIMIZED
            } else {
                it
            }
        })



    /**
     * Provides the current window measurements in the form [x, y, width, height], depending on the current screenMode
     */
    val windowMeasurementsObs = screenModeObs
        .apply( map {
            when (it) {
                ScreenMode.NORMAL -> normalMeasurements
                ScreenMode.MAXIMIZED -> maximizedMeasurements
                ScreenMode.FULLSCREEN -> fullscreenMeasurements
            }
        })

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
        // TODO use Builder instead
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
     * Check if a pixel is shown on this monitor.
     * @return <code>true</code> if the pixel is on the monitor, <code>false</code> otherwise
     */
    fun isOnMonitor(x: Int, y: Int): Boolean {
        return this.x <= x && x < (this.x + this.width) && this.y <= y && y < (this.y + this.height)
    }
}