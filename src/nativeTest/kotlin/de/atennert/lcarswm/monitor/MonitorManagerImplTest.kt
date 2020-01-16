package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.pointed
import xlib.RROutput
import xlib.Window
import xlib.XRROutputInfo
import xlib.XRRScreenResources
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

    @Test
    fun `check that monitors have their names`() {
        val systemApi = SystemFacadeMock()

        val monitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        monitorManager.updateMonitorList()

        val monitorList = monitorManager.getMonitors()

        monitorList.forEachIndexed { index, monitor ->
            assertEquals(systemApi.outputNames[index], monitor.name, "The monitor names should be correct")
        }
    }

    @Test
    fun `add monitor measurements`() {
        val systemApi = SystemFacadeMock()

        val monitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        monitorManager.updateMonitorList()

        val monitorList = monitorManager.getMonitors()

        monitorList.forEachIndexed { index, monitor ->
            val totalMeasurements = systemApi.crtcInfos[index]
            assertEquals(totalMeasurements[0], monitor.x, "The expected x value needs to match the monitors x value")
            assertEquals(totalMeasurements[1], monitor.y, "The expected y value needs to match the monitors y value")
            assertEquals(totalMeasurements[2], monitor.width, "The expected width needs to match the monitors width")
            assertEquals(totalMeasurements[3], monitor.height, "The expected height needs to match the monitors height")
        }
    }

    @Test
    fun `don't add monitors without crtc`() {
        val systemApi = object : SystemFacadeMock() {
            override fun rGetOutputInfo(
                resources: CPointer<XRRScreenResources>,
                output: RROutput
            ): CPointer<XRROutputInfo>? {
                val result = super.rGetOutputInfo(resources, output)
                if (output == outputs[1]) {
                    result!!.pointed.crtc = 0.convert()
                }
                return result
            }
        }

        val monitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        monitorManager.updateMonitorList()

        val monitorList = monitorManager.getMonitors()

        assertEquals(1, monitorList.size, "There should only be one monitor as the other doesn't have a crtc")

        assertEquals(systemApi.outputs[0], monitorList[0].id, "The monitor with crtc should be in the monitor list")
    }

    @Test
    fun `get the combined monitor screen size`() {
        val systemApi = SystemFacadeMock()

        val monitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        monitorManager.updateMonitorList()

        val (width, height) = monitorManager.getCombinedScreenSize()

        assertEquals(2000, width, "The width should match the combined screen width")

        assertEquals(500, height, "The height should match the combined screen height")
    }

    @Test
    fun `check that default screen mode is normal`() {
        val systemApi = SystemFacadeMock()

        val monitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        assertEquals(ScreenMode.NORMAL, monitorManager.getScreenMode(), "The default screen mode should be normal")
    }

    // TODO add change of screen mode

    @Test
    fun `change the screen mode`() {
        val systemApi = SystemFacadeMock()

        val monitorManager = MonitorManagerImpl(systemApi, systemApi.rootWindowId)

        monitorManager.toggleScreenMode()
        assertEquals(ScreenMode.MAXIMIZED, monitorManager.getScreenMode(), "The second mode should be maximized")

        monitorManager.toggleScreenMode()
        assertEquals(ScreenMode.FULLSCREEN, monitorManager.getScreenMode(), "The third mode should be fullscreen")

        monitorManager.toggleScreenMode()
        assertEquals(ScreenMode.NORMAL, monitorManager.getScreenMode(), "Finally it should wrap around back to normal")
    }
}
