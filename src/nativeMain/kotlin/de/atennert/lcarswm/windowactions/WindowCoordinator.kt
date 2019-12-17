package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.monitor.Monitor
import xlib.Window

interface WindowCoordinator {
    fun rearrangeActiveWindows()

    /**
     * @return measurements
     */
    fun addWindowToMonitor(window: FramedWindow): List<Int>

    fun removeWindow(window: FramedWindow)

    fun moveWindowToNextMonitor(windowId: Window)

    fun moveWindowToPreviousMonitor(windowId: Window)

    fun getMonitorForWindow(windowId: Window): Monitor

    fun getWindowMeasurements(windowId: Window): List<Int>

    fun stackWindowToTheTop(windowId: Window)
}