package de.atennert.lcarswm.events

import de.atennert.lcarswm.drawing.UIDrawing
import de.atennert.lcarswm.keys.*
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.EventApi
import de.atennert.lcarswm.window.WindowCoordinator
import de.atennert.lcarswm.window.WindowFocusHandler
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import xlib.*

class KeyPressHandler(
    private val logger: Logger,
    private val eventApi: EventApi,
    private val keyManager: KeyManager,
    private val keyConfiguration: KeyConfiguration,
    private val keySessionManager: KeySessionManager,
    private val monitorManager: MonitorManager,
    private val windowCoordinator: WindowCoordinator,
    private val windowFocusHandler: WindowFocusHandler,
    private val uiDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        val keyMask = keyManager.filterMask(event.xkey.state)

        logger.logDebug("KeyPressHandler::handleEvent::key code: $keyCode, key mask: $keyMask")
        keySessionManager.pressKeys(keyCode, keyMask)

        val keySym = keyManager.getKeySym(keyCode.convert())
        if (keySym == null) {
            forwardEvent(event)
            return false
        }

        val keyBinding = keyConfiguration.getBindingForKey(keySym, keyMask)
        if (keyBinding != null) {
            when (keyBinding) {
                is KeyAction -> act(keyBinding.action)
                else -> {/* so far we only handle key actions here */}
            }
        } else {
            forwardEvent(event)
        }

        return false
    }

    private fun forwardEvent(event: XEvent) {
        windowFocusHandler.getFocusedWindow()?.let { focusedWindow ->
            event.xkey.window = focusedWindow
            eventApi.sendEvent(focusedWindow, false, KeyPressMask, event.ptr)
        }
    }

    private fun act(action: WmAction): Boolean {
        when (action) {
            WmAction.WINDOW_MOVE_UP -> moveWindowToNextMonitor()
            WmAction.WINDOW_MOVE_DOWN -> moveWindowToPreviousMonitor()
            WmAction.WINDOW_TOGGLE -> toggleFocusedWindow()
            WmAction.SCREEN_MODE_TOGGLE -> toggleScreenMode()
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
