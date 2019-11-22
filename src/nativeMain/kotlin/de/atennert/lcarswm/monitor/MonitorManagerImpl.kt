package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.RROutput
import xlib.Window

class MonitorManagerImpl(private val randrApi: RandrApi, private val rootWindowId: Window) : MonitorManager {
    private var monitors: List<Monitor> = emptyList()

    override fun updateMonitorList() {
        val screenResources = randrApi.rGetScreenResources(rootWindowId)!!.pointed

        val outputs = Array(screenResources.noutput) {screenResources.outputs!![it]}
        val primary = getPrimary(outputs)

        monitors = outputs
                .map { Monitor(it, "", it == primary) }
    }

    private fun getPrimary(outputs: Array<RROutput>): RROutput {
        val primaryScreen = randrApi.rGetOutputPrimary(rootWindowId)

        return if (outputs.contains(primaryScreen)) {
            primaryScreen
        } else {
            outputs[0]
        }
    }

    override fun getMonitors(): List<Monitor> = monitors.toList()

    override fun getPrimaryMonitor(): Monitor = monitors.single { it.isPrimary }
}