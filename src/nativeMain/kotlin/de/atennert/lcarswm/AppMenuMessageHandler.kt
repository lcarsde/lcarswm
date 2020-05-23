package de.atennert.lcarswm

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.window.WindowFocusHandler
import de.atennert.lcarswm.window.WindowList
import de.atennert.lcarswm.window.closeWindow

class AppMenuMessageHandler(
    private val logger: Logger,
    private val systemApi: SystemApi,
    private val atomLibrary: AtomLibrary,
    private val windowList: WindowList,
    private val focusHandler: WindowFocusHandler
) {
    private val messageHandlers = mapOf(
        Pair("close", this::handleClose),
        Pair("select", this::handleSelection)
    )

    fun handleMessage(message: String) {
        val messageLines = message.lines()
        val messageType = messageLines[0]
        val messageContent = messageLines.drop(1)

        messageHandlers[messageType]?.invoke(messageContent)
            ?: logger.logWarning("AppMenuMessageHandler::handleMessage::unknown message type $messageType")
    }

    private fun handleClose(message: List<String>) {
        val targetWindowId = message[0].toULong()
        logger.logDebug("AppMenuMessageHandler::handleClose::$targetWindowId")

        if (windowList.isManaged(targetWindowId)) {
            closeWindow(logger, systemApi, atomLibrary, targetWindowId)
        }
    }

    private fun handleSelection(message: List<String>) {
        val targetWindow = message[0].toULong()
        logger.logDebug("AppMenuMessageHandler::handleSelection::$targetWindow")

        if (windowList.isManaged(targetWindow)) {
            focusHandler.setFocusedWindow(targetWindow)
        }
    }
}