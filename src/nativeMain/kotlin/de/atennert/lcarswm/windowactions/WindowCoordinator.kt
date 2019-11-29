package de.atennert.lcarswm.windowactions

import xlib.Window

interface WindowCoordinator {
    fun rearrangeActiveWindows()

    fun addWindowToMonitor(windowId: Window)
}