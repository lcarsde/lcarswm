package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.WindowContainer
import de.atennert.lcarswm.WindowManagerStateMock
import de.atennert.lcarswm.X_TRUE
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.pointed
import xlib.IsViewable
import xlib.NormalState
import xlib.Window
import xlib.XWindowAttributes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 *
 */
class WindowRegistrationTest {
    @Test
    fun `check window initialization`() {
        val systemApi = SystemFacadeMock()
        val rootWindowId: Window = 2.convert()
        val windowId: Window = 5.convert()

        val windowManagerState = WindowManagerStateMock()
        val atomLibrary = AtomLibrary(systemApi)

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowRegistration(
            systemApi,
            LoggerMock(),
            windowManagerState,
            atomLibrary,
            rootWindowId
        )

        windowRegistration.addWindow(windowId, false)

        checkWindowAddProcedure(systemApi, windowId, windowManagerState)
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

        val windowManagerState = WindowManagerStateMock()
        val atomLibrary = AtomLibrary(systemApi)

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowRegistration(
            systemApi,
            LoggerMock(),
            windowManagerState,
            atomLibrary,
            rootWindowId
        )

        windowRegistration.addWindow(windowId, true)

        checkWindowAddProcedure(systemApi, windowId, windowManagerState)
    }

    private fun checkWindowAddProcedure(
        systemApi: SystemFacadeMock,
        windowId: Window,
        windowManagerState: WindowManagerStateMock
    ) {
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

        val addWindowCall = windowManagerState.functionCalls.removeAt(0)
        assertEquals("addWindow", addWindowCall.name, "the child window should be _added to the window list_")
        assertEquals(
            windowId,
            (addWindowCall.parameters[0] as WindowContainer).id,
            "the _child window_ should be added to the window list"
        )
    }

    // TODO setup && !viewable (negative)
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

        val windowManagerState = WindowManagerStateMock()
        val atomLibrary = AtomLibrary(systemApi)

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowRegistration(
            systemApi,
            LoggerMock(),
            windowManagerState,
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
    fun `don't map override-redirect windows during WM setup`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getWindowAttributes(window: Window, attributes: CPointer<XWindowAttributes>): Int {
                attributes.pointed.override_redirect = X_TRUE
                return 0
            }
        }
        val rootWindowId: Window = systemApi.rootWindowId
        val windowId: Window = systemApi.getNewWindowId()

        val windowManagerState = WindowManagerStateMock()
        val atomLibrary = AtomLibrary(systemApi)

        systemApi.functionCalls.clear() // remove AtomLibrary setup

        val windowRegistration = WindowRegistration(
            systemApi,
            LoggerMock(),
            windowManagerState,
            atomLibrary,
            rootWindowId
        )

        windowRegistration.addWindow(windowId, true)

        assertEquals(0, systemApi.functionCalls.size, "There should no system call")
    }

    @Test
    fun `provide info about whether we know a certain window`() {
        val rootWindowId: Window = 2.convert()
        val windowId: Window = 5.convert()

        val systemApi = SystemFacadeMock()
        val windowManagerState = WindowManagerStateMock()
        val atomLibrary = AtomLibrary(systemApi)

        val windowRegistration = WindowRegistration(
            systemApi,
            LoggerMock(),
            windowManagerState,
            atomLibrary,
            rootWindowId
        )

        assertFalse(windowRegistration.isWindowManaged(windowId), "An unknown window should not be reported managed")

        windowRegistration.addWindow(windowId, false)

        assertTrue(windowRegistration.isWindowManaged(windowId), "A known window should be reported managed")
    }
}