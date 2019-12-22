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
        val monitors = monitorManager.getMonitors()
        val primaryMonitor = monitorManager.getPrimaryMonitor()
        val updatedWindows = windowsOnMonitors
            .filterNot { (_, monitor) -> monitors.contains(monitor) }
            .map { (window, _) -> Pair(window, primaryMonitor) }
            .onEach { (window, monitor) ->
                adjustWindowPositionAndSize(eventApi, monitor.getWindowMeasurements(), window)
            }
        windowsOnMonitors.putAll(updatedWindows)
    }

    override fun addWindowToMonitor(window: FramedWindow): List<Int> {
        windowsOnMonitors[window] = monitorManager.getPrimaryMonitor()
        return getMonitorForWindow(window.id).getWindowMeasurements()
    }

    override fun removeWindow(window: FramedWindow) {
        windowsOnMonitors.remove(window)
    }

    override fun moveWindowToNextMonitor(windowId: Window) {
        val window = windowsOnMonitors.keys.single { it.id == windowId }
        val currentMonitor = windowsOnMonitors[window]
        val monitors = monitorManager.getMonitors()
        val currentMonitorIndex = monitors.indexOf(currentMonitor)
        val nextMonitorIndex = (currentMonitorIndex + 1).rem(monitors.size)
        val nextMonitor = monitors[nextMonitorIndex]
        windowsOnMonitors[window] = nextMonitor
        adjustWindowPositionAndSize(eventApi, nextMonitor.getWindowMeasurements(), window)
    }

    override fun moveWindowToPreviousMonitor(windowId: Window) {
        val window = windowsOnMonitors.keys.single { it.id == windowId }
        val currentMonitor = windowsOnMonitors[window]
        val monitors = monitorManager.getMonitors()
        val currentMonitorIndex = monitors.indexOf(currentMonitor)
        val nextMonitorIndex = if (currentMonitorIndex == 0) monitors.size - 1 else currentMonitorIndex - 1
        val nextMonitor = monitors[nextMonitorIndex]
        windowsOnMonitors[window] = nextMonitor
        adjustWindowPositionAndSize(eventApi, nextMonitor.getWindowMeasurements(), window)
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
