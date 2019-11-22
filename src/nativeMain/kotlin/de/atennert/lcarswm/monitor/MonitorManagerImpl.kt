package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.Window

class MonitorManagerImpl(private val randrApi: RandrApi, private val rootWindowId: Window) : MonitorManager {
    private var monitors: List<Monitor> = listOf();

    override fun updateMonitorList() {
        val screenResources = randrApi.rGetScreenResources(rootWindowId)!!.pointed
        val primaryScreen = randrApi.rGetOutputPrimary(rootWindowId)

        monitors = Array(screenResources.noutput) {screenResources.outputs!![it]}
                .map { Monitor(it, "", it == primaryScreen) }
    }

    override fun getMonitors(): List<Monitor> = monitors.toList()

    override fun getPrimaryMonitor(): Monitor = monitors.single { it.isPrimary }
}