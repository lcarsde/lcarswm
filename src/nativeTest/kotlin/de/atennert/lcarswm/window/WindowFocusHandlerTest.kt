package de.atennert.lcarswm.window

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
        var oldWindow: Window? = 64.convert()
        windowFocusHandler.registerObserver { n, o, _ -> activeWindow = n; oldWindow = o}
        assertNull(activeWindow, "The observer should get null window")
        assertNull(oldWindow, "The observer should get no old window")
    }

    @Test
    fun `update focused window`() {
        val windowFocusHandler = WindowFocusHandler()
        val testWindow1 = 21.convert<Window>()
        val testWindow2 = 22.convert<Window>()
        var newWindow: Window? = 42.convert()
        var oldWindow: Window? = 64.convert()

        windowFocusHandler.registerObserver { n, o, _ -> newWindow = n; oldWindow = o}

        windowFocusHandler.setFocusedWindow(testWindow1)
        assertEquals(testWindow1, windowFocusHandler.getFocusedWindow(), "The focused window should be updated (1)")
        assertEquals(testWindow1, newWindow, "The observer should get the updated window (1)")
        assertNull(oldWindow, "The observer should get the old window (1)")

        windowFocusHandler.setFocusedWindow(testWindow2)
        assertEquals(testWindow2, windowFocusHandler.getFocusedWindow(), "The focused window should be updated (2)")
        assertEquals(testWindow2, newWindow, "The observer should get the updated window (2)")
        assertEquals(testWindow1, oldWindow, "The observer should get the old window (2)")
    }

    @Test
    fun `remove last focused window`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()

        focusHandler.setFocusedWindow(window1)

        focusHandler.removeWindow(window1)

        assertNull(focusHandler.getFocusedWindow(), "The focused window should be removed")
    }

    @Test
    fun `remove second focused window`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)

        focusHandler.removeWindow(window2)

        assertEquals(window1, focusHandler.getFocusedWindow(), "The fallback should be another focusable window")
    }

    @Test
    fun `remove third focused window`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)

        // remove currently focused window
        focusHandler.removeWindow(window3)

        // the last focused window should be focused
        assertEquals(window2, focusHandler.getFocusedWindow(), "The fallback should be the last focused window")
    }

    @Test
    fun `remove unfocused window`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)

        focusHandler.removeWindow(window2)

        assertEquals(window3, focusHandler.getFocusedWindow(), "Don't unfocus the focused window")
    }

    @Test
    fun `toggle through windows`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)

        focusHandler.toggleWindowFocusForward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle to window 2")

        focusHandler.toggleWindowFocusForward()
        assertEquals(window1, focusHandler.getFocusedWindow(), "The focus should toggle to window 1")

        focusHandler.toggleWindowFocusForward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle to window 3")
    }

    @Test
    fun `toggle through windows with reset`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)

        focusHandler.toggleWindowFocusForward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle to window 2")

        focusHandler.keySessionListener.stopSession()
        focusHandler.toggleWindowFocusForward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle to window 3")

        focusHandler.keySessionListener.stopSession()
        focusHandler.toggleWindowFocusForward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle back to window 2")
    }

    @Test
    fun `toggle through windows reversed`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window1, focusHandler.getFocusedWindow(), "The focus should toggle to window 1")

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle to window 2")

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle to window 3")
    }

    @Test
    fun `toggle through windows reversed with reset`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window1, focusHandler.getFocusedWindow(), "The focus should toggle to window 1")

        focusHandler.keySessionListener.stopSession()
        focusHandler.toggleWindowFocusBackward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle to window 2")

        focusHandler.keySessionListener.stopSession()
        focusHandler.toggleWindowFocusBackward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle back to window 3")
    }

    @Test
    fun `toggle through windows mixed`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()
        val window4: Window = 4.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)
        focusHandler.setFocusedWindow(window4)

        focusHandler.toggleWindowFocusForward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle to window 3")

        focusHandler.toggleWindowFocusForward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle to window 2")

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle to window 3")

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window4, focusHandler.getFocusedWindow(), "The focus should toggle to window 4")

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window1, focusHandler.getFocusedWindow(), "The focus should toggle to window 1")

        focusHandler.toggleWindowFocusForward()
        assertEquals(window4, focusHandler.getFocusedWindow(), "The focus should toggle to window 4")
    }

    @Test
    fun `toggle through windows mixed with reset`() {
        val focusHandler = WindowFocusHandler()
        val window1: Window = 1.convert()
        val window2: Window = 2.convert()
        val window3: Window = 3.convert()
        val window4: Window = 4.convert()

        focusHandler.setFocusedWindow(window1)
        focusHandler.setFocusedWindow(window2)
        focusHandler.setFocusedWindow(window3)
        focusHandler.setFocusedWindow(window4)

        focusHandler.toggleWindowFocusForward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle to window 3")

        focusHandler.toggleWindowFocusForward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle to window 2")

        focusHandler.keySessionListener.stopSession()
        focusHandler.toggleWindowFocusForward()
        assertEquals(window4, focusHandler.getFocusedWindow(), "The focus should toggle to window 2")

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window2, focusHandler.getFocusedWindow(), "The focus should toggle to window 1")

        focusHandler.toggleWindowFocusBackward()
        assertEquals(window1, focusHandler.getFocusedWindow(), "The focus should toggle to window 1")

        focusHandler.keySessionListener.stopSession()
        focusHandler.toggleWindowFocusBackward()
        assertEquals(window3, focusHandler.getFocusedWindow(), "The focus should toggle back to window 3")
    }

    @Test
    fun `don't react on toggle without windows`() {
        val focusHandler = WindowFocusHandler()

        focusHandler.toggleWindowFocusForward()

        assertNull(focusHandler.getFocusedWindow(), "There is no focusable window")
    }
}
