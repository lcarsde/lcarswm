package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode

interface MonitorObserver {
    fun toggleScreenMode(newScreenMode: ScreenMode)
}