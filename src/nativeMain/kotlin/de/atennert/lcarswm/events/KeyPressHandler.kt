package de.atennert.lcarswm.events

import de.atennert.lcarswm.*
import de.atennert.lcarswm.drawing.UIDrawing
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.window.WindowCoordinator
import de.atennert.lcarswm.window.WindowFocusHandler
import kotlinx.cinterop.convert
import xlib.*

class KeyPressHandler(
    private val logger: Logger,
    private val keyManager: KeyManager,
    private val keyConfiguration: KeyConfiguration,
    private val monitorManager: MonitorManager,
    private val windowCoordinator: WindowCoordinator,
    private val windowFocusHandler: WindowFocusHandler,
    private val uiDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        val keyMask = keyManager.filterMask(event.xkey.state)

        val keySym = keyManager.getKeySym(keyCode.convert()) ?: return false

        logger.logDebug("KeyPressHandler::handleEvent::key code: $keyCode, key mask: $keyMask")

        keyConfiguration.getBindingForKey(keySym, keyMask)?.let { keyBinding ->
            logger.logDebug("KeyPressHandler::handleEvent::run command: ${keyBinding.command}")
            when (keyBinding) {
                is KeyAction -> act(keyBinding.command)
                else -> {/* so far we only handle key actions here */}
            }
        }

        return false
    }

    private fun act(actionCommand: String): Boolean {
        when (actionCommand) {
            "window-move-up" -> moveWindowToNextMonitor()
            "window-move-down" -> moveWindowToPreviousMonitor()
            "window-toggle-forward" -> toggleFocusedWindow()
            "screen-mode-toggle" -> toggleScreenMode()
            else -> {/* nothing to do, other actions are handled in key release */}
        }
        return false
    }

    private fun moveWindowToNextMonitor() {
        val focusedWindow = windowFocusHandler.getFocusedWindow() ?: return
        logger.logDebug("KeyPressHandler::moveWindowToNextMonitor::focused window: $focusedWindow")
        windowCoordinator.moveWindowToNextMonitor(focusedWindow)
        uiDrawer.drawWindowManagerFrame()
    }

    private fun moveWindowToPreviousMonitor() {
        val focusedWindow = windowFocusHandler.getFocusedWindow() ?: return
        logger.logDebug("KeyPressHandler::moveWindowToPreviousMonitor::focused window: $focusedWindow")
        windowCoordinator.moveWindowToPreviousMonitor(focusedWindow)
        uiDrawer.drawWindowManagerFrame()
    }

    private fun toggleFocusedWindow() {
        windowFocusHandler.toggleWindowFocus()
        val newFocusedWindow = windowFocusHandler.getFocusedWindow()
        logger.logDebug("KeyPressHandler::toggleFocusedWindow::new focused window: $newFocusedWindow")
    }

    private fun toggleScreenMode() {
        logger.logDebug("KeyPressHandler::toggleScreenMode::")
        monitorManager.toggleScreenMode()
        windowCoordinator.realignWindows()
        uiDrawer.drawWindowManagerFrame()
    }
}
