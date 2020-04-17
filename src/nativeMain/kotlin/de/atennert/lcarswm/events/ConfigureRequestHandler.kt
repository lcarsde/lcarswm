package de.atennert.lcarswm.events

import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.EventApi
import de.atennert.lcarswm.windowactions.WindowCoordinator
import de.atennert.lcarswm.windowactions.WindowRegistration
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.*

/**
 * Depending on whether we know or don't know the window of the configure request event, we adjust the windows
 * dimensions or simply forward the request.
 */
class ConfigureRequestHandler(
    private val eventApi: EventApi,
    private val logger: Logger,
    private val windowRegistration: WindowRegistration,
    private val windowCoordinator: WindowCoordinator
) : XEventHandler {
    override val xEventType = ConfigureRequest

    override fun handleEvent(event: XEvent): Boolean {
        val configureEvent = event.xconfigurerequest

        val isWindowKnown = windowRegistration.isWindowManaged(configureEvent.window)

        logger.logDebug("ConfigureRequestHandler::handleEvent::configure request for window ${configureEvent.window}, stack mode: ${configureEvent.detail}, sibling: ${configureEvent.above}, parent: ${configureEvent.parent}, is known: $isWindowKnown")

        if (isWindowKnown) {
            adjustWindowToScreen(configureEvent)
        } else {
            forwardConfigureRequest(configureEvent)
        }

        return false
    }

    private fun adjustWindowToScreen(configureEvent: XConfigureRequestEvent) {
        val measurements = windowCoordinator.getWindowMeasurements(configureEvent.window)

        val e = nativeHeap.alloc<XEvent>()
        e.type = ConfigureNotify
        e.xconfigure.display = eventApi.getDisplay()
        e.xconfigure.event = configureEvent.window
        e.xconfigure.window = configureEvent.window
        e.xconfigure.x = measurements.x
        e.xconfigure.y = measurements.y
        e.xconfigure.width = measurements.width
        e.xconfigure.height = measurements.height
        e.xconfigure.border_width = configureEvent.border_width
        e.xconfigure.above = None.convert()
        e.xconfigure.override_redirect = X_FALSE
        eventApi.sendEvent(configureEvent.window, false, StructureNotifyMask, e.ptr)
    }

    private fun forwardConfigureRequest(configureEvent: XConfigureRequestEvent) {
        val windowChanges = nativeHeap.alloc<XWindowChanges>()
        windowChanges.x = configureEvent.x
        windowChanges.y = configureEvent.y
        windowChanges.width = configureEvent.width
        windowChanges.height = configureEvent.height
        windowChanges.border_width = configureEvent.border_width
        windowChanges.sibling = configureEvent.above
        windowChanges.stack_mode = configureEvent.detail
        eventApi.configureWindow(configureEvent.window, configureEvent.value_mask.convert(), windowChanges.ptr)
    }
}