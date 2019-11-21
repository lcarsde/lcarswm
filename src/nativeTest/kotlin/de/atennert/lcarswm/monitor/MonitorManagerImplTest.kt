package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlin.test.Test

class MonitorManagerImplTest {
    @Test
    fun `update monitor list`() {
        val systemApi = SystemFacadeMock()
        val monitorManager: MonitorManager = MonitorManagerImpl(systemApi)

        monitorManager.updateMonitorList()

        val monitorList = monitorManager.getMonitors()
        val primaryMonitor = monitorManager.getPrimaryMonitor()
    }
}
