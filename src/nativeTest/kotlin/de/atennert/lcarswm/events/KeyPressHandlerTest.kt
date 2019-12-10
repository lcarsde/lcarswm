package de.atennert.lcarswm.events

import xlib.KeyPress
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyPressHandlerTest {
    @Test
    fun `return the event type KeyPressHandler`() {
        val keyPressHandler = KeyPressHandler()

        assertEquals(KeyPress, keyPressHandler.xEventType, "The key press handler should have the correct type")
    }
}