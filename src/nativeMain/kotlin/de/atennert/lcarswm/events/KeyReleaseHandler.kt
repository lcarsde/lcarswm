package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.convert
import xlib.KeyRelease
import xlib.XEvent
import xlib.XK_F4
import xlib.XK_Q

/**
 *
 */
class KeyReleaseHandler(
    private val systemApi: SystemApi,
    private val focusHandler: WindowFocusHandler,
    private val keyManager: KeyManager
) :
    XEventHandler {
    override val xEventType = KeyRelease

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        when (keyManager.getKeySym(keyCode.convert()).convert<Int>()) {
            XK_F4 -> closeActiveWindow()
            XK_Q -> return true
        }
        return false
    }

    private fun closeActiveWindow() {
        val focusedWindow = focusHandler.getFocusedWindow()
        if (focusedWindow != null) {
            systemApi.killClient(focusedWindow)
        }
    }
}
