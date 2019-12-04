package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import xlib.Window

/**
 *
 */
class ActiveWindowCoordinator(private val monitorManager: MonitorManager) : WindowCoordinator {
    private val windowsOnMonitors = mutableMapOf<Window, Monitor>()

    override fun rearrangeActiveWindows() {
    }

    override fun addWindowToMonitor(windowId: Window): List<Int> {
        windowsOnMonitors[windowId] = monitorManager.getPrimaryMonitor()
        return getMonitorForWindow(windowId).getCurrentWindowMeasurements(monitorManager.getScreenMode())
    }

    override fun removeWindow(windowId: Window) {
        windowsOnMonitors.remove(windowId)
    }

    override fun moveWindowToNextMonitor(windowId: Window) {
    }

    override fun moveWindowToPreviousMonitor(windowId: Window) {
    }

    override fun getMonitorForWindow(windowId: Window): Monitor {
        return windowsOnMonitors.getValue(windowId)
    }

    override fun getWindowMeasurements(windowId: Window): List<Int> {
        return emptyList() // TODO
    }
}
