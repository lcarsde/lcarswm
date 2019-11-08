package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.WindowContainer
import de.atennert.lcarswm.WindowManagerStateMock
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms.WM_STATE
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.convert
import xlib.Atom
import xlib.NormalState
import xlib.Window
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
        val rootWindowId: Window = 2.convert()
        val windowId: Window = 5.convert()
        val frameId: Window = 12.convert()
        val commandList = mutableListOf<String>()

        val systemApi = SystemApiHelper(frameId, commandList)
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

        assertEquals("createSimpleWindow-$rootWindowId", commandList.removeAt(0), "frame window should be created firstly")
        assertEquals("reparentWindow-$windowId-$frameId", commandList.removeAt(0), "child window should be reparented to frame secondly")
        assertEquals("mapWindow-$frameId", commandList.removeAt(0), "frame window should be mapped thirdly")
        assertEquals("mapWindow-$windowId", commandList.removeAt(0), "child window should be mapped fourthly")
        assertEquals("changeProperty - $windowId:${atomLibrary[WM_STATE]}:${atomLibrary[WM_STATE]}:$NormalState", commandList.removeAt(0), "normal state needs to be set in windows frame atom")

        val addWindowCall = windowManagerState.functionCalls.removeAt(0)
        assertEquals("addWindow", addWindowCall.name, "the child window should be _added to the window list_")
        assertEquals(windowId, (addWindowCall.parameters[0] as WindowContainer).id, "the _child window_ should be added to the window list")

        assertTrue(commandList.isEmpty(), "There should be no unchecked commands")
    }

    // TODO override-redirect window (negative)

    // TODO setup && !viewable (negative)

    // TODO setup && viewable (positive)

    // TODO get rid of SystemApiHelper

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

    class SystemApiHelper(
        private val frameId: Window,
        private val commandList: MutableList<String>
    ) : SystemFacadeMock() {
        override fun createSimpleWindow(parentWindow: Window, measurements: List<Int>): Window {
            commandList.add("createSimpleWindow-$parentWindow")
            return frameId
        }

        override fun reparentWindow(window: Window, parent: Window, x: Int, y: Int): Int {
            commandList.add("reparentWindow-$window-$parent")
            return 0
        }

        override fun mapWindow(window: Window): Int {
            commandList.add("mapWindow-$window")
            return 0
        }

        override fun changeProperty(
            window: Window,
            propertyAtom: Atom,
            typeAtom: Atom,
            data: UByteArray?,
            format: Int
        ): Int {
            commandList.add("changeProperty - $window:$propertyAtom:$typeAtom:${data?.get(0)}")
            return 0
        }
    }
}