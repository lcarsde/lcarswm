package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.system.FunctionCall
import kotlinx.cinterop.convert

class MonitorManagerMock : MonitorManager {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun updateMonitorList() {
        functionCalls.add(FunctionCall("updateMonitorList"))
    }

    override fun getMonitors(): List<Monitor> = listOf()

    val primaryMonitor = Monitor(42.convert(), "", true)
    override fun getPrimaryMonitor(): Monitor = primaryMonitor

    override fun getCombinedScreenSize(): Pair<Int, Int> = Pair(1920, 1080)

    override fun getScreenMode(): ScreenMode = ScreenMode.NORMAL
}