package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.window.AppMenuHandler
import de.atennert.lcarswm.window.StatusBarHandler
import de.atennert.lcarswm.window.WindowRegistration
import xlib.DestroyNotify
import xlib.XEvent

/**
 * The DestroyNotify event is triggered when a window was destroyed. If we (still) know the destroyed window, then we
 * need to clean up after it here.
 */
class DestroyNotifyHandler(
    private val logger: Logger,
    private val windowRegistration: WindowRegistration,
    private val appMenuHandler: AppMenuHandler,
    private val statusBarHandler: StatusBarHandler
) : XEventHandler {
    override val xEventType = DestroyNotify

    override fun handleEvent(event: XEvent): Boolean {
        val destroyedWindow = event.xdestroywindow.window
        logger.logDebug("DestroyNotifyHandler::handleEvent::clean up after destroyed window: $destroyedWindow")

        when {
            windowRegistration.isWindowManaged(destroyedWindow) -> {
                windowRegistration.removeWindow(destroyedWindow)
            }
            appMenuHandler.isKnownAppMenu(destroyedWindow) -> {
                appMenuHandler.removeWindow()
            }
            statusBarHandler.isStatusBar(destroyedWindow) -> {
                statusBarHandler.removeWindow()
            }
        }
        return false
    }
}