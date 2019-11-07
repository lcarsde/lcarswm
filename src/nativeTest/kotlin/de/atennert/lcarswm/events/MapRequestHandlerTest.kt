package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.windowactions.WindowRegistrationMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.MapRequest
import xlib.Window
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class MapRequestHandlerTest {
    @Test
    fun `has MapRequest type`() {
        val mapRequestHandler = MapRequestHandler(LoggerMock(), WindowRegistrationMock())

        assertEquals(MapRequest, mapRequestHandler.xEventType, "The MapRequestHandler should have the type MapRequest")
    }

    @Test
    fun `don't handle known windows`() {
        val windowRegistration = object : WindowRegistrationMock() {
            override fun isWindowManaged(windowId: Window): Boolean = true
        }

        val mapRequestHandler = MapRequestHandler(LoggerMock(), windowRegistration)

        val mapRequestEvent = nativeHeap.alloc<XEvent>()
        mapRequestEvent.type = MapRequest
        mapRequestEvent.xmaprequest.window = 1.convert()

        val shutdownValue = mapRequestHandler.handleEvent(mapRequestEvent)

        assertEquals(false, shutdownValue, "The MapRequestHandler shouldn't trigger a shutdown")

        assertTrue(windowRegistration.functionCalls.isEmpty(), "There shouldn't be actions on the window registration")
    }

    @Test
    fun `handle unknown windows`() {
        val windowRegistration = WindowRegistrationMock()

        val mapRequestHandler = MapRequestHandler(LoggerMock(), windowRegistration)

        val mapRequestEvent = nativeHeap.alloc<XEvent>()
        mapRequestEvent.type = MapRequest
        mapRequestEvent.xmaprequest.window = 1.convert()

        val shutdownValue = mapRequestHandler.handleEvent(mapRequestEvent)

        val registrationCalls = windowRegistration.functionCalls

        assertEquals(false, shutdownValue, "The MapRequestHandler shouldn't trigger a shutdown")

        val addWindowCall = registrationCalls.removeAt(0)
        assertEquals("addWindow", addWindowCall.name, "An unknown window should be added")
        assertEquals(mapRequestEvent.xmaprequest.window, addWindowCall.parameters[0], "The added window should be the one from the map event")
    }
}
