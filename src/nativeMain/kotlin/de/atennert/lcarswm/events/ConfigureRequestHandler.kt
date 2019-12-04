package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.EventApi
import de.atennert.lcarswm.windowactions.WindowCoordinator
import de.atennert.lcarswm.windowactions.WindowRegistration
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.ConfigureRequest
import xlib.XConfigureRequestEvent
import xlib.XEvent
import xlib.XWindowChanges

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

        sendConfigureNotify(eventApi, configureEvent.window, measurements)
    }

    private fun forwardConfigureRequest(configureEvent: XConfigureRequestEvent) {
        val windowChanges = nativeHeap.alloc<XWindowChanges>()
        windowChanges.x = configureEvent.x
        windowChanges.y = configureEvent.y
        windowChanges.width = configureEvent.width
        windowChanges.height = configureEvent.height
        windowChanges.sibling = configureEvent.above
        windowChanges.stack_mode = configureEvent.detail
        windowChanges.border_width = 0
        eventApi.configureWindow(configureEvent.window, configureEvent.value_mask.convert(), windowChanges.ptr)
    }
}