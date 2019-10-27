package de.atennert.lcarswm.events

import de.atennert.lcarswm.Monitor
import de.atennert.lcarswm.WindowContainer
import de.atennert.lcarswm.WindowManagerStateMock
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.Window
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HandleDestroyNotifyTest {
    @Test
    fun `remove window on destroy notify`() {
        val windowId: Window = 1.convert()

        val destroyNotifyEvent = nativeHeap.alloc<XEvent>()
        destroyNotifyEvent.xdestroywindow.window = windowId

        val windowManagerState = WindowManagerStateTestImpl(windowId)
        val system = SystemFacadeMock()

        val requestShutdown = handleDestroyNotify(system, LoggerMock(), windowManagerState, destroyNotifyEvent)

        assertFalse(requestShutdown, "Destroy handling should not request shutdown of the window manager")
        assertEquals(1, windowManagerState.removedWindowIds.size, "There wasn't exactly one window removal")
        assertEquals(windowId, windowManagerState.removedWindowIds[0], "Window was not removed from state management")

        assertEquals("unmapWindow", system.functionCalls.removeAt(0).name, "The frame needs to be unmapped")
        assertEquals("removeFromSaveSet", system.functionCalls.removeAt(0).name, "the window needs to be removed from the save set")
        assertEquals("destroyWindow", system.functionCalls.removeAt(0).name, "The frame needs to be destroyed")
    }

    @Test
    fun `don't remove unknown window`() {
        val windowId: Window = 1.convert()

        val destroyNotifyEvent = nativeHeap.alloc<XEvent>()
        destroyNotifyEvent.xdestroywindow.window = windowId

        val windowManagerState = WindowManagerStateTestImpl(0.convert())
        val system = SystemFacadeMock()

        val requestShutdown = handleDestroyNotify(system, LoggerMock(), windowManagerState, destroyNotifyEvent)

        assertFalse(requestShutdown, "Destroy handling should not request shutdown of the window manager")
        assertTrue(windowManagerState.removedWindowIds.isEmpty(), "an unknown window shouldn't be removed")
        assertTrue(system.functionCalls.isEmpty(), "There should be no calls to system, as nothing was to do")
    }

    private class WindowManagerStateTestImpl(private val knownWindow: Window) : WindowManagerStateMock() {
        val removedWindowIds = mutableListOf<Window>()

        override val windows = listOf(Pair(WindowContainer(knownWindow), Monitor(0.convert(), "", true)))

        override fun removeWindow(windowId: Window) {
            this.removedWindowIds.add(windowId)
        }

        override fun hasWindow(windowId: Window): Boolean = windowId == knownWindow
    }
}
