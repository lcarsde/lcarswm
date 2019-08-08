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

    val activeWindow: Window?
        get() = this.windows.lastOrNull()?.first

    var screenSize = Pair(0, 0)

    val monitors = ArrayList<Monitor>(3)

    private var windows = mutableListOf<Pair<Window, Monitor>>()

    /**
     * @return the monitor to which the window was added
     */
    fun addWindow(window: Window): Monitor {
        this.windows.add(Pair(window, monitors[0]))

        return monitors[0]
    }

    fun removeWindow(windowId: UInt) {
        this.windows.removeAll { (window, _) -> window.id == windowId }
    }

    fun getWindowMonitor(windowId: UInt): Monitor? {
        return this.windows
            .find { (window, _) -> window.id == windowId }
            ?.second
    }

    fun updateMonitors(monitors: List<Monitor>, updateWindowFcn: Function2<List<Int>, UInt, Unit>) {
        this.monitors.clear()
        this.monitors.addAll(monitors)

        this.windows.withIndex()
            .filterNot { (_, windowEntry) -> monitors.contains(windowEntry.second) }
            .forEach { (i, windowEntry) -> this.windows[i] = Pair(windowEntry.first, monitors[0]) }

        val reversedWindows = windows.reversed()

        monitors.forEach { monitor ->
            val windowMeasurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            reversedWindows
                .filter { (_, windowMonitor) -> monitor == windowMonitor }
                .forEach { (window, _) -> updateWindowFcn(windowMeasurements, window.id) }
        }
    }

    fun updateScreenMode(screenMode: ScreenMode, updateWindowFcn: Function2<List<Int>, UInt, Unit>) {
        this.screenMode = screenMode
        val reversedWindows = windows.reversed()

        this.monitors.forEach { monitor ->
            val measurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            reversedWindows.forEach { (window, _) ->
                updateWindowFcn(measurements, window.id)
            }
        }
    }

    fun toggleActiveWindow(): Window? {
        if (windows.isNotEmpty()) {
            val currentActiveWindow = windows.removeAt(windows.lastIndex)
            windows.add(0, currentActiveWindow)
        }
        return this.activeWindow
    }

    fun moveWindowToNextMonitor(window: Window): Monitor {
        return moveWindowToNewMonitor(window, 1)
    }

    fun moveWindowToPreviousMonitor(window: Window): Monitor {
        return moveWindowToNewMonitor(window, -1)
    }

    private fun moveWindowToNewMonitor(window: Window, direction: Int): Monitor {
        val windowEntryIndex = this.windows.indexOfFirst { (listWindow, _) -> window == listWindow }
        val windowEntry = this.windows[windowEntryIndex]
        val currentMonitor = windowEntry.second
        val monitorIndex = monitors.indexOf(currentMonitor)
        val newMonitor = monitors[(monitorIndex + direction + monitors.size) % monitors.size]

        this.windows[windowEntryIndex] = Pair(window, newMonitor)

        return newMonitor
    }

    fun getScreenModeForMonitor(monitor: Monitor): ScreenMode = when {
        this.screenMode != ScreenMode.NORMAL -> this.screenMode
        monitor.isPrimary -> ScreenMode.NORMAL
        else -> ScreenMode.MAXIMIZED
    }
}
