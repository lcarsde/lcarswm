package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.system.FunctionCall
import xlib.Window

class WindowCoordinatorMock : WindowCoordinator {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun rearrangeActiveWindows() {
        functionCalls.add(FunctionCall("rearrangeActiveWindows"))
    }

    override fun addWindowToMonitor(windowId: Window) {
        functionCalls.add(FunctionCall("addWindowToMonitor", windowId))
    }
}