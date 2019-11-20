package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.system.FunctionCall

class MonitorManagerMock : MonitorManager {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun updateMonitorList() {
        functionCalls.add(FunctionCall("updateMonitorList"))
    }
}