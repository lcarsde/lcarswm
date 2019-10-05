package de.atennert.lcarswm.events

import xlib.XEvent

/** Use the builder to create this */
class EventManager private constructor(private val eventHandlers: Map<Int, XEventHandler>) {

    fun handleEvent(event: XEvent) {
        this.eventHandlers[event.type]?.handleEvent(event)
    }

    class Builder {
        private val eventHandlers = mutableListOf<XEventHandler>()

        fun addEventHandler(eventHandler: XEventHandler): Builder {
            this.eventHandlers.add(eventHandler)
            return this
        }

        fun build(): EventManager {
            val mappedEventHandlers = eventHandlers.associateBy { it.xEventType }
            return EventManager(mappedEventHandlers)
        }
    }
}