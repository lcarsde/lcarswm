package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.drawing.FrameDrawerMock
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.monitor.WindowMeasurements
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.pointed
import xlib.Above
import xlib.CWStackMode
import xlib.XWindowChanges
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ActiveWindowCoordinatorTest {
    @Test
    fun `add windows to a monitor`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())

        val measurements = activeWindowCoordinator.addWindowToMonitor(window)

        assertEquals(
            monitorManager.primaryMonitor.getWindowMeasurements(), measurements,
            "New windows should initially be added to the primary monitor"
        )

        assertEquals(
            monitorManager.primaryMonitor, activeWindowCoordinator.getMonitorForWindow(window.id),
            "The monitor manager should have stored the window-monitor-relation"
        )
    }

    @Test
    fun `remove window from coordinator`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())

        activeWindowCoordinator.addWindowToMonitor(window)

        activeWindowCoordinator.removeWindow(window)

        assertFails("The window should be removed") { activeWindowCoordinator.getMonitorForWindow(window.id) }
    }

    @Test
    fun `get measurements for window`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())
        activeWindowCoordinator.addWindowToMonitor(window)

        val measurements = activeWindowCoordinator.getWindowMeasurements(window.id)

        assertEquals(monitorManager.primaryMonitor.getWindowMeasurements(), measurements,
            "The window coordinator should return the correct window measurements")
    }

    @Test
    fun `rearrange registered window`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        window.frame = systemApi.getNewWindowId()
        lateinit var monitor: Monitor
        val monitorManager = object : MonitorManagerMock() {
            override fun getPrimaryMonitor(): Monitor = monitor
        }
        monitor = Monitor(monitorManager, 21.convert(), "", true)

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())
        activeWindowCoordinator.addWindowToMonitor(window)

        monitor = Monitor(monitorManager, 42.convert(), "", true)
        activeWindowCoordinator.rearrangeActiveWindows()

        val systemCalls = systemApi.functionCalls

        checkMoveWindow(systemCalls, window)

        assertEquals(monitor, activeWindowCoordinator.getMonitorForWindow(window.id), "The window should be moved to the new monitor")
    }

    @Test
    private fun `restack a window to the top`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        window.frame = systemApi.getNewWindowId()
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())
        activeWindowCoordinator.addWindowToMonitor(window)

        activeWindowCoordinator.stackWindowToTheTop(window.id)

        val configureCall = systemApi.functionCalls.removeAt(0)
        assertEquals("configureWindow", configureCall.name, "The window $window needs to be configured")
        assertEquals(window.frame, configureCall.parameters[0], "The _window ${window}_ needs to be configured")
        assertEquals(CWStackMode.convert<UInt>(), configureCall.parameters[1], "The $window window needs to be restacked")
        assertEquals(Above, (configureCall.parameters[2] as CPointer<XWindowChanges>).pointed.stack_mode, "The stack mode should be 'above'")
    }

    @Test
    fun `move window to next monitor`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        window.frame = systemApi.getNewWindowId()
        lateinit var secondaryMonitor: Monitor
        lateinit var tertiaryMonitor: Monitor
        val monitorManager = object : MonitorManagerMock() {
            override fun getMonitors(): List<Monitor> {
                return listOf(primaryMonitor, secondaryMonitor, tertiaryMonitor)
            }
        }
        secondaryMonitor = Monitor(monitorManager, 3.convert(), "", false)
        tertiaryMonitor = Monitor(monitorManager, 4.convert(), "", false)

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())
        activeWindowCoordinator.addWindowToMonitor(window)

        val systemCalls = systemApi.functionCalls

        activeWindowCoordinator.moveWindowToNextMonitor(window.id)
        assertEquals(secondaryMonitor, activeWindowCoordinator.getMonitorForWindow(window.id), "The window should be moved to the first next monitor")
        checkMoveWindow(systemCalls, window)

        activeWindowCoordinator.moveWindowToNextMonitor(window.id)
        assertEquals(tertiaryMonitor, activeWindowCoordinator.getMonitorForWindow(window.id), "The window should be moved to the second next monitor")
        checkMoveWindow(systemCalls, window)

        activeWindowCoordinator.moveWindowToNextMonitor(window.id)
        assertEquals(monitorManager.primaryMonitor, activeWindowCoordinator.getMonitorForWindow(window.id), "The window should be moved to the primary monitor")
        checkMoveWindow(systemCalls, window)
    }

    @Test
    fun `move window to previous monitor`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        window.frame = systemApi.getNewWindowId()
        lateinit var secondaryMonitor: Monitor
        lateinit var tertiaryMonitor: Monitor
        val monitorManager = object : MonitorManagerMock() {
            override fun getMonitors(): List<Monitor> {
                return listOf(primaryMonitor, secondaryMonitor, tertiaryMonitor)
            }
        }
        secondaryMonitor = Monitor(monitorManager, 3.convert(), "", false)
        tertiaryMonitor = Monitor(monitorManager, 4.convert(), "", false)

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())
        activeWindowCoordinator.addWindowToMonitor(window)

        val systemCalls = systemApi.functionCalls

        activeWindowCoordinator.moveWindowToPreviousMonitor(window.id)
        assertEquals(tertiaryMonitor, activeWindowCoordinator.getMonitorForWindow(window.id), "The window should be moved to the first previous monitor")
        checkMoveWindow(systemCalls, window)

        activeWindowCoordinator.moveWindowToPreviousMonitor(window.id)
        assertEquals(secondaryMonitor, activeWindowCoordinator.getMonitorForWindow(window.id), "The window should be moved to the second previous monitor")
        checkMoveWindow(systemCalls, window)

        activeWindowCoordinator.moveWindowToPreviousMonitor(window.id)
        assertEquals(monitorManager.primaryMonitor, activeWindowCoordinator.getMonitorForWindow(window.id), "The window should be moved to the primary monitor")
        checkMoveWindow(systemCalls, window)
    }

    @Test
    fun `test realign windows`() {
        val systemApi = SystemFacadeMock()
        val window = FramedWindow(systemApi.getNewWindowId(), "", 0)
        window.frame = systemApi.getNewWindowId()
        lateinit var monitor: Monitor
        val monitorManager = object : MonitorManagerMock() {
            override fun getPrimaryMonitor(): Monitor = monitor
        }
        monitor = Monitor(monitorManager, 21.convert(), "", true)

        val activeWindowCoordinator = ActiveWindowCoordinator(systemApi, monitorManager, FrameDrawerMock())
        activeWindowCoordinator.addWindowToMonitor(window)

        monitorManager.screenMode = ScreenMode.MAXIMIZED
        activeWindowCoordinator.realignWindows()
        var measurments = activeWindowCoordinator.getWindowMeasurements(window.id)
        assertEquals(WindowMeasurements(40, 48, 720, 504, 552), measurments, "")
        checkMoveWindow(systemApi.functionCalls, window)

        monitorManager.screenMode = ScreenMode.FULLSCREEN
        activeWindowCoordinator.realignWindows()
        measurments = activeWindowCoordinator.getWindowMeasurements(window.id)
        assertEquals(WindowMeasurements(0, 0, 800, 600, 600), measurments, "")
        checkMoveWindow(systemApi.functionCalls, window)

        monitorManager.screenMode = ScreenMode.NORMAL
        activeWindowCoordinator.realignWindows()
        measurments = activeWindowCoordinator.getWindowMeasurements(window.id)
        assertEquals(WindowMeasurements(208, 242, 552, 292, 358), measurments, "")
        checkMoveWindow(systemApi.functionCalls, window)
    }
}
