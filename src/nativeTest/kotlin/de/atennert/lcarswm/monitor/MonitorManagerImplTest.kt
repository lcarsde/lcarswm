package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.convert
import xlib.RROutput
import xlib.Window
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonitorManagerImplTest {
    @Test
    fun `update monitor list`() {
        val systemApi = SystemFacadeMock()
        val monitorManager: MonitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        monitorManager.updateMonitorList()

        val monitorList = monitorManager.getMonitors()
        val primaryMonitor = monitorManager.getPrimaryMonitor()

        assertEquals(systemApi.outputs.size, monitorList.size, "There should be as many monitors as provided outputs")

        assertTrue(monitorList.contains(primaryMonitor), "The primary monitor should be part of the monitor list")
        assertEquals(systemApi.primaryOutput, primaryMonitor.id, "The ID of the primary monitor should match")
    }

    @Test
    fun `check the first monitor becomes primary, if there's no primary`() {
        val systemApi = object : SystemFacadeMock() {
            override fun rGetOutputPrimary(window: Window): RROutput = 0.convert()
        }
        val monitorManager: MonitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        monitorManager.updateMonitorList()

        val monitorList = monitorManager.getMonitors()
        val primaryMonitor = monitorManager.getPrimaryMonitor()

        assertEquals(monitorList[0], primaryMonitor, "The first monitor shall become the primary, if there's no primary")
    }
}
