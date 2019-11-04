package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.ConfigureRequest
import xlib.XEvent
import xlib.XWindowChanges

/**
 *
 */
class ConfigureRequestHandler(
    private val eventApi: EventApi,
    private val logger: Logger,
    private val windowManagerState: WindowManagerStateHandler
) : XEventHandler {
    override val xEventType = ConfigureRequest

    override fun handleEvent(event: XEvent): Boolean {
        val configureEvent = event.xconfigurerequest

        logger.logDebug("::handleConfigureRequest::configure request for window ${configureEvent.window}, stack mode: ${configureEvent.detail}, sibling: ${configureEvent.above}, parent: ${configureEvent.parent}")

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
}