package de.atennert.lcarswm.events

import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.keys.*
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
    private val toggleSessionManager: KeySessionManager,
    private val atomLibrary: AtomLibrary
) :
    XEventHandler {
    override val xEventType = KeyRelease

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        val keyMask = keyManager.filterMask(event.xkey.state)

        logger.logDebug("KeyReleaseHandler::handleEvent::key code: $keyCode, key mask: $keyMask")
        toggleSessionManager.releaseKeys(keyCode)

        keyManager.getKeySym(keyCode.convert())?.let { keySym ->
            keyConfiguration.getBindingForKey(keySym, keyMask)?.let { keyBinding ->
                logger.logDebug("KeyReleaseHandler::handleEvent::run command: ${keyBinding.command}")
                return when (keyBinding) {
                    is KeyExecution -> {
                        execute(keyBinding.command)
                        false
                    }
                    is KeyAction -> act(keyBinding.action)
                }
            }
        }

        return false
    }

    private fun execute(execCommand: String) {
        val commandParts = execCommand.split(' ')
        runProgram(systemApi, commandParts[0], commandParts)
    }

    private fun act(action: WmAction): Boolean {
        when (action) {
            WmAction.WINDOW_CLOSE -> closeActiveWindow()
            WmAction.WM_QUIT -> return true
            else -> {/* nothing to do, other actions are handled in key press */}
        }
        return false
    }

    private fun closeActiveWindow() {
        val focusedWindowId = focusHandler.getFocusedWindow() ?: return

        closeWindow(logger, systemApi, atomLibrary, focusedWindowId)
    }
}
