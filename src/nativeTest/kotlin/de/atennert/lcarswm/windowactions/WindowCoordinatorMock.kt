package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.system.FunctionCall
import kotlinx.cinterop.convert
import xlib.Window

class WindowCoordinatorMock : WindowCoordinator {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun rearrangeActiveWindows() {
        functionCalls.add(FunctionCall("rearrangeActiveWindows"))
    }

    val primaryMonitor = Monitor(1.convert(), "", true)
    override fun addWindowToMonitor(windowId: Window): Monitor {
        functionCalls.add(FunctionCall("addWindowToMonitor", windowId))
        return primaryMonitor
    }
}