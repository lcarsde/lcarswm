package de.atennert.lcarswm.events.old

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.events.sendConfigureNotify
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.XEvent
import xlib.XWindowChanges

/**
 * Filter the values that lcarswm requires and send the configuration to X.
 */
fun handleConfigureRequest(
    eventApi: EventApi,
    logger: Logger,
    windowManagerState: WindowManagerState,
    xEvent: XEvent
): Boolean {
    val configureEvent = xEvent.xconfigurerequest

    logger.logDebug("::handleConfigureRequest::configure request for window ${configureEvent.window}, stack mode: ${configureEvent.detail}, sibling: ${configureEvent.above}, parent: ${configureEvent.parent}")

    if (windowManagerState.hasWindow(configureEvent.window)) {
        val windowPair = windowManagerState.windows.single {it.first.id == configureEvent.window}
        val measurements = windowPair.second.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowPair.second))

        val window = windowPair.first
        sendConfigureNotify(eventApi, window.id, measurements)
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

    eventApi.configureWindow(configureEvent.window, configureEvent.value_mask.convert(), windowChanges.ptr)

    return false
}
