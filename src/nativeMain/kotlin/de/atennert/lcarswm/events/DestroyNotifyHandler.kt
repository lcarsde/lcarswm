package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import xlib.DestroyNotify
import xlib.XEvent

/**
 *
 */
class DestroyNotifyHandler(
    private val system: SystemApi,
    private val logger: Logger,
    private val windowManagerState: WindowManagerStateHandler
) : XEventHandler {
    override val xEventType = DestroyNotify

    override fun handleEvent(event: XEvent): Boolean {
        val destroyEvent = event.xdestroywindow
        logger.logDebug("DestroyNotifyHandler::handleEvent::destroy window: ${destroyEvent.window}")
        if (windowManagerState.hasWindow(destroyEvent.window)) {
            val window = windowManagerState.windows.map { it.first }.single { it.id == destroyEvent.window }
            system.unmapWindow(window.frame)
            system.removeFromSaveSet(destroyEvent.window)
            system.destroyWindow(window.frame)

            windowManagerState.removeWindow(destroyEvent.window)
        }
        return false
    }
}