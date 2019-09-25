package de.atennert.lcarswm

import xlib.Window

/**
 * Container class for the state of the window manager.
 */
class WindowManagerState(
    atomProvider: Function1<String, ULong>
) : WindowManagerStateHandler {
    val wmDeleteWindow = atomProvider("WM_DELETE_WINDOW")

    val wmProtocols = atomProvider("WM_PROTOCOLS")

    var screenMode = ScreenMode.NORMAL
        private set

    /** map that holds the key sym to all registered key codes */
    val keyboardKeys = HashMap<UInt, Int>()

    /** List that holds all key codes for our used modifier key */
    val modifierKeys = ArrayList<UByte>(8)

    val activeWindow: WindowContainer?
        get() = this.windows.lastOrNull()?.first

    private var activeWindowListener: (WindowContainer?) -> Unit = {}

    var screenSize = Pair(0, 0)

    val monitors = ArrayList<Monitor>(3)

    var windows = mutableListOf<Pair<WindowContainer, Monitor>>()
        private set

    override val initialMonitor: Monitor
        get() = this.monitors[0]

    override fun addWindow(window: WindowContainer, monitor: Monitor) {
        this.windows.add(Pair(window, monitor))
        this.activeWindowListener(this.activeWindow)
    }

    override fun removeWindow(windowId: Window) {
        this.windows.removeAll { (window, _) -> window.id == windowId }
        this.activeWindowListener(this.activeWindow)
    }

    fun getWindowMonitor(windowId: Window): Monitor? {
        return this.windows
            .find { (window, _) -> window.id == windowId }
            ?.second
    }

    fun updateMonitors(monitors: List<Monitor>, updateWindowFcn: Function2<List<Int>, WindowContainer, Unit>) {
        this.monitors.clear()
        this.monitors.addAll(monitors)

        this.windows.withIndex()
            .filterNot { (_, windowEntry) -> monitors.contains(windowEntry.second) }
            .forEach { (i, windowEntry) -> this.windows[i] = Pair(windowEntry.first, monitors[0]) }

        monitors.forEach { monitor ->
            val windowMeasurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            this.windows
                .filter { (_, windowMonitor) -> monitor == windowMonitor }
                .forEach { (window, _) -> updateWindowFcn(windowMeasurements, window) }
        }
    }

    fun updateScreenMode(screenMode: ScreenMode, updateWindowFcn: Function2<List<Int>, WindowContainer, Unit>) {
        this.screenMode = screenMode

        this.monitors.forEach { monitor ->
            val measurements = monitor.getCurrentWindowMeasurements(getScreenModeForMonitor(monitor))
            this.windows
                .filter { (_, windowMonitor) -> windowMonitor == monitor }
                .forEach { (window, _) ->
                    updateWindowFcn(measurements, window)
                }
        }
    }

    fun toggleActiveWindow(): WindowContainer? {
        if (windows.isNotEmpty()) {
            val currentActiveWindow = windows.removeAt(windows.lastIndex)
            windows.add(0, currentActiveWindow)
        }
        this.activeWindowListener(this.activeWindow)
        return this.activeWindow
    }

    fun moveWindowToPreviousMonitor(window: WindowContainer): Monitor {
        return moveWindowToNewMonitor(window, -1)
    }

    fun moveWindowToNextMonitor(window: WindowContainer): Monitor {
        return moveWindowToNewMonitor(window, 1)
    }

    private fun moveWindowToNewMonitor(window: WindowContainer, direction: Int): Monitor {
        val windowEntryIndex = this.windows.indexOfFirst { (listWindow, _) -> window == listWindow }
        val windowEntry = this.windows[windowEntryIndex]
        val currentMonitor = windowEntry.second
        val monitorIndex = monitors.indexOf(currentMonitor)
        val newMonitor = monitors[(monitorIndex + direction + monitors.size) % monitors.size]

        this.windows[windowEntryIndex] = Pair(window, newMonitor)

        return newMonitor
    }

    override fun getScreenModeForMonitor(monitor: Monitor): ScreenMode = when {
        this.screenMode != ScreenMode.NORMAL -> this.screenMode
        monitor.isPrimary -> ScreenMode.NORMAL
        else -> ScreenMode.MAXIMIZED
    }

    fun hasWindow(windowId: Window) = this.windows.find { (w, _) -> w.id == windowId } != null

    fun setActiveWindowListener(activeWindowListener: (WindowContainer?) -> Unit) {
        this.activeWindowListener = activeWindowListener
        activeWindowListener(this.activeWindow)
    }
}
