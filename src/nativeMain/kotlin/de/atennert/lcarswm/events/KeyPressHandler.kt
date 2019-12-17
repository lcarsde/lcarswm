package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.windowactions.WindowCoordinator
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.convert
import xlib.*

class KeyPressHandler(
    private val keyManager: KeyManager,
    private val windowCoordinator: WindowCoordinator,
    private val windowFocusHandler: WindowFocusHandler,
    private val uiDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        val focusedWindow = windowFocusHandler.getFocusedWindow()

        when (keyManager.getKeySym(keyCode.convert()).convert<Int>()) {
            XK_Up -> moveWindowToNextMonitor(focusedWindow!!)
            XK_Down -> moveWindowToPreviousMonitor(focusedWindow!!)
            XK_Tab -> toggleFocusedWindow()
        }

        return false
    }

    private fun moveWindowToPreviousMonitor(focusedWindow: Window) {
        windowCoordinator.moveWindowToPreviousMonitor(focusedWindow)
        uiDrawer.drawWindowManagerFrame()
    }

    private fun moveWindowToNextMonitor(focusedWindow: Window) {
        windowCoordinator.moveWindowToNextMonitor(focusedWindow)
        uiDrawer.drawWindowManagerFrame()
    }

    private fun toggleFocusedWindow() {
        windowFocusHandler.toggleWindowFocus()
        val newFocusedWindow = windowFocusHandler.getFocusedWindow() ?: return
        windowCoordinator.stackWindowToTheTop(newFocusedWindow)
    }
}
