package de.atennert.lcarswm.events

import de.atennert.lcarswm.drawing.UIDrawing
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.window.WindowCoordinator
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.cinterop.usePinned
import xlib.RRScreenChangeNotify
import xlib.XEvent

class RandrHandlerFactory(systemApi: SystemApi, private val logger: Logger) {
    private val randrEventBase: Int
    private val randrErrorBase: Int

    init {
        val eventBase = IntArray(1)
        val errorBase = IntArray(1)

        eventBase.usePinned { eventBasePinned ->
            errorBase.usePinned { errorBasePinned ->
                systemApi.rQueryExtension(eventBasePinned.addressOf(0), errorBasePinned.addressOf(0))
            }
        }

        randrEventBase = eventBase[0]
        randrErrorBase = errorBase[0]

        logger.logDebug("RandrHandlerFactory::init::event base: $randrEventBase, error base: $randrErrorBase")
    }

    fun createScreenChangeHandler(
        monitorManager: MonitorManager,
        windowCoordinator: WindowCoordinator,
        uiDrawer: UIDrawing
    ): XEventHandler = object : XEventHandler {
        override val xEventType = randrEventBase + RRScreenChangeNotify

        override fun handleEvent(event: XEvent): Boolean {
            logger.logDebug("ScreenChangeHandler::handleEvent::screen changed")

            monitorManager.updateMonitorList()

            windowCoordinator.rearrangeActiveWindows()

            uiDrawer.drawWindowManagerFrame()

            return false
        }
    }
}