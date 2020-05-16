package de.atennert.lcarswm.events

import de.atennert.lcarswm.drawing.UIDrawingMock
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.window.WindowRegistrationMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.UnmapNotify
import xlib.Window
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 *
 */
class UnmapNotifyHandlerTest {
    @Test
    fun `return correct message type`() {
        val unmapNotifyHandler = UnmapNotifyHandler(LoggerMock(), WindowRegistrationMock(),
            UIDrawingMock()
        )

        assertEquals(UnmapNotify, unmapNotifyHandler.xEventType, "UnmapNotifyHandler should have type UnmapNotify")
    }

    @Test
    fun `unregister managed window`() {
        val unmapWindowId: Window = 42.convert()
        val uiDrawingMock = UIDrawingMock()
        val windowRegistration = WindowRegistrationMock()

        val unmapNotifyHandler = UnmapNotifyHandler(LoggerMock(), windowRegistration, uiDrawingMock)

        val unmapEvent = nativeHeap.alloc<XEvent>()
        unmapEvent.type = UnmapNotify
        unmapEvent.xunmap.window = unmapWindowId

        // make window known
        windowRegistration.addWindow(unmapWindowId, false)
        windowRegistration.functionCalls.clear()

        val shutdownValue = unmapNotifyHandler.handleEvent(unmapEvent)

        val unregisterWindowCalls = windowRegistration.functionCalls

        assertFalse(shutdownValue, "The unmap handling shouldn't trigger a shutdown")

        val removeFromRegistryCall = unregisterWindowCalls.removeAt(0)
        assertEquals("removeWindow", removeFromRegistryCall.name, "The window needs to be _removed_ from the registry")
        assertEquals(unmapWindowId, removeFromRegistryCall.parameters[0], "The _window_ needs to be removed from the registry")

        val redrawUiCall = uiDrawingMock.functionCalls.removeAt(0)
        assertEquals("drawWindowManagerFrame", redrawUiCall.name, "We need to redraw the root window UI on unmapping")
    }

    @Test
    fun `handle unmanaged window`() {
        val unmapWindowId: Window = 42.convert()
        val uiDrawingMock = UIDrawingMock()
        val windowRegistration = WindowRegistrationMock()

        val unmapNotifyHandler = UnmapNotifyHandler(LoggerMock(), windowRegistration, uiDrawingMock)

        val unmapEvent = nativeHeap.alloc<XEvent>()
        unmapEvent.type = UnmapNotify
        unmapEvent.xunmap.window = unmapWindowId

        windowRegistration.functionCalls.clear()

        val shutdownValue = unmapNotifyHandler.handleEvent(unmapEvent)

        val unregisterWindowCalls = windowRegistration.functionCalls

        assertFalse(shutdownValue, "The unmap handling shouldn't trigger a shutdown")

        assertTrue(unregisterWindowCalls.isEmpty(), "There should be calls to the window registration for an unknown window")

        val redrawUiCall = uiDrawingMock.functionCalls.removeAt(0)
        assertEquals("drawWindowManagerFrame", redrawUiCall.name, "We need to redraw the root window UI removing an unknown window")
    }
}