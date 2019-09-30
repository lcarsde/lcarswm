package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateMock
import de.atennert.lcarswm.log.LoggerMock
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
        val windowId = 1.toULong()

        val destroyNotifyEvent = nativeHeap.alloc<XEvent>()
        destroyNotifyEvent.xdestroywindow.window = windowId

        val windowManagerState = WindowManagerStateTestImpl(windowId)

        val requestShutdown = handleDestroyNotify(LoggerMock(), windowManagerState, destroyNotifyEvent)

        assertFalse(requestShutdown, "Destroy handling should not request shutdown of the window manager")
        assertEquals(1, windowManagerState.removedWindowIds.size, "There wasn't exactly one window removal")
        assertEquals(windowId, windowManagerState.removedWindowIds[0], "Window was not removed from state management")
    }

    @Test
    fun `don't remove unknown window`() {
        val windowId = 1.toULong()

        val destroyNotifyEvent = nativeHeap.alloc<XEvent>()
        destroyNotifyEvent.xdestroywindow.window = windowId

        val windowManagerState = WindowManagerStateTestImpl(0.convert())

        val requestShutdown = handleDestroyNotify(LoggerMock(), windowManagerState, destroyNotifyEvent)

        assertFalse(requestShutdown, "Destroy handling should not request shutdown of the window manager")
        assertTrue(windowManagerState.removedWindowIds.isEmpty(), "an unknown window shouldn't be removed")
    }

    private class WindowManagerStateTestImpl(private val knownWindow: Window) : WindowManagerStateMock() {
        val removedWindowIds = mutableListOf<Window>()

        override fun removeWindow(windowId: Window) {
            this.removedWindowIds.add(windowId)
        }

        override fun hasWindow(windowId: Window): Boolean = windowId == knownWindow
    }
}
