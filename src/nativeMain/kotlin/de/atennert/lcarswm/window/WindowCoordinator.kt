package de.atennert.lcarswm.window

import de.atennert.lcarswm.monitor.Monitor
import xlib.RROutput
import xlib.Window

/**
 * Interface for class that manages the mapping of windows to monitors
 */
interface WindowCoordinator {
    fun moveWindowToNextMonitor(windowId: Window)

    fun moveWindowToPreviousMonitor(windowId: Window)

    fun moveWindowToMonitor(windowId: Window, monitor: Monitor<RROutput>)
}
