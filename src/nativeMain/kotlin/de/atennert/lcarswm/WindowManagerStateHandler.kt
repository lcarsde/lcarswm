package de.atennert.lcarswm

import xlib.Window

interface WindowManagerStateHandler {
    val wmState: ULong

    val initialMonitor: Monitor

    val windows: List<Pair<WindowContainer, Monitor>>

    fun addWindow(window: WindowContainer, monitor: Monitor)

    fun removeWindow(windowId: Window)

    fun hasWindow(windowId: Window): Boolean

    fun getWindowContainer(windowId: Window): WindowContainer

    fun getScreenModeForMonitor(monitor: Monitor): ScreenMode
}