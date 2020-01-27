package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode

/**
 * Interface for manager for handling monitors and the current screen mode for those monitors.
 * The screen modes are defined in {@link ScreenMode}
 */
interface MonitorManager {
    /**
     * Update the list of known active monitors
     */
    fun updateMonitorList()

    /**
     * @return the list of known active monitors
     */
    fun getMonitors(): List<Monitor>

    /**
     * @return the primary monitor, if there's no monitor marked as primary monitor, the monitor manager will select the first monitor it finds as primary
     */
    fun getPrimaryMonitor(): Monitor

    /**
     * @return the combined size of all monitors, depending on the configured monitor screen size and the positioning of the monitors
     */
    fun getCombinedScreenSize(): Pair<Int, Int>

    /**
     * @return the current screen mode
     */
    fun getScreenMode(): ScreenMode

    /**
     * Toggle between all ScreenModes
     */
    fun toggleScreenMode()
}