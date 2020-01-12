package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import xlib.XEvent

/** Use the EventManager.Builder to create this */
class EventManager private constructor(
    private val eventHandlers: Map<Int, XEventHandler>,
    private val logger: Logger
) {
    /**
     * Process a given event via one of the registered event handlers.
     * @return true if the WM should shut down, false otherwise
     */
    fun handleEvent(event: XEvent): Boolean {
        val eventHandler = this.eventHandlers[event.type]

        if (eventHandler == null) {
            logger.logWarning("EventManager::handleEvent::no handler registered for event of type ${event.type}")
            return false
        }

        return eventHandler.handleEvent(event)
    }

    class Builder(private val logger: Logger) {
        private val eventHandlers = mutableListOf<XEventHandler>()

        fun addEventHandler(eventHandler: XEventHandler): Builder {
            this.eventHandlers.add(eventHandler)
            return this
        }

        fun build(): EventManager {
            val mappedEventHandlers = eventHandlers.associateBy { it.xEventType }
            return EventManager(mappedEventHandlers, logger)
        }
    }
}