package de.atennert.lcarswm.events

import de.atennert.lcarswm.system.SystemFacadeMock
import xlib.RRScreenChangeNotify
import kotlin.test.Test
import kotlin.test.assertEquals

class RandrHandlerFactoryTest {
    @Test
    fun `create RandrScreenChangeHandler`() {
        val systemApi = SystemFacadeMock()
        val randrHandlerFactory = RandrHandlerFactory(systemApi)

        val screenChangeHandler = randrHandlerFactory.createScreenChangeHandler()

        assertEquals(systemApi.randrEventBase + RRScreenChangeNotify,
                screenChangeHandler.xEventType,
                "The factory should create a screen change handler with the appropriate event type")
    }
}
