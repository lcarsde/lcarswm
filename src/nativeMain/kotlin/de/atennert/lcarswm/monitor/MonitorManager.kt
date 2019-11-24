package de.atennert.lcarswm.monitor

interface MonitorManager {
    fun updateMonitorList()

    fun getMonitors(): List<Monitor>

    fun getPrimaryMonitor(): Monitor
}