package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.adjustWindowPositionAndSize
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.Above
import xlib.CWStackMode
import xlib.Window
import xlib.XWindowChanges

/**
 *
 */
class ActiveWindowCoordinator(private val eventApi: EventApi, private val monitorManager: MonitorManager) :
    WindowCoordinator {
    private val windowsOnMonitors = mutableMapOf<FramedWindow, Monitor>()

    override fun rearrangeActiveWindows() {
        windowsOnMonitors.forEach { (window, monitor) ->
            adjustWindowPositionAndSize(eventApi, monitor.getWindowMeasurements(), window)
        }
    }

    override fun addWindowToMonitor(window: FramedWindow): List<Int> {
        windowsOnMonitors[window] = monitorManager.getPrimaryMonitor()
        return getMonitorForWindow(window.id).getWindowMeasurements()
    }

    override fun removeWindow(window: FramedWindow) {
        windowsOnMonitors.remove(window)
    }

    override fun moveWindowToNextMonitor(windowId: Window) {
    }

    override fun moveWindowToPreviousMonitor(windowId: Window) {
    }

    override fun getMonitorForWindow(windowId: Window): Monitor {
        return windowsOnMonitors.entries.single { (window, _) -> window.id == windowId }.value
    }

    override fun getWindowMeasurements(windowId: Window): List<Int> {
        return getMonitorForWindow(windowId).getWindowMeasurements()
    }

    override fun stackWindowToTheTop(windowId: Window) {
        val window = windowsOnMonitors.keys.single { it.id == windowId }

        val windowChanges = nativeHeap.alloc<XWindowChanges>()
        windowChanges.stack_mode = Above

        eventApi.configureWindow(window.frame, CWStackMode.convert(), windowChanges.ptr)
    }
}
