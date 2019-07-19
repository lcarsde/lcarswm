package de.atennert.lcarswm

/**
 *
 */
class WindowManagerConfig(
    private val windowManagerSize: Pair<Int, Int>,
    val screenRoot: UInt,
    private val atomProvider: Function1<String, UInt>) {

    val defaultWindowPosition = Pair(0, 100) // TODO get real values

    val defaultWindowSize = Pair(windowManagerSize.first, windowManagerSize.second / 2) // TODO get real values

    val maximizedWindowPosition = Pair(5, 20) // TODO get real values

    val maximizedWindowSize = Pair(windowManagerSize.first / 2, windowManagerSize.second / 2) // TODO get real values

    val fullscreenWindowPosition = Pair(0, 0)

    val fullscreenWindowSize = this.windowManagerSize

    val windows = hashMapOf<UInt, Window>()

    val wm_state = atomProvider("WM_STATE")
}
