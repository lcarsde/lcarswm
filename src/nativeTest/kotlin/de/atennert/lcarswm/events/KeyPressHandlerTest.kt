package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.Modifiers
import de.atennert.lcarswm.UIDrawingMock
import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.SystemFacadeMock
import de.atennert.lcarswm.windowactions.WindowCoordinatorMock
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyPressHandlerTest {
    @Test
    fun `return the event type KeyPressHandler`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawing = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawing)

        assertEquals(KeyPress, keyPressHandler.xEventType, "The key press handler should have the correct type")
    }

    @Test
    fun `move active window to the next monitor`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawer = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        keyManager.grabInternalKeys()
        windowFocusHandler.setFocusedWindow(systemApi.getNewWindowId())

        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = systemApi.keySyms.getValue(XK_Up).convert()
        keyPressEvent.xkey.state = keyManager.modMasks.getValue(Modifiers.SUPER).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the up-key shouldn't trigger a shutdown")

        val moveWindowCall = windowCoordinator.functionCalls.removeAt(0)
        assertEquals("moveWindowToNextMonitor", moveWindowCall.name, "The focused window should be moved to the next monitor")
        assertEquals(windowFocusHandler.getFocusedWindow(), moveWindowCall.parameters[0], "The _focused window_ should be moved to the next monitor")

        val uiRedrawCall = uiDrawer.functionCalls.removeAt(0)
        assertEquals("drawWindowManagerFrame", uiRedrawCall.name, "The WM UI needs to be redrawn")
    }

    @Test
    fun `move active window to the previous monitor`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawer = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        keyManager.grabInternalKeys()
        windowFocusHandler.setFocusedWindow(systemApi.getNewWindowId())

        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = systemApi.keySyms.getValue(XK_Down).convert()
        keyPressEvent.xkey.state = keyManager.modMasks.getValue(Modifiers.SUPER).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the up-key shouldn't trigger a shutdown")

        val moveWindowCall = windowCoordinator.functionCalls.removeAt(0)
        assertEquals("moveWindowToPreviousMonitor", moveWindowCall.name, "The focused window should be moved to the previous monitor")
        assertEquals(windowFocusHandler.getFocusedWindow(), moveWindowCall.parameters[0], "The _focused window_ should be moved to the previous monitor")

        val uiRedrawCall = uiDrawer.functionCalls.removeAt(0)
        assertEquals("drawWindowManagerFrame", uiRedrawCall.name, "The WM UI needs to be redrawn")
    }

    @Test
    fun `don't react on move to next monitor without a focusable window`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawer = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        keyManager.grabInternalKeys()

        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = systemApi.keySyms.getValue(XK_Up).convert()
        keyPressEvent.xkey.state = keyManager.modMasks.getValue(Modifiers.SUPER).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the up-key shouldn't trigger a shutdown")

        assertTrue(windowCoordinator.functionCalls.isEmpty(), "There should be no call to the window coordinator without focused window")

        assertTrue(uiDrawer.functionCalls.isEmpty(), "There should be no call to the UI drawer without focused window")
    }

    @Test
    fun `don't react on move to previous monitor without a focusable window`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawer = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        keyManager.grabInternalKeys()

        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = systemApi.keySyms.getValue(XK_Down).convert()
        keyPressEvent.xkey.state = keyManager.modMasks.getValue(Modifiers.SUPER).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the up-key shouldn't trigger a shutdown")

        assertTrue(windowCoordinator.functionCalls.isEmpty(), "There should be no call to the window coordinator without focused window")

        assertTrue(uiDrawer.functionCalls.isEmpty(), "There should be no call to the UI drawer without focused window")
    }

    @Test
    fun `toggle focused Window`() {
        val systemApi = object : SystemFacadeMock() {
            override fun keysymToKeycode(keySym: KeySym): KeyCode {
                if (keySym.convert<Int>() == XK_Tab) {
                    return 42.convert()
                }
                return super.keysymToKeycode(keySym)
            }
        }
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawer = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        val window1 = systemApi.getNewWindowId()
        val window2 = systemApi.getNewWindowId()
        val window3 = systemApi.getNewWindowId()
        keyManager.grabInternalKeys()
        windowFocusHandler.setFocusedWindow(window1)
        windowFocusHandler.setFocusedWindow(window2)
        windowFocusHandler.setFocusedWindow(window3)
        windowFocusHandler.setFocusedWindow(window1)

        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = 42.convert()
        keyPressEvent.xkey.state = keyManager.modMasks.getValue(Modifiers.SUPER).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the up-key shouldn't trigger a shutdown")
        val coordinatorCalls = windowCoordinator.functionCalls

        assertEquals(window2, windowFocusHandler.getFocusedWindow(), "The focused window needs to be updated")
        checkRestacking(coordinatorCalls.removeAt(0), window2)

        keyPressHandler.handleEvent(keyPressEvent)
        assertEquals(window3, windowFocusHandler.getFocusedWindow(), "The focused window needs to be updated")
        checkRestacking(coordinatorCalls.removeAt(0), window3)


        keyPressHandler.handleEvent(keyPressEvent)
        assertEquals(window1, windowFocusHandler.getFocusedWindow(), "The focused window needs to be updated")
        checkRestacking(coordinatorCalls.removeAt(0), window1)
    }

    private fun checkRestacking(restackCall: FunctionCall, window: Window) {
        assertEquals("stackWindowToTheTop", restackCall.name, "The window $window needs to be stacked to the top")
        assertEquals(window, restackCall.parameters[0], "The _window ${window}_ needs to be stacked to the top")
    }

    @Test
    fun `don't toggle without focusable windows`() {
        val systemApi = object : SystemFacadeMock() {
            override fun keysymToKeycode(keySym: KeySym): KeyCode {
                if (keySym.convert<Int>() == XK_Tab) {
                    return 42.convert()
                }
                return super.keysymToKeycode(keySym)
            }
        }
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawer = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        keyManager.grabInternalKeys()

        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = 42.convert()
        keyPressEvent.xkey.state = keyManager.modMasks.getValue(Modifiers.SUPER).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the up-key shouldn't trigger a shutdown")

        assertEquals(0, windowCoordinator.functionCalls.size, "There is no window to restack")
    }

    @Test
    fun `toggle screen mode on M`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        val windowCoordinator = WindowCoordinatorMock()
        val windowFocusHandler = WindowFocusHandler()
        val uiDrawer = UIDrawingMock()
        val monitorManager = MonitorManagerMock()
        keyManager.grabInternalKeys()
        windowFocusHandler.setFocusedWindow(systemApi.getNewWindowId())

        val keyPressHandler = KeyPressHandler(keyManager, monitorManager, windowCoordinator, windowFocusHandler, uiDrawer)

        val keyPressEvent = nativeHeap.alloc<XEvent>()
        keyPressEvent.type = KeyPress
        keyPressEvent.xkey.keycode = systemApi.keySyms.getValue(XK_M).convert()
        keyPressEvent.xkey.state = keyManager.modMasks.getValue(Modifiers.SUPER).convert()

        val shutdownValue = keyPressHandler.handleEvent(keyPressEvent)

        assertFalse(shutdownValue, "Handling the M-key shouldn't trigger a shutdown.")

        val monitorModeToggleCall = monitorManager.functionCalls.removeAt(0)
        assertEquals("toggleScreenMode", monitorModeToggleCall.name, "The screen mode should be toggled.")

        val redrawUIcall = uiDrawer.functionCalls.removeAt(0)
        assertEquals("drawWindowManagerFrame", redrawUIcall.name, "The window frame needs to be redrawn on screen mode change.")

        val realignWindowsCall = windowCoordinator.functionCalls.removeAt(0)
        assertEquals("realignWindows", realignWindowsCall.name, "The windows need to be realigned after toggling the screen mode.")
    }
}