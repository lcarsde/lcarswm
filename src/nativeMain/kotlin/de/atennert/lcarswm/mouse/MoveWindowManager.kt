package de.atennert.lcarswm.mouse

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorObserver
import de.atennert.lcarswm.window.FramedWindow
import de.atennert.lcarswm.window.WindowCoordinator

class MoveWindowManager(
    private val logger: Logger,
    private val windowCoordinator: WindowCoordinator
) : MonitorObserver {

    private var monitors = emptyList<Monitor>()

    private lateinit var lastWindowMonitor: Monitor

    private var targetWindow: FramedWindow? = null

    fun press(window: FramedWindow, x: Int, y: Int) {
        targetWindow?.let {
            logger.logWarning("MoveWindowManager::press::apparently there was a window moving in process for ${it.id}.")
        }

        targetWindow = window
        lastWindowMonitor = getMonitor(x, y) ?: return
    }

    fun move(x: Int, y: Int) {
        targetWindow?.let {
            val newMonitor = getMonitor(x, y) ?: return
            if (newMonitor.id != lastWindowMonitor.id) {
                windowCoordinator.moveWindowToMonitor(it.id, newMonitor)
                lastWindowMonitor = newMonitor
            }
        }
    }

    fun release() {
        targetWindow = null
    }

    private fun getMonitor(x: Int, y: Int): Monitor? {
        try {
            return monitors.first { it.isOnMonitor(x, y) }
        } catch (e: Exception) {
            logger.logError(e.toString())
            return null
        }
    }

    override fun toggleScreenMode(newScreenMode: ScreenMode) {}

    override fun updateMonitors(monitors: List<Monitor>) {
        this.monitors = monitors
        logger.logDebug("MoveWindowManager::updateMonitors::$monitors")

        // cancel moving the window
        targetWindow?.let {
            logger.logWarning("MoveWindowManager::updateMonitors::cancel moving of ${it.id}")
        }
        targetWindow = null
    }
}