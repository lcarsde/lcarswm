package de.atennert.lcarswm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the WindowManagerState class
 */
class WindowManagerStateTest {
    @Test
    fun `new added windows become active`() {
        val windowManagerState = WindowManagerState(0.toUInt()) {1.toUInt()}
        val monitor = Monitor(1.toUInt(), "name")
        val window1 = Window(1.toUInt())
        val window2 = Window(2.toUInt())

        windowManagerState.updateMonitors(listOf(monitor)) { _, _ -> }

        windowManagerState.addWindow(window1)
        assertEquals(windowManagerState.activeWindow, window1, "active window is not window1")

        windowManagerState.addWindow(window2)
        assertEquals(windowManagerState.activeWindow, window2, "active window is not window2")
    }

    @Test
    fun `test full lifecycle with one window`() {
        val monitor1 = Monitor(1.toUInt(), "name")
        val monitor2 = Monitor(2.toUInt(), "name")

        val windowManagerState = WindowManagerState(0.toUInt()) {1.toUInt()}

        var receivedWindowId: UInt? = null
        val windowUpdateFcn: Function2<List<Int>, UInt, Unit> = {_, windowId -> receivedWindowId = windowId}

        windowManagerState.updateMonitors(listOf(monitor1, monitor2), windowUpdateFcn)

        // nothing to configure
        assertNull(receivedWindowId)

        // no window registered yet
        assertNull(windowManagerState.getWindowMonitor(1.toUInt()))

        // windows are added to the first monitor
        val windowMonitor = windowManagerState.addWindow(Window(1.toUInt()))
        assertEquals(windowMonitor, monitor1)

        // we can now get the monitor with getWindowMonitor
        assertEquals(windowManagerState.getWindowMonitor(1.toUInt()), monitor1)

        // let's update the monitors
        windowManagerState.updateMonitors(listOf(monitor1, monitor2), windowUpdateFcn)

        // the known windows will be updated
        assertEquals(receivedWindowId, 1.toUInt())

        // let's remove the window
        windowManagerState.removeWindow(1.toUInt())

        // no window registered anymore
        assertNull(windowManagerState.getWindowMonitor(1.toUInt()))
    }

    @Test
    fun `move window up monitor list`() {
        val windowManagerState = WindowManagerState(0.toUInt()) { 1.toUInt() }
        val monitor1 = Monitor(1.toUInt(), "name1")
        val monitor2 = Monitor(2.toUInt(), "name2")
        val monitor3 = Monitor(3.toUInt(), "name3")
        val window1 = Window(1.toUInt())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2, monitor3)) { _, _ -> }

        windowManagerState.addWindow(window1)

        // wrap around
        assertEquals(windowManagerState.moveWindowToPreviousMonitor(window1), monitor3, "Unable to move window to 3rd monitor")

        assertEquals(windowManagerState.moveWindowToPreviousMonitor(window1), monitor2, "Unable to move window to 2nd monitor")
        assertEquals(windowManagerState.moveWindowToPreviousMonitor(window1), monitor1, "Unable to move window to 1st monitor")
    }

    @Test
    fun `move window down monitor list`() {
        val windowManagerState = WindowManagerState(0.toUInt()) { 1.toUInt() }
        val monitor1 = Monitor(1.toUInt(), "name1")
        val monitor2 = Monitor(2.toUInt(), "name2")
        val monitor3 = Monitor(3.toUInt(), "name3")
        val window1 = Window(1.toUInt())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2, monitor3)) { _, _ -> }

        windowManagerState.addWindow(window1)

        assertEquals(windowManagerState.moveWindowToNextMonitor(window1), monitor2, "Unable to move window to 2nd monitor")
        assertEquals(windowManagerState.moveWindowToNextMonitor(window1), monitor3, "Unable to move window to 3rd monitor")

        // wrap around
        assertEquals(windowManagerState.moveWindowToNextMonitor(window1), monitor1, "Unable to move window to 1st monitor")
    }

    @Test
    fun `toggle active window`() {
        val windowManagerState = WindowManagerState(0.toUInt()) { 1.toUInt() }
        val monitor = Monitor(1.toUInt(), "name")
        val window1 = Window(1.toUInt())
        val window2 = Window(2.toUInt())

        windowManagerState.updateMonitors(listOf(monitor)) { _, _ -> }

        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)

        assertEquals(windowManagerState.toggleActiveWindow(), window1, "Unable to activate first window")
        assertEquals(windowManagerState.toggleActiveWindow(), window2, "Unable to activate second window")

        // wrap around
        assertEquals(windowManagerState.toggleActiveWindow(), window1, "Unable to wrap around")
    }

    @Test
    fun `toggle away from removed window`() {
        val windowManagerState = WindowManagerState(0.toUInt()) { 1.toUInt() }
        val monitor = Monitor(1.toUInt(), "name")
        val window1 = Window(1.toUInt())
        val window2 = Window(2.toUInt())

        windowManagerState.updateMonitors(listOf(monitor)) { _, _ -> }

        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)

        assertEquals(windowManagerState.toggleActiveWindow(), window1, "Unable to activate first window")
        assertEquals(windowManagerState.toggleActiveWindow(), window2, "Unable to activate second window")

        windowManagerState.removeWindow(window2.id)

        assertEquals(windowManagerState.toggleActiveWindow(), window1, "Unable to activate window 1 after removal of window 2")
    }

    @Test
    fun `toggle between windows on different monitors`() {
        val windowManagerState = WindowManagerState(0.toUInt()) { 1.toUInt() }
        val monitor1 = Monitor(1.toUInt(), "name1")
        val monitor2 = Monitor(2.toUInt(), "name2")
        val window1 = Window(1.toUInt())
        val window2 = Window(2.toUInt())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2)) {_, _ ->}
        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)
        windowManagerState.moveWindowToNextMonitor(window2)

        assertEquals(windowManagerState.toggleActiveWindow(), window1) // monitor 1
        assertEquals(windowManagerState.toggleActiveWindow(), window2) // monitor 2
    }

    @Test
    fun `toggle returns null when there are no windows`() {
        val windowManagerState = WindowManagerState(0.toUInt()) { 1.toUInt() }
        val monitor = Monitor(1.toUInt(), "name")

        windowManagerState.updateMonitors(listOf(monitor)) {_, _ ->}

        assertNull(windowManagerState.toggleActiveWindow())
    }
}