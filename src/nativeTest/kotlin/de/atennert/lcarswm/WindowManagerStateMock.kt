package de.atennert.lcarswm

import kotlinx.cinterop.convert
import xlib.Atom
import xlib.Window

open class WindowManagerStateMock: WindowManagerStateHandler {
    override val wmState: Atom = 0.convert()

    override val initialMonitor: Monitor = Monitor(0.convert(), "Monitor", true)

    override val windows = emptyList<Pair<WindowContainer, Monitor>>()

    override fun addWindow(window: WindowContainer, monitor: Monitor) {}

    override fun removeWindow(windowId: Window) {}

    override fun hasWindow(windowId: Window): Boolean = false

    override fun getScreenModeForMonitor(monitor: Monitor): ScreenMode = ScreenMode.NORMAL

    override fun getWindowContainer(windowId: Window): WindowContainer = windows.map {it.first}.single {it.id == windowId}
}