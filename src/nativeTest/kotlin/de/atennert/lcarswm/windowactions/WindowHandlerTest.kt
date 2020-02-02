package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.X_TRUE
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.pointed
import xlib.*
import kotlin.test.*

/**
 *
 */
class WindowHandlerTest {
    @Test
    fun `check window initialization`() {
        val systemApi = SystemFacadeMock()
        val rootWindowId: Window = systemApi.rootWindowId
        val windowId: Window = systemApi.getNewWindowId()

        val windowCoordinator = WindowCoordinatorMock()
        val atomLibrary = AtomLibrary(systemApi)
        val focusHandler = WindowFocusHandler()

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowHandler(
            systemApi,
            LoggerMock(),
            windowCoordinator,
            focusHandler,
            atomLibrary,
            rootWindowId
        )

        windowRegistration.addWindow(windowId, false)

        checkWindowAddProcedure(systemApi, windowId, windowCoordinator, focusHandler)
    }

    @Test
    fun `check window initialization for setup`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getWindowAttributes(window: Window, attributes: CPointer<XWindowAttributes>): Int {
                attributes.pointed.map_state = IsViewable
                return 0
            }
        }
        val rootWindowId: Window = systemApi.rootWindowId
        val windowId: Window = systemApi.getNewWindowId()

        val windowCoordinator = WindowCoordinatorMock()
        val atomLibrary = AtomLibrary(systemApi)
        val focusHandler = WindowFocusHandler()

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowHandler(
            systemApi,
            LoggerMock(),
            windowCoordinator,
            focusHandler,
            atomLibrary,
            rootWindowId
        )

        windowRegistration.addWindow(windowId, true)

        checkWindowAddProcedure(systemApi, windowId, windowCoordinator, focusHandler)
    }

    private fun checkWindowAddProcedure(
        systemApi: SystemFacadeMock,
        windowId: Window,
        windowCoordinator: WindowCoordinatorMock,
        focusHandler: WindowFocusHandler
    ) {
        val addWindowToMonitorCall = windowCoordinator.functionCalls.removeAt(0)
        assertEquals("addWindowToMonitor", addWindowToMonitorCall.name, "Add window to a monitor")
        assertEquals(windowId, (addWindowToMonitorCall.parameters[0] as FramedWindow).id, "Add _window_ to monitor")

        val setupCalls = systemApi.functionCalls

        assertEquals("createSimpleWindow", setupCalls.removeAt(0).name, "frame window should be created")
        assertEquals("selectInput", setupCalls.removeAt(0).name, "select the input on the window frame")
        assertEquals("addToSaveSet", setupCalls.removeAt(0).name, "add the windows frame to the save set")
        assertEquals("reparentWindow", setupCalls.removeAt(0).name, "child window should be reparented to frame")
        assertEquals("resizeWindow", setupCalls.removeAt(0).name, "resize window to monitor required dimensions")
        assertEquals("mapWindow", setupCalls.removeAt(0).name, "frame window should be mapped")
        assertEquals("mapWindow", setupCalls.removeAt(0).name, "child window should be mapped")

        val changePropertyCall = setupCalls.removeAt(0)
        assertEquals("changeProperty", changePropertyCall.name, "normal state needs to be _set_ in windows state atom")
        assertEquals(windowId, changePropertyCall.parameters[0], "normal state needs to be set in _windows_ state atom")
        assertEquals(
            NormalState,
            (changePropertyCall.parameters[3] as UByteArray)[0].convert(),
            "_normal state_ needs to be set in windows state atom"
        )

        assertEquals(windowId, focusHandler.getFocusedWindow(), "The new window needs to be focused")
    }

    @Test
    fun `don't add non-viewable windows during setup`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getWindowAttributes(window: Window, attributes: CPointer<XWindowAttributes>): Int {
                attributes.pointed.map_state = IsUnmapped
                return 0
            }
        }
        testForNoActionDuringSetup(systemApi)
    }

    @Test
    fun `don't map override-redirect windows during WM setup`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getWindowAttributes(window: Window, attributes: CPointer<XWindowAttributes>): Int {
                attributes.pointed.override_redirect = X_TRUE
                return 0
            }
        }
        testForNoActionDuringSetup(systemApi)
    }

    private fun testForNoActionDuringSetup(systemApi: SystemFacadeMock) {
        val rootWindowId: Window = systemApi.rootWindowId
        val windowId: Window = systemApi.getNewWindowId()

        val windowCoordinator = WindowCoordinatorMock()
        val atomLibrary = AtomLibrary(systemApi)
        val focusHandler = WindowFocusHandler()

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowHandler(
            systemApi,
            LoggerMock(),
            windowCoordinator,
            focusHandler,
            atomLibrary,
            rootWindowId
        )

        windowRegistration.addWindow(windowId, true)

        assertEquals(0, systemApi.functionCalls.size, "There should no system call")
    }

    @Test
    fun `don't add override-redirect windows`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getWindowAttributes(window: Window, attributes: CPointer<XWindowAttributes>): Int {
                attributes.pointed.override_redirect = X_TRUE
                return 0
            }
        }
        val rootWindowId: Window = systemApi.rootWindowId
        val windowId: Window = systemApi.getNewWindowId()

        val windowCoordinator = WindowCoordinatorMock()
        val atomLibrary = AtomLibrary(systemApi)
        val focusHandler = WindowFocusHandler()

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowHandler(
            systemApi,
            LoggerMock(),
            windowCoordinator,
            focusHandler,
            atomLibrary,
            rootWindowId
        )

        windowRegistration.addWindow(windowId, false)

        val setupCalls = systemApi.functionCalls

        assertEquals(1, setupCalls.size, "There should only be one call to map the popup")

        val mapCall = setupCalls.removeAt(0)
        assertEquals("mapWindow", mapCall.name, "We need to map the popup")
        assertEquals(windowId, mapCall.parameters[0], "The popup needs to be mapped")
    }

    @Test
    fun `provide info about whether we know a certain window`() {
        val systemApi = SystemFacadeMock()

        val rootWindowId = systemApi.rootWindowId
        val windowId = systemApi.getNewWindowId()

        val windowCoordinator = WindowCoordinatorMock()
        val atomLibrary = AtomLibrary(systemApi)
        val focusHandler = WindowFocusHandler()

        val windowRegistration = WindowHandler(
            systemApi,
            LoggerMock(),
            windowCoordinator,
            focusHandler,
            atomLibrary,
            rootWindowId
        )

        assertFalse(windowRegistration.isWindowManaged(windowId), "An unknown window should not be reported managed")

        windowRegistration.addWindow(windowId, false)

        assertTrue(windowRegistration.isWindowManaged(windowId), "A known window should be reported managed")
    }

    @Test
    fun `check window removal`() {
        val systemApi = SystemFacadeMock()

        val rootWindowId = systemApi.rootWindowId
        val windowId = systemApi.getNewWindowId()

        val windowCoordinator = WindowCoordinatorMock()
        val atomLibrary = AtomLibrary(systemApi)
        val focusHandler = WindowFocusHandler()

        val windowRegistration = WindowHandler(
            systemApi,
            LoggerMock(),
            windowCoordinator,
            focusHandler,
            atomLibrary,
            rootWindowId
        )
        windowRegistration.addWindow(windowId, false)

        systemApi.functionCalls.clear() // remove AtomLibrary setup and window adding
        windowCoordinator.functionCalls.clear()

        windowRegistration.removeWindow(windowId)

        val unregisterSystemCalls = systemApi.functionCalls

        val unmapFrameCall = unregisterSystemCalls.removeAt(0)
        assertEquals("unmapWindow", unmapFrameCall.name, "The frame of the removed window needs to be _unmapped_")

        val reparentCall = unregisterSystemCalls.removeAt(0)
        assertEquals("reparentWindow", reparentCall.name, "We need to _reparent_ the window back to root")
        assertEquals(windowId, reparentCall.parameters[0], "We need to reparent the _window_ back to root")
        assertEquals(rootWindowId, reparentCall.parameters[1], "We need to reparent the window back to _root_")

        val removeFromSaveSetCall = unregisterSystemCalls.removeAt(0)
        assertEquals("removeFromSaveSet", removeFromSaveSetCall.name, "We need to _remove_ the window from the save set")
        assertEquals(windowId, removeFromSaveSetCall.parameters[0], "We need to remove the _window_ from the save set")
        
        val destroyFrameCall = unregisterSystemCalls.removeAt(0)
        assertEquals("destroyWindow", destroyFrameCall.name, "The frame of the removed window needs to be _destroyed_")

        val removeFromCoordinatorCall = windowCoordinator.functionCalls.removeAt(0)
        assertEquals("removeWindow", removeFromCoordinatorCall.name, "Remove the window from the window coordinator")
        assertEquals(windowId, (removeFromCoordinatorCall.parameters[0] as FramedWindow).id, "Remove the _window_ from the window coordinator")

        assertFalse(windowRegistration.isWindowManaged(windowId), "The window should not be managed anymore")

        assertNull(focusHandler.getFocusedWindow(), "The focused window needs to be unset")
    }

    @Test
    fun `check for window parent`() {
        val systemApi = SystemFacadeMock()

        val rootWindowId = systemApi.rootWindowId
        val windowId = systemApi.getNewWindowId()
        val parentId = systemApi.nextWindowId

        val windowCoordinator = WindowCoordinatorMock()
        val atomLibrary = AtomLibrary(systemApi)
        val focusHandler = WindowFocusHandler()

        val windowRegistration = WindowHandler(
            systemApi,
            LoggerMock(),
            windowCoordinator,
            focusHandler,
            atomLibrary,
            rootWindowId
        )
        windowRegistration.addWindow(windowId, false)

        assertTrue(windowRegistration.isWindowParentedBy(windowId, parentId), "Return true for the registered window parent")
        assertFalse(windowRegistration.isWindowParentedBy(windowId, rootWindowId), "Return false for unknown window parents")
        assertFalse(windowRegistration.isWindowParentedBy(rootWindowId, rootWindowId), "Return false for unmanaged windows")
    }
}
