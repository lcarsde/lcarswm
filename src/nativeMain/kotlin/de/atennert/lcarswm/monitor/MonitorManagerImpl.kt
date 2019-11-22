package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.Window

class MonitorManagerImpl(private val randrApi: RandrApi, private val rootWindowId: Window) : MonitorManager {
    private var monitors: List<Monitor> = emptyList()

    override fun updateMonitorList() {
        val screenResources = randrApi.rGetScreenResources(rootWindowId)!!.pointed
        val primaryScreen = randrApi.rGetOutputPrimary(rootWindowId)

        val outputs = Array(screenResources.noutput) {screenResources.outputs!![it]}

        val checkedPrimary = if (outputs.contains(primaryScreen)) {
            primaryScreen
        } else {
            outputs[0]
        }

        monitors = Array(screenResources.noutput) {screenResources.outputs!![it]}
                .map { Monitor(it, "", it == checkedPrimary) }
    }

    override fun getMonitors(): List<Monitor> = monitors.toList()

    override fun getPrimaryMonitor(): Monitor = monitors.single { it.isPrimary }
}