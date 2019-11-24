package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.RROutput
import xlib.Window
import xlib.XRROutputInfo
import xlib.XRRScreenResources

class MonitorManagerImpl(private val randrApi: RandrApi, private val rootWindowId: Window) : MonitorManager {
    private var monitors: List<Monitor> = emptyList()

    override fun updateMonitorList() {
        val monitorData = getMonitorData()
        val monitorIds = getMonitorIds(monitorData)
        val primary = getPrimary(monitorIds)

        val monitorNames = monitorIds
                .mapNotNull { randrApi.rGetOutputInfo(monitorData, it) }
                .map(this::getOutputName)

        monitors = monitorIds.zip(monitorNames)
                .map { (id, name) -> Monitor(id, name, id == primary) }
    }

    private fun getMonitorData(): CPointer<XRRScreenResources> {
        return randrApi.rGetScreenResources(rootWindowId)!!
    }

    private fun getMonitorIds(monitorData: CPointer<XRRScreenResources>): Array<RROutput> {
        val screenResources = monitorData.pointed
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

    /**
     * Get the name of the given output.
     */
    private fun getOutputName(outputObject: CPointer<XRROutputInfo>): String {
        val name = outputObject.pointed.name
        val nameArray = ByteArray(outputObject.pointed.nameLen) { name!![it] }

        return nameArray.decodeToString()
    }

    override fun getMonitors(): List<Monitor> = monitors.toList()

    override fun getPrimaryMonitor(): Monitor = monitors.single { it.isPrimary }
}