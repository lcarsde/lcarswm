package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.UIDrawingMock
import de.atennert.lcarswm.system.SystemFacadeMock
import de.atennert.lcarswm.windowactions.WindowCoordinatorMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.KeyPress
import xlib.XEvent
import xlib.XK_Up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KeyPressHandlerTest {
    @Test
    fun `return the event type KeyPressHandler`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val uiDrawing = UIDrawingMock()
        val keyPressHandler = KeyPressHandler(keyManager, windowCoordinator, uiDrawing)

        assertEquals(KeyPress, keyPressHandler.xEventType, "The key press handler should have the correct type")
    }

    @Test
    fun `move active window to the next monitor`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val uiDrawer = UIDrawingMock()
        keyManager.grabInputControls()

        val keyPressHandler = KeyPressHandler(keyManager, windowCoordinator, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = systemApi.keySyms.getValue(XK_Up).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the up-key shouldn't trigger a shutdown")

        // TODO redraw active window

        val uiRedrawCall = uiDrawer.functionCalls.removeAt(0)
        assertEquals("drawWindowManagerFrame", uiRedrawCall.name, "The WM UI needs to be redrawn")
    }
}