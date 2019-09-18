package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.system.xEventApi
import kotlinx.cinterop.*
import xlib.Display
import xlib.XEvent
import xlib.XWindowChanges

/**
 * Filter the values that lcarswm requires and send the configuration to X.
 */
fun handleConfigureRequest(
    display: CPointer<Display>,
    windowManagerState: WindowManagerState,
    xEvent: XEvent
): Boolean {
    val configureEvent = xEvent.xconfigurerequest

    println("::handleConfigureRequest::configure request for window ${configureEvent.window}, stack mode: ${configureEvent.detail}, sibling: ${configureEvent.above}, parent: ${configureEvent.parent}")

    if (windowManagerState.hasWindow(configureEvent.window)) {
        val windowPair = windowManagerState.windows.single {it.first.id == configureEvent.window}
        val measurements = windowPair.second.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowPair.second))

        val window = windowPair.first
        sendConfigureNotify(display, window.id, measurements)
        return false
    }

    val windowChanges = nativeHeap.alloc<XWindowChanges>()
    windowChanges.x = configureEvent.x
    windowChanges.y = configureEvent.y
    windowChanges.width = configureEvent.width
    windowChanges.height = configureEvent.height
    windowChanges.sibling = configureEvent.above
    windowChanges.stack_mode = configureEvent.detail
    windowChanges.border_width = 0

    xEventApi().configureWindow(display, configureEvent.window, configureEvent.value_mask.convert(), windowChanges.ptr)

    return false
}
