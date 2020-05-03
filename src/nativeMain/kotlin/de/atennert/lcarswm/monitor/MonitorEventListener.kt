package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode

interface MonitorEventListener {
    fun toggleScreenMode(newScreenMode: ScreenMode)
}