package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import xlib.Window

/**
 *
 */
class ActiveWindowCoordinator(private val monitorManager: MonitorManager) : WindowCoordinator {
    override fun rearrangeActiveWindows() {

    }

    override fun addWindowToMonitor(windowId: Window): Monitor {
        return monitorManager.getPrimaryMonitor()
    }
}
