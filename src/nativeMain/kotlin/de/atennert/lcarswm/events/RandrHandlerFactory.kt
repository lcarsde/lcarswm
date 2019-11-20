package de.atennert.lcarswm.events

import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import xlib.RRScreenChangeNotify
import xlib.XEvent

class RandrHandlerFactory(randrApi: RandrApi) {
    private val randrEventBase: Int
    private val randrErrorBase: Int

    init {
        val eventBase = IntArray(1).pin()
        val errorBase = IntArray(1).pin()

        randrApi.rQueryExtension(eventBase.addressOf(0), errorBase.addressOf(0))

        randrEventBase = eventBase.get()[0]
        randrErrorBase = errorBase.get()[0]
    }

    fun createScreenChangeHandler(): XEventHandler = object : XEventHandler {
        override val xEventType = randrEventBase + RRScreenChangeNotify

        override fun handleEvent(event: XEvent): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}