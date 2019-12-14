package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.windowactions.WindowCoordinator
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.convert
import xlib.KeyPress
import xlib.XEvent
import xlib.XK_Down
import xlib.XK_Up

class KeyPressHandler(
    private val keyManager: KeyManager,
    private val windowCoordinator: WindowCoordinator,
    private val windowFocusHandler: WindowFocusHandler,
    private val uiDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        when (keyManager.getKeySym(keyCode.convert()).convert<Int>()) {
            XK_Up -> {
                windowCoordinator.moveWindowToNextMonitor(windowFocusHandler.getFocusedWindow()!!)
                uiDrawer.drawWindowManagerFrame()
            }
            XK_Down -> {
                windowCoordinator.moveWindowToPreviousMonitor(windowFocusHandler.getFocusedWindow()!!)
                uiDrawer.drawWindowManagerFrame()
            }
        }

        return false
    }
}
