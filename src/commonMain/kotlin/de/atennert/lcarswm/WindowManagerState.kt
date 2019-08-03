package de.atennert.lcarswm

/**
 * Container class for the state of the window manager.
 */
class WindowManagerState(
    val screenRoot: UInt,
    val lcarsWindowId: UInt,
    val graphicsContexts: List<UInt>,
    private val atomProvider: Function1<String, UInt>
) {
    val wmState = atomProvider("WM_STATE")

    val wmName = atomProvider("WM_NAME")

    var screenMode = ScreenMode.NORMAL
        private set

    /** map that holds the key sym to all registered key codes */
    val keyboardKeys = HashMap<UByte, Int>()

    /** List that holds all key codes for our used modifier key */
    val modifierKeys = ArrayList<UByte>(8)

    var activeWindow: Window? = null
        get() = field?.copy()
        private set

    var screenSize = Pair(0, 0)

    val monitors = ArrayList<Monitor>(3)

    /**
     * @return the monitor to which the window was added
     */
    fun addWindow(window: Window): Monitor {
        monitors[0].windows[window.id] = window
        activeWindow = window

        return monitors[0]
    }

    fun removeWindow(windowId: UInt) {
        for (monitor in monitors) {
            val windowToRemove = monitor.windows.remove(windowId)
            if (windowToRemove != null) {
                return
            }
        }
    }

    fun getWindowMonitor(windowId: UInt): Monitor? {
        return monitors.find { it.windows.containsKey(windowId) }
    }

    fun updateMonitors(monitors: List<Monitor>, updateWindowFcn: Function2<List<Int>, UInt, Unit>) {
        val windowsWithoutMonitor = HashMap<UInt, Window>()
        this.monitors.forEach { oldMonitor ->
            val newMonitor = monitors.find { newMonitor -> oldMonitor.id == newMonitor.id }
            if (newMonitor != null) {
                newMonitor.windows.putAll(oldMonitor.windows)
            } else {
                windowsWithoutMonitor.putAll(oldMonitor.windows)
            }
        }
        monitors[0].windows.putAll(windowsWithoutMonitor)
        this.monitors.clear()
        this.monitors.addAll(monitors)

        monitors.forEach { monitor ->
            val windowMeasurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            monitor.windows.keys.forEach { updateWindowFcn(windowMeasurements, it) }
        }
    }

    fun updateScreenMode(screenMode: ScreenMode, updateWindowFcn: Function2<List<Int>, UInt, Unit>) {
        this.screenMode = screenMode

        this.monitors.forEach { monitor ->
            val measurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            monitor.windows.keys.forEach { windowId ->
                updateWindowFcn(measurements, windowId)
            }
        }
    }

    fun toggleActiveWindow(): Window? {
        val currentActiveWindow = this.activeWindow

        val windowList = monitors
            .map { monitor -> monitor.windows.values }
            .fold(mutableListOf<Window>(), { combined, monitorList -> combined.addAll(monitorList); combined })
            .toList()

        if (windowList.isEmpty()) {
            this.activeWindow = null
            return null
        }

        val activeWindowIndex = windowList.indexOf(currentActiveWindow)
        val newActiveWindow = windowList.getOrNull((activeWindowIndex + 1) % windowList.size)

        this.activeWindow = newActiveWindow
        return newActiveWindow
    }

    fun moveWindowToNextMonitor(window: Window): Monitor {
        return moveWindowToNewMonitor(window, 1)
    }

    fun moveWindowToPreviousMonitor(window: Window): Monitor {
        return moveWindowToNewMonitor(window, -1)
    }

    private fun moveWindowToNewMonitor(window: Window, direction: Int): Monitor {
        val currentMonitor = monitors.single { monitor -> monitor.windows.containsKey(window.id) }
        val monitorIndex = monitors.indexOf(currentMonitor)
        val newMonitor = monitors[(monitorIndex + direction + monitors.size) % monitors.size]

        currentMonitor.windows.remove(window.id)
        newMonitor.windows[window.id] = window

        return newMonitor
    }

    fun getScreenModeForMonitor(monitor: Monitor): ScreenMode = when {
        this.screenMode != ScreenMode.NORMAL -> this.screenMode
        monitor.isPrimary -> ScreenMode.NORMAL
        else -> ScreenMode.MAXIMIZED
    }
}
