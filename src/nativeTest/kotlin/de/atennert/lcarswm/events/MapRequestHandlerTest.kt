package de.atennert.lcarswm.events

import xlib.MapRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class MapRequestHandlerTest {
    @Test
    fun `has MapRequest type`() {
        val mapRequestHandler = MapRequestHandler()

        assertEquals(MapRequest, mapRequestHandler.xEventType, "The MapRequestHandler should have the type MapRequest")
    }
}
