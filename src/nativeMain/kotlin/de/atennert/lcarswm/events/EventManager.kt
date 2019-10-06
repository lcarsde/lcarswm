package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.Logger
import xlib.XEvent

/** Use the EventManager.Builder to create this */
class EventManager private constructor(
    private val eventHandlers: Map<Int, XEventHandler>,
    private val logger: Logger
) {
    fun handleEvent(event: XEvent) {
        this.eventHandlers[event.type]?.handleEvent(event)
            ?: logger.logWarning("EventManager::handleEvent::no handler registered for event of type ${event.type}")
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