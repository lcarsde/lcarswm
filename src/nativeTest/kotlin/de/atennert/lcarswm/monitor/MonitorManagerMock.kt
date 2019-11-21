package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.FunctionCall
import kotlinx.cinterop.convert

class MonitorManagerMock : MonitorManager {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun updateMonitorList() {
        functionCalls.add(FunctionCall("updateMonitorList"))
    }

    override fun getMonitors(): List<Monitor> = listOf()

    override fun getPrimaryMonitor(): Monitor = Monitor(1.convert(), "", true)
}