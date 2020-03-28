package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyConfiguration
import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.log.Logger
import xlib.MappingNotify
import xlib.Window
import xlib.XEvent

class MappingNotifyHandler(
    private val logger: Logger,
    private val keyManager: KeyManager,
    private val keyConfiguration: KeyConfiguration,
    private val rootWindowId: Window
) : XEventHandler {
    override val xEventType = MappingNotify

    override fun handleEvent(event: XEvent): Boolean {
        logger.logDebug("MappingNotifyHandler::handleEvent::reloading keyboard config")
        keyManager.ungrabAllKeys(rootWindowId)
        keyManager.reloadConfig()
        keyManager.grabInternalKeys(rootWindowId)
        keyConfiguration.reloadConfig()
        return false
    }
}