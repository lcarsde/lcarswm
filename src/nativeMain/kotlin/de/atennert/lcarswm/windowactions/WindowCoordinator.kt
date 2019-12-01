package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.Monitor
import xlib.Window

interface WindowCoordinator {
    fun rearrangeActiveWindows()

    fun addWindowToMonitor(windowId: Window): Monitor

    fun removeWindow(windowId: Window)

    fun moveWindowToNextMonitor(windowId: Window)

    fun moveWindowToPreviousMonitor(windowId: Window)
}