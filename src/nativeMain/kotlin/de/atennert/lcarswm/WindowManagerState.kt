package de.atennert.lcarswm

import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.monitor.Monitor
import xlib.Atom
import xlib.Window

/**
 * Container class for the state of the window manager.
 */
class WindowManagerState(
    atomProvider: Function1<Atoms, Atom>
) : WindowManagerStateHandler {
    val wmDeleteWindow = atomProvider(Atoms.WM_DELETE_WINDOW)

    val wmProtocols = atomProvider(Atoms.WM_PROTOCOLS)

    override val wmState = atomProvider(Atoms.WM_STATE)

    var screenMode = ScreenMode.NORMAL
        private set

    /** map that holds the key sym to all registered key codes */
    val keyboardKeys = HashMap<UInt, Int>()

    /** List that holds all key codes for our used modifier key */
    val modifierKeys = ArrayList<UByte>(8)

    val activeFramedWindow: FramedWindow?
        get() = this.windows.lastOrNull()?.first

    private var activeWindowListener: (FramedWindow?) -> Unit = {}

    var screenSize = Pair(0, 0)

    val monitors = ArrayList<Monitor>(3)

    override var windows = mutableListOf<Pair<FramedWindow, Monitor>>()
        private set

    override val initialMonitor: Monitor
        get() = this.monitors[0]

    override fun addWindow(framedWindow: FramedWindow, monitor: Monitor) {
        this.windows.add(Pair(framedWindow, monitor))
        this.activeWindowListener(this.activeFramedWindow)
    }

    override fun removeWindow(windowId: Window) {
        this.windows.removeAll { (window, _) -> window.id == windowId }
        this.activeWindowListener(this.activeFramedWindow)
    }

    override fun hasWindow(windowId: Window) = this.windows.find { (w, _) -> w.id == windowId } != null

    override fun getWindowContainer(windowId: Window): FramedWindow = this.windows.map {it.first}.single { it.id == windowId }

    fun getWindowMonitor(windowId: Window): Monitor? {
        return this.windows
            .find { (window, _) -> window.id == windowId }
            ?.second
    }

    fun updateMonitors(monitors: List<Monitor>, updateWindowFcn: Function2<List<Int>, FramedWindow, Unit>) {
        this.monitors.clear()
        this.monitors.addAll(monitors)

        this.windows.withIndex()
            .filterNot { (_, windowEntry) -> monitors.contains(windowEntry.second) }
            .forEach { (i, windowEntry) -> this.windows[i] = Pair(windowEntry.first, monitors[0]) }

        monitors.forEach { monitor ->
            val windowMeasurements = monitor.getWindowMeasurements()
            this.windows
                .filter { (_, windowMonitor) -> monitor == windowMonitor }
                .forEach { (window, _) -> updateWindowFcn(windowMeasurements, window) }
        }
    }

    fun updateScreenMode(screenMode: ScreenMode, updateWindowFcn: Function2<List<Int>, FramedWindow, Unit>) {
        this.screenMode = screenMode

        this.monitors.forEach { monitor ->
            val measurements = monitor.getWindowMeasurements()
            this.windows
                .filter { (_, windowMonitor) -> windowMonitor == monitor }
                .forEach { (window, _) ->
                    updateWindowFcn(measurements, window)
                }
        }
    }

    fun toggleActiveWindow(): FramedWindow? {
        if (windows.isNotEmpty()) {
            val currentActiveWindow = windows.removeAt(windows.lastIndex)
            windows.add(0, currentActiveWindow)
        }
        this.activeWindowListener(this.activeFramedWindow)
        return this.activeFramedWindow
    }

    fun moveWindowToPreviousMonitor(framedWindow: FramedWindow): Monitor {
        return moveWindowToNewMonitor(framedWindow, -1)
    }

    fun moveWindowToNextMonitor(framedWindow: FramedWindow): Monitor {
        return moveWindowToNewMonitor(framedWindow, 1)
    }

    private fun moveWindowToNewMonitor(framedWindow: FramedWindow, direction: Int): Monitor {
        val windowEntryIndex = this.windows.indexOfFirst { (listWindow, _) -> framedWindow == listWindow }
        val windowEntry = this.windows[windowEntryIndex]
        val currentMonitor = windowEntry.second
        val monitorIndex = monitors.indexOf(currentMonitor)
        val newMonitor = monitors[(monitorIndex + direction + monitors.size) % monitors.size]

        this.windows[windowEntryIndex] = Pair(framedWindow, newMonitor)

        return newMonitor
    }

    override fun getScreenModeForMonitor(monitor: Monitor): ScreenMode = when {
        this.screenMode != ScreenMode.NORMAL -> this.screenMode
        monitor.isPrimary -> ScreenMode.NORMAL
        else -> ScreenMode.MAXIMIZED
    }

    fun setActiveWindowListener(activeWindowListener: (FramedWindow?) -> Unit) {
        this.activeWindowListener = activeWindowListener
        activeWindowListener(this.activeFramedWindow)
    }
}
