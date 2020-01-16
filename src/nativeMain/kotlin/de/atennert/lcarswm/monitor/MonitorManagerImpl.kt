package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.*
import kotlin.math.max

class MonitorManagerImpl(private val randrApi: RandrApi, private val rootWindowId: Window) : MonitorManager {
    private var monitors: List<Monitor> = emptyList()

    private var screenMode = ScreenMode.NORMAL

    override fun updateMonitorList() {
        val monitorData = getMonitorData()
        val monitorIds = getMonitorIds(monitorData)
        val primary = getPrimary(monitorIds)

        val activeMonitorInfos = monitorIds
            .map { monitorId -> Pair(monitorId, randrApi.rGetOutputInfo(monitorData, monitorId)) }
            .filter { (_, outputInfo) -> outputInfo != null && outputInfo.pointed.crtc.convert<Int>() != 0 }

        val monitorNames = activeMonitorInfos
            .map { (_, outputInfo) -> getOutputName(outputInfo!!) }

        monitors = activeMonitorInfos
            .map { (monitorId, _) -> monitorId }
            .zip(monitorNames)
            .map { (id, name) -> Monitor(this, id, name, id == primary) }
            .zip(activeMonitorInfos.map { it.second })
            .map { (monitor, outputInfo) -> addMeasurementToMonitor(monitor, outputInfo!!.pointed.crtc, monitorData) }
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

    private fun addMeasurementToMonitor(
        monitor: Monitor,
        crtcReference: RRCrtc,
        monitorData: CPointer<XRRScreenResources>
    ): Monitor {
        val crtcInfo = randrApi.rGetCrtcInfo(monitorData, crtcReference)!!.pointed

        monitor.setMonitorMeasurements(crtcInfo.x, crtcInfo.y, crtcInfo.width, crtcInfo.height)

        return monitor
    }

    override fun getMonitors(): List<Monitor> = monitors.toList()

    override fun getPrimaryMonitor(): Monitor = monitors.single { it.isPrimary }

    override fun getCombinedScreenSize(): Pair<Int, Int> = monitors
        .fold(Pair(0, 0)) { (oldWidth, oldHeight), monitor ->
            Pair(
                max(monitor.x + monitor.width, oldWidth),
                max(monitor.y + monitor.height, oldHeight)
            )
        }

    override fun getScreenMode(): ScreenMode {
        return screenMode
    }

    override fun toggleScreenMode() {
        screenMode = when (screenMode) {
            ScreenMode.NORMAL -> ScreenMode.MAXIMIZED
            ScreenMode.MAXIMIZED -> ScreenMode.FULLSCREEN
            ScreenMode.FULLSCREEN -> ScreenMode.NORMAL
        }
    }
}