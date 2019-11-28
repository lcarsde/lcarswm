package de.atennert.lcarswm.events

import de.atennert.lcarswm.UIDrawing
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.WindowCoordinator
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.pin
import xlib.RRScreenChangeNotify
import xlib.Window
import xlib.XEvent

class RandrHandlerFactory(
    private val systemApi: SystemApi,
    private val logger: Logger,
    private val monitorManager: MonitorManager,
    private val windowCoordinator: WindowCoordinator,
    private val uiDrawer: UIDrawing,
    private val rootWindowId: Window
) {
    private val randrEventBase: Int
    private val randrErrorBase: Int

    init {
        val eventBase = IntArray(1).pin()
        val errorBase = IntArray(1).pin()

        systemApi.rQueryExtension(eventBase.addressOf(0), errorBase.addressOf(0))

        randrEventBase = eventBase.get()[0]
        randrErrorBase = errorBase.get()[0]

        logger.logDebug("RandrHandlerFactory::init::event base: $randrEventBase, error base: $randrErrorBase")
    }

    fun createScreenChangeHandler(): XEventHandler = object : XEventHandler {
        override val xEventType = randrEventBase + RRScreenChangeNotify

        override fun handleEvent(event: XEvent): Boolean {
            logger.logDebug("ScreenChangeHandler::handleEvent::screen changed")

            monitorManager.updateMonitorList()

            val (width, height) = monitorManager.getCombinedScreenSize()
            systemApi.resizeWindow(rootWindowId, width.convert(), height.convert())

            windowCoordinator.rearrangeActiveWindows()

            uiDrawer.drawWindowManagerFrame()

            return false
        }
    }
}