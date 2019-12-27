package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.system.SystemFacadeMock
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 *
 */
class KeyReleaseHandlerTest {
    @Test
    fun `return the event type KeyReleaseHandler`() {
        val systemApi = SystemFacadeMock()
        val focusHandler = WindowFocusHandler()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager)

        assertEquals(
            KeyRelease,
            keyReleaseHandler.xEventType,
            "The type of the key release handler should be KeyRelease"
        )
    }

    @Test
    fun `shutdown on Q`() {
        val systemApi = SystemFacadeMock()
        val focusHandler = WindowFocusHandler()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        keyManager.grabInputControls()

        systemApi.functionCalls.clear()

        val keyReleaseEvent = nativeHeap.alloc<XEvent>()
        keyReleaseEvent.type = KeyRelease
        keyReleaseEvent.xkey.keycode = systemApi.keySyms.getValue(XK_Q).convert()
        keyReleaseEvent.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager)

        val shutdownValue = keyReleaseHandler.handleEvent(keyReleaseEvent)

        assertTrue(shutdownValue, "The window manager should shut down on Q")
    }

    @Test
    fun `kill active window if the WM protocols result is != 0 on F4`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getWMProtocols(
                window: Window,
                protocolsReturn: CPointer<CPointerVar<AtomVar>>,
                protocolCountReturn: CPointer<IntVar>
            ): Int = -1
        }
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val focusHandler = WindowFocusHandler()
        val windowId = systemApi.getNewWindowId()
        focusHandler.setFocusedWindow(windowId)
        keyManager.grabInputControls()

        systemApi.functionCalls.clear()

        val keyReleaseEvent = nativeHeap.alloc<XEvent>()
        keyReleaseEvent.type = KeyRelease
        keyReleaseEvent.xkey.keycode = systemApi.keySyms.getValue(XK_F4).convert()
        keyReleaseEvent.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager)

        val shutdownValue = keyReleaseHandler.handleEvent(keyReleaseEvent)

        assertFalse(shutdownValue, "The window manager should not shut down on F4")

        val killClientCall = systemApi.functionCalls.removeAt(0)
        assertEquals("killClient", killClientCall.name, "The window should be killed if we can't get its protocols")
        assertEquals(windowId, killClientCall.parameters[0], "The active window needs to be killed")
    }

    @Test
    fun `kill active window if the WM protocols don't contain WM_DELETE_WINDOW`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getWMProtocols(
                window: Window,
                protocolsReturn: CPointer<CPointerVar<AtomVar>>,
                protocolCountReturn: CPointer<IntVar>
            ): Int {
                protocolCountReturn.pointed.value = 0
                return 0
            }
        }
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val focusHandler = WindowFocusHandler()
        val windowId = systemApi.getNewWindowId()
        focusHandler.setFocusedWindow(windowId)
        keyManager.grabInputControls()

        systemApi.functionCalls.clear()

        val keyReleaseEvent = nativeHeap.alloc<XEvent>()
        keyReleaseEvent.type = KeyRelease
        keyReleaseEvent.xkey.keycode = systemApi.keySyms.getValue(XK_F4).convert()
        keyReleaseEvent.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager)

        val shutdownValue = keyReleaseHandler.handleEvent(keyReleaseEvent)

        assertFalse(shutdownValue, "The window manager should not shut down on F4")

        val killClientCall = systemApi.functionCalls.removeAt(0)
        assertEquals("killClient", killClientCall.name, "The window should be killed if we can't get its protocols")
        assertEquals(windowId, killClientCall.parameters[0], "The active window needs to be killed")
    }
}
