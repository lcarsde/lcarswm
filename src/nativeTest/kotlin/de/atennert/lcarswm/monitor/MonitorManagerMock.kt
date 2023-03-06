package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.system.FunctionCall
import kotlinx.cinterop.convert

open class MonitorManagerMock : MonitorManager {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun updateMonitorList() {
        functionCalls.add(FunctionCall("updateMonitorList"))
    }

    override fun getMonitors(): List<Monitor> = listOf(getPrimaryMonitor())

    val primaryMonitor = Monitor(this, 42.convert(), "", true)
    override fun getPrimaryMonitor(): Monitor = primaryMonitor

    override fun getCombinedScreenSize(): Pair<Int, Int> = Pair(1920, 1080)

    var screenMode = ScreenMode.NORMAL
    override fun getScreenMode(): ScreenMode = screenMode

    override fun toggleScreenMode() {
        functionCalls.add(FunctionCall("toggleScreenMode"))
    }

    override fun toggleFramedScreenMode() {
        functionCalls.add(FunctionCall("toggleFramedScreenMode"))
    }
}