package de.atennert.lcarswm.events.old

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.loadAppFromKeyBinding
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.toggleScreenMode
import de.atennert.lcarswm.windowactions.closeWindow
import kotlinx.cinterop.CPointer
import xlib.*

/**
 *
 */
fun handleKeyRelease(
    system: SystemApi,
    logger: Logger,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Boolean {
    val releasedEvent = xEvent.xkey
    val key = releasedEvent.keycode
    logger.logDebug("::handleKeyRelease::Key released: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_M -> toggleScreenMode(system, windowManagerState, image, rootWindow, graphicsContexts)
        XK_T -> loadAppFromKeyBinding(system, logger, "Win+T")
        XK_B -> loadAppFromKeyBinding(system, logger, "Win+B")
        XK_I -> loadAppFromKeyBinding(system, logger, "Win+I")
        XF86XK_AudioMute -> loadAppFromKeyBinding(system, logger, "XF86AudioMute")
        XF86XK_AudioLowerVolume -> loadAppFromKeyBinding(system, logger, "XF86AudioLowerVolume")
        XF86XK_AudioRaiseVolume -> loadAppFromKeyBinding(system, logger, "XF86AudioRaiseVolume")
        XK_F4 -> {
            val window = windowManagerState.activeFramedWindow
            if (window != null) {
                logger.logDebug("::handleKeyRelease::closing window ${window.id}")
                closeWindow(window.id, system, windowManagerState)
            }
        }
        XK_Q -> {
            logger.logDebug("::handlerKeyRelease::closing WM")
            return true
        }
        else -> logger.logInfo("::handleKeyRelease::unknown key: $key")
    }
    return false
}
