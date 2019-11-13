package de.atennert.lcarswm.events

import xlib.UnmapNotify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *
 */
class UnmapNotifyHandlerTest {
    @Test
    fun `return correct message type`() {
        val unmapNotifyHandler = UnmapNotifyHandler()

        assertEquals(UnmapNotify, unmapNotifyHandler.xEventType, "UnmapNotifyHandler should have type UnmapNotify")
    }
}