package de.atennert.lcarswm.window

import de.atennert.lcarswm.drawing.IFrameDrawer
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.*

/**
 *
 */
class ActiveWindowCoordinator(
    private val eventApi: EventApi,
    private val monitorManager: MonitorManager,
    private val frameDrawer: IFrameDrawer
) :
    WindowCoordinator {
    private val windowsOnMonitors = mutableMapOf<FramedWindow, Monitor>()

    override fun rearrangeActiveWindows() {
        val monitors = monitorManager.getMonitors()
        val primaryMonitor = monitorManager.getPrimaryMonitor()
        val updatedWindows = windowsOnMonitors
            .map { (window, monitor) ->
                Pair(window, monitors.getOrElse(monitors.indexOf(monitor)) { primaryMonitor })
            }
            .onEach { (window, monitor) ->
                adjustWindowPositionAndSize(
                    eventApi,
                    monitor.getWindowMeasurements(),
                    window
                )
                frameDrawer.drawFrame(window, monitor)
            }

        windowsOnMonitors.putAll(updatedWindows)
    }

    override fun addWindowToMonitor(window: FramedWindow): WindowMeasurements {
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
        adjustWindowPositionAndSize(
            eventApi,
            nextMonitor.getWindowMeasurements(),
            window
        )
        frameDrawer.drawFrame(window, nextMonitor)
    }

    override fun moveWindowToPreviousMonitor(windowId: Window) {
        val window = windowsOnMonitors.keys.single { it.id == windowId }
        val currentMonitor = windowsOnMonitors[window]
        val monitors = monitorManager.getMonitors()
        val currentMonitorIndex = monitors.indexOf(currentMonitor)
        val nextMonitorIndex = if (currentMonitorIndex == 0) monitors.size - 1 else currentMonitorIndex - 1
        val nextMonitor = monitors[nextMonitorIndex]
        windowsOnMonitors[window] = nextMonitor
        adjustWindowPositionAndSize(
            eventApi,
            nextMonitor.getWindowMeasurements(),
            window
        )
        frameDrawer.drawFrame(window, nextMonitor)
    }

    override fun moveWindowToMonitor(windowId: Window, monitor: Monitor) {
        val window = windowsOnMonitors.keys.single { it.id == windowId }
        windowsOnMonitors[window] = monitor
        adjustWindowPositionAndSize(
            eventApi,
            monitor.getWindowMeasurements(),
            window
        )
        frameDrawer.drawFrame(window, monitor)
    }

    override fun getMonitorForWindow(windowId: Window): Monitor {
        return windowsOnMonitors.entries.single { (window, _) -> window.id == windowId }.value
    }

    override fun getWindowMeasurements(windowId: Window): WindowMeasurements {
        return getMonitorForWindow(windowId).getWindowMeasurements()
    }

    override fun stackWindowToTheTop(windowId: Window) {
        val window = windowsOnMonitors.keys.single { it.id == windowId }

        val windowChanges = nativeHeap.alloc<XWindowChanges>()
        windowChanges.stack_mode = Above

        eventApi.configureWindow(window.frame, CWStackMode.convert(), windowChanges.ptr)
    }

    override fun realignWindows() {
        windowsOnMonitors.forEach { (window, monitor) ->
            adjustWindowPositionAndSize(
                eventApi, monitor.getWindowMeasurements(), window
            )
            frameDrawer.drawFrame(window, monitor)
        }
    }
}
