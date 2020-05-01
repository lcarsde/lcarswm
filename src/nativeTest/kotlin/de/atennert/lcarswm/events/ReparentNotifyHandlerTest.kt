package de.atennert.lcarswm.events

import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import de.atennert.lcarswm.window.WindowRegistrationMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import xlib.ReparentNotify
import xlib.Window
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ReparentNotifyHandlerTest {
    @Test
    fun `handler should have correct type`() {
        assertEquals(ReparentNotify, ReparentNotifyHandler(LoggerMock(), WindowRegistrationMock()).xEventType, "The type should be ReparentNotify")
    }

    @Test
    fun `unmanage a client that is not reparented to its desired frame`() {
        val system = SystemFacadeMock()
        val windowId = system.getNewWindowId()
        val parentId = system.getNewWindowId()
        val windowHandler = object : WindowRegistrationMock() {
            override fun isWindowParentedBy(windowId: Window, parentId: Window): Boolean = false
        }

        val reparentEvent = nativeHeap.alloc<XEvent>()
        reparentEvent.type = ReparentNotify
        reparentEvent.xreparent.window = windowId
        reparentEvent.xreparent.parent = parentId

        val reparentNotifyHandler = ReparentNotifyHandler(LoggerMock(), windowHandler)

        val shutdownValue = reparentNotifyHandler.handleEvent(reparentEvent)

        assertFalse(shutdownValue, "ReparentNotify should not trigger a shutdown")

        val removeWindowCall = windowHandler.functionCalls.removeAt(0)
        assertEquals("removeWindow", removeWindowCall.name, "Remove a window with bad reparenting")
        assertEquals(windowId, removeWindowCall.parameters[0])
    }
}