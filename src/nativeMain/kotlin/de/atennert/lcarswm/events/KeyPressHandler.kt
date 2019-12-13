package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.windowactions.WindowCoordinator
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import xlib.KeyPress
import xlib.XEvent

class KeyPressHandler(
    private val keyManager: KeyManager,
    private val windowCoordinator: WindowCoordinator,
    private val windowFocusHandler: WindowFocusHandler,
    private val uiDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        windowCoordinator.moveWindowToNextMonitor(windowFocusHandler.getFocusedWindow()!!)
        uiDrawer.drawWindowManagerFrame()
        return false
    }
}
