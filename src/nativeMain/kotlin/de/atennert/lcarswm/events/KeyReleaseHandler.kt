package de.atennert.lcarswm.events

import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.window.WindowFocusHandler
import de.atennert.lcarswm.window.closeWindow
import kotlinx.cinterop.*
import xlib.*

/**
 * Handles key release events. This covers a few internal codes as well as configured key bindings.
 */
class KeyReleaseHandler(
    private val logger: Logger,
    private val systemApi: SystemApi,
    private val focusHandler: WindowFocusHandler,
    private val keyManager: KeyManager,
    private val keyConfiguration: KeyConfiguration,
    private val atomLibrary: AtomLibrary
) :
    XEventHandler {
    override val xEventType = KeyRelease

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        val keyMask = keyManager.filterMask(event.xkey.state)

        logger.logDebug("KeyReleaseHandler::handleEvent::key code: $keyCode, key mask: $keyMask")

        val keySym = keyManager.getKeySym(keyCode.convert()) ?: return false
        val requiredKeyMask = keyManager.modMasks[LCARS_WM_KEY_SYMS[keySym.convert()]]

        when (Pair(keySym.convert<Int>(), keyMask)) {
            Pair(XK_F4, requiredKeyMask) -> closeActiveWindow()
            Pair(XK_Q, requiredKeyMask) -> return true // shutdown the WM
            else -> {
                keyConfiguration.getCommandForKey(keySym, keyMask)?.let { command ->
                    logger.logDebug("KeyReleaseHandler::handleEvent::run command: $command")
                    val commandParts = command.split(' ')
                    runProgram(systemApi, commandParts[0], commandParts)
                }
            }
        }
        return false
    }

    private fun closeActiveWindow() {
        val focusedWindowId = focusHandler.getFocusedWindow() ?: return

        closeWindow(logger, systemApi, atomLibrary, focusedWindowId)
    }
}
