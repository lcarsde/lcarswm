package de.atennert.lcarswm.events

import de.atennert.lcarswm.UIDrawingMock
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import xlib.RRScreenChangeNotify
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RandrScreenChangeHandlerTest {
    @Test
    fun `check correct type of RandrScreenChangeHandler`() {
        val systemApi = SystemFacadeMock()
        val monitorManager = MonitorManagerMock()
        val uiDrawer = UIDrawingMock()
        val randrHandlerFactory = RandrHandlerFactory(systemApi, LoggerMock(), monitorManager, uiDrawer)

        val screenChangeHandler = randrHandlerFactory.createScreenChangeHandler()

        assertEquals(systemApi.randrEventBase + RRScreenChangeNotify,
                screenChangeHandler.xEventType,
                "The factory should create a screen change handler with the appropriate event type")
    }

    @Test
    fun `handle screen change`() {
        val systemApi = SystemFacadeMock()
        val monitorManager = MonitorManagerMock()
        val uiDrawer = UIDrawingMock()

        val randrHandlerFactory = RandrHandlerFactory(systemApi, LoggerMock(), monitorManager, uiDrawer)

        val screenChangeHandler = randrHandlerFactory.createScreenChangeHandler()

        val screenChangeEvent = nativeHeap.alloc<XEvent>()
        screenChangeEvent.type = systemApi.randrEventBase + RRScreenChangeNotify

        val shutdownValue = screenChangeHandler.handleEvent(screenChangeEvent)

        val monitorManagerCalls = monitorManager.functionCalls
        val drawingCalls = uiDrawer.functionCalls

        assertFalse(shutdownValue, "Handling a screen change should close the window manager")

        val updateMonitorListCall = monitorManagerCalls.removeAt(0)
        assertEquals("updateMonitorList", updateMonitorListCall.name, "The monitor list needs to be updated")

        val redrawUiCall = drawingCalls.removeAt(0)
        assertEquals("drawWindowManagerFrame", redrawUiCall.name, "The window manager UI needs to be redrawn on the updated monitors")
    }
}
