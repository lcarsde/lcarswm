package de.atennert.lcarswm

/**
 * Container class for the state of the window manager.
 */
class WindowManagerState(
    private var windowManagerSize: Pair<Int, Int>,
    val screenRoot: UInt,
    private val atomProvider: Function1<String, UInt>
) {

    private val defaultWindowPosition = Pair(0, 100) // TODO get real values

    private val defaultWindowSize = Pair(windowManagerSize.first, windowManagerSize.second / 2) // TODO get real values

    private val maximizedWindowPosition = Pair(5, 20) // TODO get real values

    private val maximizedWindowSize =
        Pair(windowManagerSize.first / 2, windowManagerSize.second / 2) // TODO get real values

    private val fullscreenWindowPosition = Pair(0, 0)

    private val fullscreenWindowSize = this.windowManagerSize

    val windows = hashMapOf<UInt, Window>()

    val wmState = atomProvider("WM_STATE")

    var screenMode = ScreenMode.NORMAL

    /**
     * @return the current window measurements in the form [x, y, width, height], depending on the current screenMode
     */
    val currentWindowMeasurements: List<Int>
        get() = when (this.screenMode) {
            ScreenMode.NORMAL -> windowMeasurementsToList(this.defaultWindowPosition, this.defaultWindowSize)
            ScreenMode.MAXIMIZED -> windowMeasurementsToList(this.maximizedWindowPosition, this.maximizedWindowSize)
            ScreenMode.FULLSCREEN -> windowMeasurementsToList(this.fullscreenWindowPosition, this.fullscreenWindowSize)
        }

    private fun windowMeasurementsToList(position: Pair<Int, Int>, size: Pair<Int, Int>): List<Int> {
        val (x, y) = position
        val (width, height) = size

        // why a list and not an array you ask? because no toCValues() on the array created with arrayOf :-(
        return listOf(x, y, width, height)
    }
}
