package de.atennert.lcarswm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the WindowManagerState class
 */
class WindowManagerStateTest {
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
    fun `move up monitor list`() {
        // TODO
    }

    @Test
    fun `move down monitor list`() {
        // TODO
    }

    @Test
    fun `toggle active window`() {
        val windowManagerState = WindowManagerState(0.toUInt()) { 1.toUInt() }
        val monitor = Monitor(1.toUInt(), "name")
        val window1 = Window(1.toUInt())
        val window2 = Window(2.toUInt())
        val windowUpdateFcn: Function2<List<Int>, UInt, Unit> = { _, _ -> }

        windowManagerState.updateMonitors(listOf(monitor), windowUpdateFcn)

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
        val windowUpdateFcn: Function2<List<Int>, UInt, Unit> = { _, _ -> }

        windowManagerState.updateMonitors(listOf(monitor), windowUpdateFcn)

        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)

        assertEquals(windowManagerState.toggleActiveWindow(), window1, "Unable to activate first window")
        assertEquals(windowManagerState.toggleActiveWindow(), window2, "Unable to activate second window")

        windowManagerState.removeWindow(window2.id)

        assertEquals(windowManagerState.toggleActiveWindow(), window1, "Unable to activate window 1 after removal of window 2")
    }
}