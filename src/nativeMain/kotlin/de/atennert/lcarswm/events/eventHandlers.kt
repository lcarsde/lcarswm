package de.atennert.lcarswm.events

import de.atennert.lcarswm.*
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.PosixApi
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.closeWindow
import kotlinx.cinterop.*
import xlib.*

/**
 * Map of event types to event handlers. DON'T EDIT THE MAPS CONTENT!!!
 */
val EVENT_HANDLERS =
    hashMapOf<Int, Function7<SystemApi, Logger, WindowManagerState, XEvent, CPointer<XImage>, Window, List<GC>, Boolean>>(
        Pair(KeyPress, ::handleKeyPress),
        Pair(KeyRelease, ::handleKeyRelease),
        Pair(ButtonPress, { _, l, _, e, _, _, _ -> logButtonPress(l, e) }),
        Pair(ButtonRelease, { _, l, _, e, _, _, _ -> logButtonRelease(l, e) }),
        Pair(ConfigureRequest, { s, l, w, e, _, _, _ -> handleConfigureRequest(s, l, w, e) }),
        Pair(MapRequest, { s, l, w, e, _, rw, _ -> handleMapRequest(s, l, w, e, rw) }),
        Pair(MapNotify, { _, l, _, e, _, _, _ -> logMapNotify(l, e) }),
        Pair(DestroyNotify, { _, l, w, e, _, _, _ -> handleDestroyNotify(l, w, e) }),
        Pair(UnmapNotify, ::handleUnmapNotify),
        Pair(ReparentNotify, { _, l, _, e, _, _, _ -> logReparentNotify(l, e) }),
        Pair(CreateNotify, { _, l, _, e, _, _, _ -> logCreateNotify(l, e) }),
        Pair(ConfigureNotify, { _, l, _, e, _, _, _ -> logConfigureNotify(l, e) })
    )

private fun handleKeyRelease(
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
            val window = windowManagerState.activeWindow
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


private fun loadAppFromKeyBinding(posixApi: PosixApi, logger: Logger, keyBinding: String) {
    val programConfig = readFromConfig(posixApi, KEY_CONFIG_FILE, keyBinding) ?: return
    logger.logInfo("::loadAppFromKeyBinding::loading app for $keyBinding - ${programConfig[0]}")
    runProgram(posixApi, programConfig[0], programConfig)
}
