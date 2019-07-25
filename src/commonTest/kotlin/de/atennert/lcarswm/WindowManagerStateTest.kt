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
        val windowMonitor = windowManagerState.addWindow(1.toUInt(), Window(1.toUInt()))
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
}