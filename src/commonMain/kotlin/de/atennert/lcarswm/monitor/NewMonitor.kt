package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.window.WindowMeasurements
import kotlin.properties.Delegates

expect fun <Output> getOutputHash(output: Output): Int

data class NewMonitor<Output> private constructor(
    val id: Output,
    val name: String,
    val isPrimary: Boolean,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val screenMode: ScreenMode,
) {
    val windowMeasurements = when (screenMode) {
        ScreenMode.NORMAL -> WindowMeasurements::createNormal
        ScreenMode.MAXIMIZED -> WindowMeasurements::createMaximized
        ScreenMode.FULLSCREEN -> WindowMeasurements::createFullscreen
    }(x, y, width, height)

    /**
     * Check if a pixel is shown on this monitor.
     * @return <code>true</code> if the pixel is on the monitor, <code>false</code> otherwise
     */
    fun isOnMonitor(x: Int, y: Int): Boolean {
        return this.x <= x && x < (this.x + this.width) && this.y <= y && y < (this.y + this.height)
    }

    class Builder<Output>(private val id: Output) {
        private var name by Delegates.notNull<String>()
        private var isPrimary by Delegates.notNull<Boolean>()
        var x by Delegates.notNull<Int>()
            private set
        var y by Delegates.notNull<Int>()
            private set
        private var width by Delegates.notNull<Int>()
        var height by Delegates.notNull<Int>()
            private set
        private var screenMode by Delegates.notNull<ScreenMode>()

        fun setName(name: String) = apply { this.name = name }
        fun setPrimary(isPrimary: Boolean) = apply { this.isPrimary = isPrimary }
        fun setX(x: Int) = apply { this.x = x }
        fun setY(y: Int) = apply { this.y = y }
        fun setWidth(width: Int) = apply { this.width = width }
        fun setHeight(height: Int) = apply { this.height = height }
        fun setScreenMode(screenMode: ScreenMode) = apply { this.screenMode = screenMode }
        fun build() = NewMonitor(id, name, isPrimary, x, y, width, height, if (!isPrimary && screenMode == ScreenMode.NORMAL) ScreenMode.MAXIMIZED else screenMode)
    }
}