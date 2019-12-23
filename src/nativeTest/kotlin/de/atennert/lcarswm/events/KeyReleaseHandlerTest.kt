package de.atennert.lcarswm.events

import xlib.KeyRelease
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *
 */
class KeyReleaseHandlerTest {
    @Test
    fun `return the event type KeyReleaseHandler`() {
        val keyReleaseHandler = KeyReleaseHandler()

        assertEquals(KeyRelease, keyReleaseHandler.xEventType, "The type of the key release handler should be KeyRelease")
    }
}
