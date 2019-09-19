package de.atennert.lcarswm.events

import de.atennert.lcarswm.*
import de.atennert.lcarswm.system.*
import de.atennert.lcarswm.windowactions.redrawRootWindow
import kotlinx.cinterop.*
import xlib.*

/**
 * Map of event types to event handlers. DON'T EDIT THE MAPS CONTENT!!!
 */
val EVENT_HANDLERS =
    hashMapOf<Int, Function6<CPointer<Display>, WindowManagerState, XEvent, CPointer<XImage>, Window, List<GC>, Boolean>>(
        Pair(KeyPress, ::handleKeyPress),
        Pair(KeyRelease, ::handleKeyRelease),
        Pair(ButtonPress, { _, _, e, _, _, _ -> logButtonPress(e) }),
        Pair(ButtonRelease, { _, _, e, _, _, _ -> logButtonRelease(e) }),
        Pair(ConfigureRequest, { d, w, e, _, _, _ -> handleConfigureRequest(d, w, e) }),
        Pair(MapRequest, { d, w, e, _, rw, _ -> handleMapRequest(d, w, e, rw) }),
        Pair(MapNotify, { _, _, e, _, _, _ -> logMapNotify(e) }),
        Pair(DestroyNotify, { _, w, e, _, _, _ -> handleDestroyNotify(w, e) }),
        Pair(UnmapNotify, ::handleUnmapNotify),
        Pair(ReparentNotify, { _, _, e, _, _, _ -> logReparentNotify(e) }),
        Pair(CreateNotify, { _, _, e, _, _, _ -> logCreateNotify(e) }),
        Pair(ConfigureNotify, { _, _, e, _, _, _ -> logConfigureNotify(e) })
    )

private fun handleKeyRelease(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Boolean {
    val releasedEvent = xEvent.xkey
    val key = releasedEvent.keycode
    println("::handleKeyRelease::Key released: $key")

    when (windowManagerState.keyboardKeys[key]) {
        XK_M -> toggleScreenMode(display, windowManagerState, image, rootWindow, graphicsContexts)
        XK_T -> loadAppFromKeyBinding("Win+T")
        XK_B -> loadAppFromKeyBinding("Win+B")
        XK_I -> loadAppFromKeyBinding("Win+I")
        XF86XK_AudioMute -> loadAppFromKeyBinding("XF86AudioMute")
        XF86XK_AudioLowerVolume -> loadAppFromKeyBinding("XF86AudioLowerVolume")
        XF86XK_AudioRaiseVolume -> loadAppFromKeyBinding("XF86AudioRaiseVolume")
        XK_F4 -> closeActiveWindow(display, windowManagerState)
        XK_Q -> return true
        else -> println("::handleKeyRelease::unknown key: $key")
    }
    return false
}


private fun loadAppFromKeyBinding(keyBinding: String) {
    val programConfig = readFromConfig(KEY_CONFIG_FILE, keyBinding) ?: return
    println("::loadAppFromKeyBinding::loading app for $keyBinding ${programConfig.size}")
    runProgram(programConfig[0], programConfig)
}

private fun closeActiveWindow(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState
) {
    val activeWindow = windowManagerState.activeWindow ?: return

    val supportedProtocols = nativeHeap.allocPointerTo<ULongVarOf<ULong>>()
    val numSupportedProtocols = IntArray(1).pin()

    val protocolsResult = xWindowUtilApi().getWMProtocols(display, activeWindow.id, supportedProtocols.ptr, numSupportedProtocols.addressOf(0))
    val min = supportedProtocols.pointed!!.value
    val max = min + numSupportedProtocols.get()[0].toULong()

    if (protocolsResult != 0 && (windowManagerState.wmDeleteWindow in (min .. max))) {
        val msg = nativeHeap.alloc<XEvent>()
        msg.xclient.type = ClientMessage
        msg.xclient.message_type = windowManagerState.wmProtocols
        msg.xclient.window = activeWindow.id
        msg.xclient.format = 32
        msg.xclient.data.l[0] = windowManagerState.wmDeleteWindow.convert()
        xEventApi().sendEvent(display, activeWindow.id, false, 0, msg.ptr)
    } else {
        xWindowUtilApi().killClient(display, activeWindow.id)
    }
}
