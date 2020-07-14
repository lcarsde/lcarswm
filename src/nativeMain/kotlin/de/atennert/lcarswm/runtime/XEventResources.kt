package de.atennert.lcarswm.runtime

import de.atennert.lcarswm.events.EventBuffer
import de.atennert.lcarswm.events.EventDistributor
import de.atennert.lcarswm.events.EventTime

data class XEventResources(val eventHandler: EventDistributor, val eventTime: EventTime, val eventBuffer: EventBuffer)