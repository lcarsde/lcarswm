package de.atennert.lcarswm.events

import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.WindowFocusHandler
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
    private val atomLibrary: AtomLibrary,
    configurationProvider: Properties,
    rootWindowId: Window
) :
    XEventHandler {
    override val xEventType = KeyRelease

    private val keyConfiguration = KeyConfiguration(systemApi, configurationProvider, keyManager, rootWindowId)

    override fun handleEvent(event: XEvent): Boolean {
        val keyCode = event.xkey.keycode
        val keyMask = event.xkey.state

        logger.logDebug("KeyReleaseHandler::handleEvent::key code: $keyCode, key mask: $keyMask")

        val keySym = keyManager.getKeySym(keyCode.convert()) ?: return false
        val winKeyMask = keyManager.modMasks.getValue(Modifiers.SUPER)

        when (Pair(keySym.convert<Int>(), keyMask.convert<Int>())) {
            Pair(XK_F4, winKeyMask) -> closeActiveWindow()
            Pair(XK_Q, winKeyMask) -> return true // shutdown the WM
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
        val focusedWindow = focusHandler.getFocusedWindow() ?: return

        logger.logInfo("KeyReleaseHandler::closeActiveWindow::focused window: $focusedWindow")

        val supportedProtocols = nativeHeap.allocPointerTo<AtomVar>()
        val numSupportedProtocols = IntArray(1).pin()

        val protocolsResult =
            systemApi.getWMProtocols(focusedWindow, supportedProtocols.ptr, numSupportedProtocols.addressOf(0))

        if (protocolsResult == 0) {
            logger.logDebug("KeyReleaseHandler::closeActiveWindow::kill window due to erroneous protocols")
            systemApi.killClient(focusedWindow)
            return
        }

        val protocols = ULongArray(numSupportedProtocols.get()[0]) { supportedProtocols.value!![it] }

        if (!protocols.contains(atomLibrary[Atoms.WM_DELETE_WINDOW])) {
            logger.logDebug("KeyReleaseHandler::closeActiveWindow::kill window due to missing WM_DELETE_WINDOW")
            systemApi.killClient(focusedWindow)
        } else {
            logger.logDebug("KeyReleaseHandler::closeActiveWindow::gracefully send WM_DELETE_WINDOW request")
            val msg = nativeHeap.alloc<XEvent>()
            msg.xclient.type = ClientMessage
            msg.xclient.message_type = atomLibrary[Atoms.WM_PROTOCOLS]
            msg.xclient.window = focusedWindow
            msg.xclient.format = 32
            msg.xclient.data.l[0] = atomLibrary[Atoms.WM_DELETE_WINDOW].convert()
            systemApi.sendEvent(focusedWindow, false, 0, msg.ptr)
        }
    }
}
