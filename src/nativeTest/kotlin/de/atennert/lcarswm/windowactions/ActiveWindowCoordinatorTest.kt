package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ActiveWindowCoordinatorTest {
    @Test
    fun `add windows to a monitor`() {
        val systemApi = SystemFacadeMock()
        val windowId = systemApi.getNewWindowId()
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(monitorManager)

        val measurements = activeWindowCoordinator.addWindowToMonitor(windowId)

        assertEquals(
            monitorManager.primaryMonitor.getWindowMeasurements(), measurements,
            "New windows should initially be added to the primary monitor"
        )

        assertEquals(
            monitorManager.primaryMonitor, activeWindowCoordinator.getMonitorForWindow(windowId),
            "The monitor manager should have stored the window-monitor-relation"
        )
    }

    @Test
    fun `remove window from coordinator`() {
        val systemApi = SystemFacadeMock()
        val windowId = systemApi.getNewWindowId()
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(monitorManager)

        activeWindowCoordinator.addWindowToMonitor(windowId)

        activeWindowCoordinator.removeWindow(windowId)

        assertFails("The window should be removed") { activeWindowCoordinator.getMonitorForWindow(windowId) }
    }

    @Test
    fun `get measurements for window`() {
        val systemApi = SystemFacadeMock()
        val windowId = systemApi.getNewWindowId()
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(monitorManager)
        activeWindowCoordinator.addWindowToMonitor(windowId)

        val measurements = activeWindowCoordinator.getWindowMeasurements(windowId)

        assertEquals(monitorManager.primaryMonitor.getWindowMeasurements(), measurements,
            "The window coordinator should return the correct window measurements")
    }
}