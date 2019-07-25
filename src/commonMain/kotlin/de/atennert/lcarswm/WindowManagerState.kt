package de.atennert.lcarswm

/**
 * Container class for the state of the window manager.
 */
class WindowManagerState( // TODO find a better name
    val screenRoot: UInt,
    private val atomProvider: Function1<String, UInt>
) {

    val wmState = atomProvider("WM_STATE")

    var screenMode = ScreenMode.NORMAL
        private set

    /** map that holds the key sym to all registered key codes */
    val keyboardKeys = HashMap<UByte, Int>()

    private val monitors = ArrayList<Monitor>(3)

    /**
     * @return the monitor to which the window was added
     */
    fun addWindow(windowId: UInt, window: Window): Monitor {
        monitors[0].windows[windowId] = window
        return monitors[0]
    }

    fun removeWindow(windowId: UInt) {
        for (monitor in monitors) {
            if (monitor.windows.remove(windowId) != null) {
                return
            }
        }
    }

    fun getWindowMonitor(windowId: UInt): Monitor? {
        return monitors.find { it.windows.containsKey(windowId) }
    }

    fun updateMonitors(monitors: List<Monitor>, updateWindowFcn: Function2<List<Int>, UInt, Unit>) {
        this.monitors.forEach { monitors[0].windows.putAll(it.windows) }
        this.monitors.clear()
        this.monitors.addAll(monitors)

        // TODO not yet optimal: all windows are moved to screen 0s
        val windowMeasurements = monitors[0].getCurrentWindowMeasurements(this.screenMode)
        monitors[0].windows.keys.forEach { updateWindowFcn(windowMeasurements, it) }
    }

    fun updateScreenMode(screenMode: ScreenMode, updateWindowFcn: Function2<List<Int>, UInt, Unit>) {
        this.screenMode = screenMode

        this.monitors.forEach { monitor ->
            val measurements = monitor.getCurrentWindowMeasurements(screenMode)
            monitor.windows.keys.forEach { windowId ->
                updateWindowFcn(measurements, windowId)
            }
        }
    }
}
