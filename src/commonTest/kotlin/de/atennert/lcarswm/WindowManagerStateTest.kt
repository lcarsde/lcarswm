package de.atennert.lcarswm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the WindowManagerState class
 */
class WindowManagerStateTest {
    @Test
    fun `new added windows become active`() {
        val windowManagerState = WindowManagerState {1.toULong()}
        val monitor = Monitor(1.toULong(), "name", false)
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

        windowManagerState.updateMonitors(listOf(monitor)) { _, _ -> }

        windowManagerState.addWindow(window1)
        assertEquals(windowManagerState.activeWindow, window1, "active window is not window1")

        windowManagerState.addWindow(window2)
        assertEquals(windowManagerState.activeWindow, window2, "active window is not window2")
    }

    @Test
    fun `test full lifecycle with one window`() {
        val monitor1 = Monitor(1.toULong(), "name", false)
        val monitor2 = Monitor(2.toULong(), "name", false)

        val windowManagerState = WindowManagerState {1.toULong()}

        var receivedWindowId: ULong? = null
        val windowUpdateFcn: Function2<List<Int>, Window, Unit> = {_, window -> receivedWindowId = window.id}

        windowManagerState.updateMonitors(listOf(monitor1, monitor2), windowUpdateFcn)

        // nothing to configure
        assertNull(receivedWindowId)

        // no window registered yet
        assertNull(windowManagerState.getWindowMonitor(1.toULong()))

        // windows are added to the first monitor
        val windowMonitor = windowManagerState.addWindow(Window(1.toULong()))
        assertEquals(windowMonitor, monitor1)

        // we can now get the monitor with getWindowMonitor
        assertEquals(windowManagerState.getWindowMonitor(1.toULong()), monitor1)

        // let's update the monitors
        windowManagerState.updateMonitors(listOf(monitor1, monitor2), windowUpdateFcn)

        // the known windows will be updated
        assertEquals(receivedWindowId, 1.toULong())

        // let's remove the window
        windowManagerState.removeWindow(1.toULong())

        // no window registered anymore
        assertNull(windowManagerState.getWindowMonitor(1.toULong()))
    }

    @Test
    fun `move window up monitor list`() {
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor1 = Monitor(1.toULong(), "name1", false)
        val monitor2 = Monitor(2.toULong(), "name2", false)
        val monitor3 = Monitor(3.toULong(), "name3", false)
        val window1 = Window(1.toULong())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2, monitor3)) { _, _ -> }

        windowManagerState.addWindow(window1)

        // wrap around
        assertEquals(windowManagerState.moveWindowToPreviousMonitor(window1), monitor3, "Unable to move window to 3rd monitor")

        assertEquals(windowManagerState.moveWindowToPreviousMonitor(window1), monitor2, "Unable to move window to 2nd monitor")
        assertEquals(windowManagerState.moveWindowToPreviousMonitor(window1), monitor1, "Unable to move window to 1st monitor")
    }

    @Test
    fun `move window down monitor list`() {
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor1 = Monitor(1.toULong(), "name1", false)
        val monitor2 = Monitor(2.toULong(), "name2", false)
        val monitor3 = Monitor(3.toULong(), "name3", false)
        val window1 = Window(1.toULong())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2, monitor3)) { _, _ -> }

        windowManagerState.addWindow(window1)

        assertEquals(windowManagerState.moveWindowToNextMonitor(window1), monitor2, "Unable to move window to 2nd monitor")
        assertEquals(windowManagerState.moveWindowToNextMonitor(window1), monitor3, "Unable to move window to 3rd monitor")

        // wrap around
        assertEquals(windowManagerState.moveWindowToNextMonitor(window1), monitor1, "Unable to move window to 1st monitor")
    }

    @Test
    fun `toggle active window`() {
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor = Monitor(1.toULong(), "name", false)
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

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
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor = Monitor(1.toULong(), "name", false)
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

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
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor1 = Monitor(1.toULong(), "name1", false)
        val monitor2 = Monitor(2.toULong(), "name2", false)
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2)) {_, _ ->}
        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)
        windowManagerState.moveWindowToNextMonitor(window2)

        assertEquals(windowManagerState.toggleActiveWindow(), window1) // monitor 1
        assertEquals(windowManagerState.toggleActiveWindow(), window2) // monitor 2
    }

    @Test
    fun `toggle returns null when there are no windows`() {
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor = Monitor(1.toULong(), "name", false)

        windowManagerState.updateMonitors(listOf(monitor)) {_, _ ->}

        assertNull(windowManagerState.toggleActiveWindow())
    }

    @Test
    fun `windows stay in the same monitor on monitor update`() {
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor1 = Monitor(1.toULong(), "name1", false)
        val monitor2 = Monitor(2.toULong(), "name2", false)
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2)) {_, _ ->}
        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)
        windowManagerState.moveWindowToNextMonitor(window2)

        windowManagerState.updateMonitors(listOf(monitor1, monitor2)) {_, _ ->}

        assertEquals(windowManagerState.getWindowMonitor(window1.id), monitor1)
        assertEquals(windowManagerState.getWindowMonitor(window2.id), monitor2)
    }

    @Test
    fun `windows move to monitor 1 when their monitor is removed`() {
        val windowManagerState = WindowManagerState { 1.toULong() }
        val monitor1 = Monitor(1.toULong(), "name1", false)
        val monitor2 = Monitor(2.toULong(), "name2", false)
        val monitor3 = Monitor(3.toULong(), "name3", false)
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

        windowManagerState.updateMonitors(listOf(monitor1, monitor2, monitor3)) {_, _ ->}
        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)
        windowManagerState.moveWindowToNextMonitor(window2)
        windowManagerState.moveWindowToNextMonitor(window2)

        windowManagerState.updateMonitors(listOf(monitor1, monitor2)) {_, _ ->}

        assertEquals(windowManagerState.getWindowMonitor(window1.id), monitor1)
        assertEquals(windowManagerState.getWindowMonitor(window2.id), monitor1)
    }

    @Test
    fun `get normal screen mode for primary monitor`() {
        val monitor = Monitor(1.toULong(), "name1", true)
        val windowManagerState = WindowManagerState { 1.toULong() }

        assertEquals(ScreenMode.NORMAL, windowManagerState.getScreenModeForMonitor(monitor))
    }

    @Test
    fun `get maximized screen mode for all monitors`() {
        val monitor1 = Monitor(1.toULong(), "name1", true)
        val monitor2 = Monitor(2.toULong(), "name2", false)
        val windowManagerState = WindowManagerState { 1.toULong() }
        windowManagerState.updateScreenMode(ScreenMode.MAXIMIZED) { _, _ ->}

        assertEquals(ScreenMode.MAXIMIZED, windowManagerState.getScreenModeForMonitor(monitor1))
        assertEquals(ScreenMode.MAXIMIZED, windowManagerState.getScreenModeForMonitor(monitor2))
    }

    @Test
    fun `get fullscreen screen mode for all monitors`() {
        val monitor1 = Monitor(1.toULong(), "name1", true)
        val monitor2 = Monitor(2.toULong(), "name2", false)
        val windowManagerState = WindowManagerState { 1.toULong() }
        windowManagerState.updateScreenMode(ScreenMode.FULLSCREEN) { _, _ ->}

        assertEquals(ScreenMode.FULLSCREEN, windowManagerState.getScreenModeForMonitor(monitor1))
        assertEquals(ScreenMode.FULLSCREEN, windowManagerState.getScreenModeForMonitor(monitor2))
    }

    @Test
    fun `get maximized screen mode for non-primary monitors in normal mode`() {
        val monitor = Monitor(1.toULong(), "name", false)
        val windowManagerState = WindowManagerState { 1.toULong() }

        assertEquals(ScreenMode.MAXIMIZED, windowManagerState.getScreenModeForMonitor(monitor))
    }

    @Test
    fun `get update with null for active window when no window is registered`() {
        val windowManagerState = WindowManagerState { 1.toULong() }

        var activeWindow: Window? = null
        var activeWindowSet = false
        windowManagerState.setActiveWindowListener {activeWindowSet = true; activeWindow = it}

        assertTrue(activeWindowSet, "The active window listener wasn't called")
        assertNull(activeWindow, "The active window should be null but wasn't")
    }

    @Test
    fun `get update for active window when a window is added`() {
        val monitor = Monitor(1.toULong(), "name", false)
        val windowManagerState = WindowManagerState { 1.toULong() }
        val window = Window(1.toULong())

        windowManagerState.updateMonitors(listOf(monitor)) { _, _ -> }

        var activeWindow: Window? = null
        windowManagerState.setActiveWindowListener {activeWindow = it}

        windowManagerState.addWindow(window)

        assertEquals(activeWindow, window, "No update for active window received")
    }

    @Test
    fun `get update for active window when a window is removed`() {
        val monitor = Monitor(1.toULong(), "name", false)
        val windowManagerState = WindowManagerState { 1.toULong() }
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

        windowManagerState.updateMonitors(listOf(monitor)) { _, _ -> }

        var activeWindow: Window? = null
        windowManagerState.setActiveWindowListener {activeWindow = it}

        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)

        windowManagerState.removeWindow(window2.id)

        assertEquals(activeWindow, window1, "No update for active window received")
    }

    @Test
    fun `get update for active window when the active window is toggled`() {
        val monitor = Monitor(1.toULong(), "name", false)
        val windowManagerState = WindowManagerState { 1.toULong() }
        val window1 = Window(1.toULong())
        val window2 = Window(2.toULong())

        windowManagerState.updateMonitors(listOf(monitor)) { _, _ -> }

        var activeWindow: Window? = null
        windowManagerState.setActiveWindowListener {activeWindow = it}

        windowManagerState.addWindow(window1)
        windowManagerState.addWindow(window2)

        windowManagerState.toggleActiveWindow()

        assertEquals(activeWindow, window1, "No update for active window received")
    }
}