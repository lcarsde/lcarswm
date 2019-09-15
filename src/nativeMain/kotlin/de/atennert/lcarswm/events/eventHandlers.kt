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

private fun handleMapRequest(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    rootWindow: Window
): Boolean {
    val mapEvent = xEvent.xmaprequest
    val window = mapEvent.window

    println("::handleMapRequest::map request for window $window, parent: ${mapEvent.parent}")
    if (windowManagerState.getWindowMonitor(window) != null) {
        return false
    }

    addWindow(display, windowManagerState, rootWindow, window, false)

    return false
}

/**
 * Filter the values that lcarswm requires and send the configuration to X.
 */
private fun handleConfigureRequest(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent
): Boolean {
    val configureEvent = xEvent.xconfigurerequest

    println("::handleConfigureRequest::configure request for window ${configureEvent.window}, stack mode: ${configureEvent.detail}, sibling: ${configureEvent.above}, parent: ${configureEvent.parent}")

    val windowChanges = nativeHeap.alloc<XWindowChanges>()
    windowChanges.x = configureEvent.x
    windowChanges.y = configureEvent.y
    windowChanges.width = configureEvent.width
    windowChanges.height = configureEvent.height
    windowChanges.sibling = configureEvent.above
    windowChanges.stack_mode = configureEvent.detail
    windowChanges.border_width = 0

    if (windowManagerState.hasWindow(configureEvent.window)) {
        val windowPair = windowManagerState.windows.single {it.first.id == configureEvent.window}
        val measurements = windowPair.second.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowPair.second))

        val window = windowPair.first
        sendConfigureNotify(display, window.id, measurements)
        return false
    }

    xEventApi().configureWindow(display, configureEvent.window, configureEvent.value_mask.convert(), windowChanges.ptr)

    return false
}


/**
 * Remove the window from the wm data on window unmap.
 */
private fun handleUnmapNotify(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Boolean {
    val unmapEvent = xEvent.xunmap
    println("::handleUnmapNotify::unmapped window: ${unmapEvent.window}")
    // only the active window can be closed, so make a new window active
    if (windowManagerState.hasWindow(unmapEvent.window) && unmapEvent.event != rootWindow) {
        val window = windowManagerState.windows.map { it.first }.single { it.id == unmapEvent.window }
        xEventApi().unmapWindow(display, window.frame)
        xEventApi().reparentWindow(display, unmapEvent.window, rootWindow, 0, 0)
        xWindowUtilApi().removeFromSaveSet(display, unmapEvent.window)
        xEventApi().destroyWindow(display, window.frame)

        windowManagerState.removeWindow(unmapEvent.window)
        moveNextWindowToTopOfStack(display, windowManagerState)
    } else if (windowManagerState.activeWindow != null) {
        xInputApi().setInputFocus(display, windowManagerState.activeWindow!!.id, RevertToNone, CurrentTime.convert())
    }

    redrawRootWindow(windowManagerState, graphicsContexts, rootWindow, display, image)
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
