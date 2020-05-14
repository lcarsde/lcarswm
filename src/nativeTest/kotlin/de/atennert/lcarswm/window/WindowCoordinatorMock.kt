package de.atennert.lcarswm.window

import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.system.FunctionCall
import kotlinx.cinterop.convert
import xlib.Window

class WindowCoordinatorMock : WindowCoordinator {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun rearrangeActiveWindows() {
        functionCalls.add(FunctionCall("rearrangeActiveWindows"))
    }

    val primaryMonitor = Monitor(MonitorManagerMock(), 21.convert(), "", true)
    override fun addWindowToMonitor(window: FramedWindow): WindowMeasurements {
        functionCalls.add(FunctionCall("addWindowToMonitor", window))
        return primaryMonitor.getWindowMeasurements()
    }

    override fun removeWindow(window: FramedWindow) {
        functionCalls.add(FunctionCall("removeWindow", window))
    }

    override fun moveWindowToNextMonitor(windowId: Window) {
        functionCalls.add(FunctionCall("moveWindowToNextMonitor", windowId))
    }

    override fun moveWindowToPreviousMonitor(windowId: Window) {
        functionCalls.add(FunctionCall("moveWindowToPreviousMonitor", windowId))
    }

    override fun getMonitorForWindow(windowId: Window): Monitor {
        return Monitor(MonitorManagerMock(), 1.convert(), "some monitor", false)
    }

    override fun getWindowMeasurements(windowId: Window): WindowMeasurements {
        return primaryMonitor.getWindowMeasurements()
    }

    override fun stackWindowToTheTop(windowId: Window) {
        functionCalls.add(FunctionCall("stackWindowToTheTop", windowId))
    }

    override fun realignWindows() {
        functionCalls.add(FunctionCall("realignWindows"))
    }
}