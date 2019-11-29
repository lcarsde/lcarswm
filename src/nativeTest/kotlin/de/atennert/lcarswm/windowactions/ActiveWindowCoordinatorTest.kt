package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveWindowCoordinatorTest {
    @Test
    fun `add windows to a monitor`() {
        val systemApi = SystemFacadeMock()
        val testWindowId = systemApi.getNewWindowId()
        val monitorManager = MonitorManagerMock()

        val activeWindowCoordinator = ActiveWindowCoordinator(monitorManager)

        val initialMonitor = activeWindowCoordinator.addWindowToMonitor(testWindowId)

        assertEquals(monitorManager.primaryMonitor, initialMonitor, "New windows should initially be added to the primary monitor")
    }
}