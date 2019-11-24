package de.atennert.lcarswm

import de.atennert.lcarswm.monitor.Monitor
import xlib.Window

interface WindowManagerStateHandler {
    val wmState: ULong

    val initialMonitor: Monitor

    val windows: List<Pair<FramedWindow, Monitor>>

    fun addWindow(framedWindow: FramedWindow, monitor: Monitor)

    fun removeWindow(windowId: Window)

    fun hasWindow(windowId: Window): Boolean

    fun getWindowContainer(windowId: Window): FramedWindow

    fun getScreenModeForMonitor(monitor: Monitor): ScreenMode
}