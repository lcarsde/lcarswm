package de.atennert.lcarswm.windowactions

import kotlinx.cinterop.convert
import xlib.Window
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowFocusHandlerTest {
    @Test
    fun `check that initially there is no focused window`() {
        val windowFocusHandler = WindowFocusHandler()

        assertNull(windowFocusHandler.getFocusedWindow(), "There is no focused window")

        var activeWindow: Window? = 42.convert()
        windowFocusHandler.registerObserver {activeWindow = it}
        assertNull(activeWindow, "The observer should get null window")
    }

    @Test
    fun `update focused window`() {
        val windowFocusHandler = WindowFocusHandler()
        val testWindow = 21.convert<Window>()
        var activeWindow: Window? = 42.convert()

        windowFocusHandler.registerObserver {activeWindow = it}

        windowFocusHandler.setFocusedWindow(testWindow)

        assertEquals(testWindow, windowFocusHandler.getFocusedWindow(), "The focused window should be updated")

        assertEquals(testWindow, activeWindow, "The observer should get the updated window")
    }

    @Test
    fun `remove last focused window`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()

        focusHandler.setFocusedWindow(window1)

        focusHandler.removeWindow(window1)

        assertNull(focusHandler.getFocusedWindow(), "The focused window should be removed")
    }
}
