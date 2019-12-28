package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.conversion.toUByteArray
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
        val atomLibrary = AtomLibrary(systemApi)

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager, atomLibrary)

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
        val atomLibrary = AtomLibrary(systemApi)
        keyManager.grabInputControls()

        systemApi.functionCalls.clear()

        val keyReleaseEvent = nativeHeap.alloc<XEvent>()
        keyReleaseEvent.type = KeyRelease
        keyReleaseEvent.xkey.keycode = systemApi.keySyms.getValue(XK_Q).convert()
        keyReleaseEvent.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager, atomLibrary)

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
        val atomLibrary = AtomLibrary(systemApi)
        focusHandler.setFocusedWindow(windowId)
        keyManager.grabInputControls()

        systemApi.functionCalls.clear()

        val keyReleaseEvent = nativeHeap.alloc<XEvent>()
        keyReleaseEvent.type = KeyRelease
        keyReleaseEvent.xkey.keycode = systemApi.keySyms.getValue(XK_F4).convert()
        keyReleaseEvent.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager, atomLibrary)

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
        val atomLibrary = AtomLibrary(systemApi)
        focusHandler.setFocusedWindow(windowId)
        keyManager.grabInputControls()

        systemApi.functionCalls.clear()

        val keyReleaseEvent = nativeHeap.alloc<XEvent>()
        keyReleaseEvent.type = KeyRelease
        keyReleaseEvent.xkey.keycode = systemApi.keySyms.getValue(XK_F4).convert()
        keyReleaseEvent.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager, atomLibrary)

        val shutdownValue = keyReleaseHandler.handleEvent(keyReleaseEvent)

        assertFalse(shutdownValue, "The window manager should not shut down on F4")

        val killClientCall = systemApi.functionCalls.removeAt(0)
        assertEquals("killClient", killClientCall.name, "The window should be killed if we can't get its protocols")
        assertEquals(windowId, killClientCall.parameters[0], "The active window needs to be killed")
    }

    @Test
    fun `send delete window event on F4 when client supports it`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val focusHandler = WindowFocusHandler()
        val windowId = systemApi.getNewWindowId()
        val atomLibrary = AtomLibrary(systemApi)
        focusHandler.setFocusedWindow(windowId)
        keyManager.grabInputControls()

        systemApi.functionCalls.clear()

        val keyReleaseEvent = nativeHeap.alloc<XEvent>()
        keyReleaseEvent.type = KeyRelease
        keyReleaseEvent.xkey.keycode = systemApi.keySyms.getValue(XK_F4).convert()
        keyReleaseEvent.xkey.state = systemApi.modifiers[systemApi.winModifierPosition].convert()

        val keyReleaseHandler = KeyReleaseHandler(systemApi, focusHandler, keyManager, atomLibrary)

        val shutdownValue = keyReleaseHandler.handleEvent(keyReleaseEvent)

        assertFalse(shutdownValue, "The window manager should not shut down on F4")

        val xClientCall = systemApi.functionCalls.removeAt(0)
        assertEquals("sendEvent", xClientCall.name, "The WM should send a delete window request")
        assertEquals(windowId, xClientCall.parameters[0], "The delete request should be for the right window")
        assertFalse(xClientCall.parameters[1] as Boolean, "The event should not propagate")
        assertEquals(0.toLong(), xClientCall.parameters[2], "The mask should not contain anything")

        val eventData = (xClientCall.parameters[3] as CPointer<XEvent>).pointed
        assertEquals(ClientMessage, eventData.xclient.type, "The message type should be client data")
        assertEquals(windowId, eventData.xclient.window, "The window data should match")
        assertEquals(atomLibrary[Atoms.WM_PROTOCOLS], eventData.xclient.message_type, "The message type should be WM_PROTOCOLS")
        assertEquals(32, eventData.xclient.format, "The message format should be 32 bit")
        assertEquals(atomLibrary[Atoms.WM_DELETE_WINDOW], eventData.xclient.data.l[0].convert(), "The delete WM window atom needs to match")
    }
}
