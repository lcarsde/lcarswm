package de.atennert.lcarswm

import de.atennert.lcarswm.system.FunctionCall
import kotlinx.cinterop.convert
import xlib.Atom
import xlib.Window

open class WindowManagerStateMock : WindowManagerStateHandler {
    val functionCalls = mutableListOf<FunctionCall>()

    override val wmState: Atom = 0.convert()

    override val initialMonitor: Monitor = Monitor(0.convert(), "Monitor", true)

    override val windows = mutableListOf<Pair<FramedWindow, Monitor>>()

    override fun addWindow(framedWindow: FramedWindow, monitor: Monitor) {
        functionCalls.add(FunctionCall("addWindow", framedWindow, monitor))
        windows.add(Pair(framedWindow, monitor))
    }

    override fun removeWindow(windowId: Window) {
        functionCalls.add(FunctionCall("removeWindow", windowId))
        windows.removeAll {(windowContainer, _) -> windowContainer.id == windowId}
    }

    override fun hasWindow(windowId: Window): Boolean {
        return windows.find { (windowContainer, _) -> windowContainer.id == windowId } != null
    }

    override fun getScreenModeForMonitor(monitor: Monitor): ScreenMode = ScreenMode.NORMAL

    override fun getWindowContainer(windowId: Window): FramedWindow = windows.map {it.first}.single {it.id == windowId}
}