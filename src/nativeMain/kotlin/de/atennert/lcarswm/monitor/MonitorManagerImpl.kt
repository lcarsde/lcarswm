package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.RROutput
import xlib.Window

class MonitorManagerImpl(private val randrApi: RandrApi, private val rootWindowId: Window) : MonitorManager {
    private var monitors: List<Monitor> = emptyList()

    override fun updateMonitorList() {
        val monitorIds = getMonitorIds()
        val primary = getPrimary(monitorIds)

        monitors = monitorIds
                .map { Monitor(it, "", it == primary) }
    }

    private fun getMonitorIds(): Array<RROutput> {
        val screenResources = randrApi.rGetScreenResources(rootWindowId)!!.pointed
        return Array(screenResources.noutput) { screenResources.outputs!![it] }
    }

    private fun getPrimary(monitorIds: Array<RROutput>): RROutput {
        val primaryMonitorId = randrApi.rGetOutputPrimary(rootWindowId)

        return if (monitorIds.contains(primaryMonitorId)) {
            primaryMonitorId
        } else {
            monitorIds[0]
        }
    }

    override fun getMonitors(): List<Monitor> = monitors.toList()

    override fun getPrimaryMonitor(): Monitor = monitors.single { it.isPrimary }
}