package de.atennert.lcarswm

import kotlinx.cinterop.convert
import xlib.Window

open class WindowManagerStateMock: WindowManagerStateHandler {
    override val wmState: ULong = 0.convert()

    override val initialMonitor: Monitor = Monitor(0.convert(), "Monitor", true)

    override fun addWindow(window: WindowContainer, monitor: Monitor) {}

    override fun removeWindow(windowId: Window) {}

    override fun hasWindow(windowId: Window): Boolean = false

    override fun getScreenModeForMonitor(monitor: Monitor): ScreenMode {
        return ScreenMode.NORMAL
    }
}