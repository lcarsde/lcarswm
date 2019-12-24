package de.atennert.lcarswm.events

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import xlib.KeyRelease
import xlib.XEvent
import xlib.XK_Q
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *
 */
class KeyReleaseHandlerTest {
    @Test
    fun `return the event type KeyReleaseHandler`() {
        val keyReleaseHandler = KeyReleaseHandler()

        assertEquals(KeyRelease, keyReleaseHandler.xEventType, "The type of the key release handler should be KeyRelease")
    }

    @Test
    fun `shutdown on Q`() {
        val systemApi = SystemFacadeMock()

        val event = nativeHeap.alloc<XEvent>()
        event.type = KeyRelease
        event.xkey.keycode = systemApi.keySyms.getValue(XK_Q).convert()
        event.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler()

        val shutdownValue = keyReleaseHandler.handleEvent(event)

        assertTrue(shutdownValue, "The window manager should shut down on Q")
    }
}
