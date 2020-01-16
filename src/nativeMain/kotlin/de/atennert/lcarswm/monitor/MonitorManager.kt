package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode

interface MonitorManager {
    fun updateMonitorList()

    fun getMonitors(): List<Monitor>

    fun getPrimaryMonitor(): Monitor

    fun getCombinedScreenSize(): Pair<Int, Int>

    fun getScreenMode(): ScreenMode

    fun toggleScreenMode()
}