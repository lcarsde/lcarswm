package de.atennert.lcarswm.events

import de.atennert.lcarswm.Monitor
import de.atennert.lcarswm.WindowContainer
import de.atennert.lcarswm.WindowManagerStateMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.MapRequest
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapRequestHandlerTest {
    @Test
    fun `has MapRequest type`() {
        val mapRequestHandler = MapRequestHandler(SystemFacadeMock(), WindowManagerStateTestImpl())

        assertEquals(MapRequest, mapRequestHandler.xEventType, "The MapRequestHandler should have the type MapRequest")
    }

    @Test
    fun `don't handle known windows`() {
        val system = SystemFacadeMock()
        val windowManagerState = WindowManagerStateTestImpl()

        val mapRequestHandler = MapRequestHandler(system, windowManagerState)

        val mapRequestEvent = nativeHeap.alloc<XEvent>()
        mapRequestEvent.type = MapRequest
        mapRequestEvent.xmaprequest.window = 1.convert()

        val shutdownValue = mapRequestHandler.handleEvent(mapRequestEvent)

        assertEquals(false, shutdownValue, "The MapRequestHandler shouldn't trigger a shutdown")

        assertEquals(0, system.functionCalls.size, "There shouldn't be calls to the outside")
        assertNull(windowManagerState.addedWindow, "There was no window added to the memory")
    }

    private class WindowManagerStateTestImpl : WindowManagerStateMock() {
        var addedWindow: WindowContainer? = null

        override fun addWindow(window: WindowContainer, monitor: Monitor) {
            addedWindow = window
        }
    }
}
