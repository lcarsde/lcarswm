package de.atennert.lcarswm.events

import xlib.ConfigureRequest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *
 */
class ConfigureRequestHandlerTest {
    @Test
    fun `has ConfigureRequest type`() {
        val configureRequestHandler = ConfigureRequestHandler()

        assertEquals(ConfigureRequest, configureRequestHandler.xEventType, "The ConfigureRequestHandler should have the ConfigureRequest type")
    }

    @Test
    fun `handle known window`() {
        val configureRequestHandler = ConfigureRequestHandler()

    }

    @Test
    fun `handle unknown window`() {
        val configureRequestHandler = ConfigureRequestHandler()

    }
}