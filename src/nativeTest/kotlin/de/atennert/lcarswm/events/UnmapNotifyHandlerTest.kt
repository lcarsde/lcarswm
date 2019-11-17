package de.atennert.lcarswm.events

import de.atennert.lcarswm.UIDrawingMock
import de.atennert.lcarswm.windowactions.WindowRegistrationMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.UnmapNotify
import xlib.Window
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 *
 */
class UnmapNotifyHandlerTest {
    @Test
    fun `return correct message type`() {
        val unmapNotifyHandler = UnmapNotifyHandler(WindowRegistrationMock(), UIDrawingMock())

        assertEquals(UnmapNotify, unmapNotifyHandler.xEventType, "UnmapNotifyHandler should have type UnmapNotify")
    }

    @Test
    fun `unregister managed window`() {
        val unmapWindowId: Window = 42.convert()
        val uiDrawingMock = UIDrawingMock()
        val windowRegistration = WindowRegistrationMock()
        windowRegistration.addWindow(unmapWindowId, false)

        val unmapNotifyHandler = UnmapNotifyHandler(windowRegistration, uiDrawingMock)

        val unmapEvent = nativeHeap.alloc<XEvent>()
        unmapEvent.type = UnmapNotify
        unmapEvent.xunmap.window = unmapWindowId

        windowRegistration.functionCalls.clear()

        val shutdownValue = unmapNotifyHandler.handleEvent(unmapEvent)

        val unregisterWindowCalls = windowRegistration.functionCalls

        assertFalse(shutdownValue, "The unmap handling shouldn't trigger a shutdown")

        val removeFromRegistryCall = unregisterWindowCalls.removeAt(0)
        assertEquals("removeWindow", removeFromRegistryCall.name, "The window needs to be _removed_ from the registry")
        assertEquals(unmapWindowId, removeFromRegistryCall.parameters[0], "The _window_ needs to be removed from the registry")

        assertEquals(1, uiDrawingMock.drawWindowManagerFrameCallCount, "We need to redraw the root window UI on unmapping")
    }
}