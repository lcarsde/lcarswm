package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.monitor.Monitor
import xlib.Window

/**
 * Interface for class that manages the mapping of windows to monitors
 */
interface WindowCoordinator {
    fun rearrangeActiveWindows()

    /**
     * Register the window with the monitor
     * @return measurements
     */
    fun addWindowToMonitor(window: FramedWindow): List<Int>
    
    /**
     * Unregister the window with the monitor
     */
    fun removeWindow(window: FramedWindow)

    fun moveWindowToNextMonitor(windowId: Window)

    fun moveWindowToPreviousMonitor(windowId: Window)

    fun getMonitorForWindow(windowId: Window): Monitor

    fun getWindowMeasurements(windowId: Window): List<Int>

    fun stackWindowToTheTop(windowId: Window)

    fun realignWindows()
}
