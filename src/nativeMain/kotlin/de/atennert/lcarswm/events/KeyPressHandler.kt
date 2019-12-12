package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.windowactions.WindowCoordinator
import xlib.KeyPress
import xlib.XEvent

class KeyPressHandler(
    private val keyManager: KeyManager,
    private val windowCoordinator: WindowCoordinator,
    private val uiDrawer: UIDrawing
) : XEventHandler {
    override val xEventType = KeyPress

    override fun handleEvent(event: XEvent): Boolean {
        uiDrawer.drawWindowManagerFrame()
        return false
    }
}
