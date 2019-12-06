package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import xlib.Window

/**
 *
 */
class ActiveWindowCoordinator(private val monitorManager: MonitorManager) : WindowCoordinator {
    private val windowsOnMonitors = mutableMapOf<FramedWindow, Monitor>()

    override fun rearrangeActiveWindows() {
        // TODO adjustWindowPositionAndSize
    }

    override fun addWindowToMonitor(window: FramedWindow): List<Int> {
        windowsOnMonitors[window] = monitorManager.getPrimaryMonitor()
        return getMonitorForWindow(window.id).getWindowMeasurements()
    }

    override fun removeWindow(window: FramedWindow) {
        windowsOnMonitors.remove(window)
    }

    override fun moveWindowToNextMonitor(windowId: Window) {
    }

    override fun moveWindowToPreviousMonitor(windowId: Window) {
    }

    override fun getMonitorForWindow(windowId: Window): Monitor {
        return windowsOnMonitors.entries.single { (window, _) -> window.id == windowId }.value
    }

    override fun getWindowMeasurements(windowId: Window): List<Int> {
        return getMonitorForWindow(windowId).getWindowMeasurements()
    }
}
