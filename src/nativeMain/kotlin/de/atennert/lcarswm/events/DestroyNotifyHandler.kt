package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import xlib.DestroyNotify
import xlib.XEvent

/**
 * The DestroyNotify event is triggered when a window was destroyed. If we (still) know the destroyed window, then we
 * need to clean up after it here.
 */
class DestroyNotifyHandler(
    private val system: SystemApi,
    private val logger: Logger,
    private val windowManagerState: WindowManagerStateHandler // TODO use WindowRegistration
) : XEventHandler {
    override val xEventType = DestroyNotify

    override fun handleEvent(event: XEvent): Boolean {
        val destroyedWindow = event.xdestroywindow.window
        logger.logDebug("DestroyNotifyHandler::handleEvent::clean up after destroyed window: $destroyedWindow")
        
        if (windowManagerState.hasWindow(destroyedWindow)) {
            val windowContainer = windowManagerState.getWindowContainer(destroyedWindow)
            system.unmapWindow(windowContainer.frame)
            system.removeFromSaveSet(destroyedWindow)
            system.destroyWindow(windowContainer.frame)

            windowManagerState.removeWindow(destroyedWindow)
        }
        return false
    }
}