package de.atennert.lcarswm.events

import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.RandrApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import xlib.RRScreenChangeNotify
import xlib.XEvent

class RandrHandlerFactory(
    randrApi: RandrApi,
    private val logger: Logger,
    private val monitorManager: MonitorManager,
    private val uiDrawer: UIDrawing
) {
    private val randrEventBase: Int
    private val randrErrorBase: Int

    init {
        val eventBase = IntArray(1).pin()
        val errorBase = IntArray(1).pin()

        randrApi.rQueryExtension(eventBase.addressOf(0), errorBase.addressOf(0))

        randrEventBase = eventBase.get()[0]
        randrErrorBase = errorBase.get()[0]

        logger.logDebug("RandrHandlerFactory::init::event base: $randrEventBase, error base: $randrErrorBase")
    }

    fun createScreenChangeHandler(): XEventHandler = object : XEventHandler {
        override val xEventType = randrEventBase + RRScreenChangeNotify

        override fun handleEvent(event: XEvent): Boolean {
            logger.logDebug("ScreenChangeHandler::handleEvent::screen changed")

            monitorManager.updateMonitorList()

            // TODO adjust root window size

            // TODO rearrange windows if necessary

            uiDrawer.drawWindowManagerFrame()

            return false
        }
    }
}