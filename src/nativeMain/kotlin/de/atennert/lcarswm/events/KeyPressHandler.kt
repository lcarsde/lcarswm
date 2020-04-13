package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.Modifiers
import de.atennert.lcarswm.drawing.UIDrawing
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.windowactions.WindowCoordinator
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.convert
import xlib.*

class KeyPressHandler(
    private val logger: Logger,
    private val keyManager: KeyManager,
    private val monitorManager: MonitorManager,
    private val windowCoordinator: WindowCoordinator,
    private val windowFocusHandler: WindowFocusHandler,
    private val uiDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        val keyMask = keyManager.filterMask(event.xkey.state)
        val winKeyMask = keyManager.modMasks.getValue(Modifiers.SUPER)

        logger.logDebug("KeyPressHandler::handleEvent::key code: $keyCode, key mask: $keyMask")

        if (keyMask != winKeyMask) {
            return false
        }

        when (keyManager.getKeySym(keyCode.convert())?.convert<Int>()) {
            XK_Up -> moveWindowToNextMonitor()
            XK_Down -> moveWindowToPreviousMonitor()
            XK_Tab -> toggleFocusedWindow()
            XK_M -> toggleScreenMode()
        }

        return false
    }

    private fun moveWindowToPreviousMonitor() {
        val focusedWindow = windowFocusHandler.getFocusedWindow() ?: return
        logger.logDebug("KeyPressHandler::moveWindowToPreviousMonitor::focused window: $focusedWindow")
        windowCoordinator.moveWindowToPreviousMonitor(focusedWindow)
        uiDrawer.drawWindowManagerFrame()
    }

    private fun moveWindowToNextMonitor() {
        val focusedWindow = windowFocusHandler.getFocusedWindow() ?: return
        logger.logDebug("KeyPressHandler::moveWindowToNextMonitor::focused window: $focusedWindow")
        windowCoordinator.moveWindowToNextMonitor(focusedWindow)
        uiDrawer.drawWindowManagerFrame()
    }

    private fun toggleFocusedWindow() {
        windowFocusHandler.toggleWindowFocus()
        val newFocusedWindow = windowFocusHandler.getFocusedWindow() ?: return
        logger.logDebug("KeyPressHandler::toggleFocusedWindow::new focused window: $newFocusedWindow")
        windowCoordinator.stackWindowToTheTop(newFocusedWindow)
    }

    private fun toggleScreenMode() {
        logger.logDebug("KeyPressHandler::toggleScreenMode::")
        monitorManager.toggleScreenMode()
        windowCoordinator.realignWindows()
        uiDrawer.drawWindowManagerFrame()
    }
}
