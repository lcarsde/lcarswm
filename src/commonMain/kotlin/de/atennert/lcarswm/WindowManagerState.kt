package de.atennert.lcarswm

/**
 * Container class for the state of the window manager.
 */
class WindowManagerState(
    private val atomProvider: Function1<String, ULong>
) {
    val wmState = atomProvider("WM_STATE")

    val wmName = atomProvider("WM_NAME")

    var screenMode = ScreenMode.NORMAL
        private set

    /** map that holds the key sym to all registered key codes */
    val keyboardKeys = HashMap<UInt, Int>()

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

    fun removeWindow(windowId: ULong) {
        this.windows.removeAll { (window, _) -> window.id == windowId }
    }

    fun getWindowMonitor(windowId: ULong): Monitor? {
        return this.windows
            .find { (window, _) -> window.id == windowId }
            ?.second
    }

    fun updateMonitors(monitors: List<Monitor>, updateWindowFcn: Function2<List<Int>, ULong, Unit>) {
        this.monitors.clear()
        this.monitors.addAll(monitors)

        this.windows.withIndex()
            .filterNot { (_, windowEntry) -> monitors.contains(windowEntry.second) }
            .forEach { (i, windowEntry) -> this.windows[i] = Pair(windowEntry.first, monitors[0]) }

        monitors.forEach { monitor ->
            val windowMeasurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            this.windows
                .filter { (_, windowMonitor) -> monitor == windowMonitor }
                .forEach { (window, _) -> updateWindowFcn(windowMeasurements, window.id) }
        }
    }

    fun updateScreenMode(screenMode: ScreenMode, updateWindowFcn: Function2<List<Int>, ULong, Unit>) {
        this.screenMode = screenMode

        this.monitors.forEach { monitor ->
            val measurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            this.windows.forEach { (window, _) ->
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

    fun hasWindow(window: ULong) = this.windows.find { (w, _) -> w.id == window } != null
}
