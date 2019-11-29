package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.Monitor
import xlib.Window

interface WindowCoordinator {
    fun rearrangeActiveWindows()

    fun addWindowToMonitor(windowId: Window): Monitor
}