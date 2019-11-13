package de.atennert.lcarswm.events

import de.atennert.lcarswm.system.SystemFacadeMock
import de.atennert.lcarswm.windowactions.WindowRegistrationMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.UnmapNotify
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
        val unmapNotifyHandler = UnmapNotifyHandler(SystemFacadeMock(), WindowRegistrationMock(), 1.convert())

        assertEquals(UnmapNotify, unmapNotifyHandler.xEventType, "UnmapNotifyHandler should have type UnmapNotify")
    }

    @Test
    fun `unregister managed window`() {
        val systemApi = SystemFacadeMock()
        val rootWindowId = systemApi.rootWindowId
        val unmapWindowId = systemApi.getNewWindowId()
        val windowRegistration = WindowRegistrationMock()
        windowRegistration.addWindow(unmapWindowId, false)

        val unmapNotifyHandler = UnmapNotifyHandler(systemApi, windowRegistration, rootWindowId)

        val unmapEvent = nativeHeap.alloc<XEvent>()
        unmapEvent.type = UnmapNotify
        unmapEvent.xunmap.window = unmapWindowId

        windowRegistration.functionCalls.clear()

        val shutdownValue = unmapNotifyHandler.handleEvent(unmapEvent)

        val unregisterCalls = systemApi.functionCalls

        assertFalse(shutdownValue, "The unmap handling shouldn't trigger a shutdown")

        val reparentCall = unregisterCalls.removeAt(0)
        assertEquals("reparentWindow", reparentCall.name, "We need to _reparent_ the window back to root")
        assertEquals(unmapWindowId, reparentCall.parameters[0], "We need to reparent the _window_ back to root")
        assertEquals(rootWindowId, reparentCall.parameters[1], "We need to reparent the window back to _root_")

        val removeFromSaveSetCall = unregisterCalls.removeAt(0)
        assertEquals("removeFromSaveSet", removeFromSaveSetCall.name, "We need to _remove_ the window from the save set")
        assertEquals(unmapWindowId, removeFromSaveSetCall.parameters[0], "We need to remove the _window_ from the save set")
    }
}