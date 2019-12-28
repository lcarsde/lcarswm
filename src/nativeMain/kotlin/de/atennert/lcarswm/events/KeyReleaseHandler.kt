package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
class KeyReleaseHandler(
    private val systemApi: SystemApi,
    private val focusHandler: WindowFocusHandler,
    private val keyManager: KeyManager,
    private val atomLibrary: AtomLibrary
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
        val focusedWindow = focusHandler.getFocusedWindow() ?: return

        val supportedProtocols = nativeHeap.allocArrayOfPointersTo<AtomVar>()
        val numSupportedProtocols = IntArray(1).pin()

        val protocolsResult =
            systemApi.getWMProtocols(focusedWindow, supportedProtocols, numSupportedProtocols.addressOf(0))

        if (protocolsResult != 0) {
            systemApi.killClient(focusedWindow)
            return
        }

        val protocols = ULongArray(numSupportedProtocols.get()[0]) { supportedProtocols.pointed.value!![it] }

        if (!protocols.contains(atomLibrary[Atoms.WM_DELETE_WINDOW])) {
            systemApi.killClient(focusedWindow)
        }
    }
}
