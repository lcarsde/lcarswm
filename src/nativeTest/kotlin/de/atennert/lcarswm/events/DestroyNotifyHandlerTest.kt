package de.atennert.lcarswm.events

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import de.atennert.lcarswm.window.AppMenuHandler
import de.atennert.lcarswm.window.StatusBarHandler
import de.atennert.lcarswm.window.WindowRegistrationMock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import xlib.DestroyNotify
import xlib.Window
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DestroyNotifyHandlerTest {
    @Test
    fun `return the event type DestroyNotify`() {
        val systemApi = SystemFacadeMock()
        val appMenuHandler = AppMenuHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)
        val statusBarHandler = StatusBarHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)
        val destroyNotifyHandler = DestroyNotifyHandler(LoggerMock(), WindowRegistrationMock(), appMenuHandler, statusBarHandler)

        assertEquals(DestroyNotify, destroyNotifyHandler.xEventType, "The event type for DestroyEventHandler needs to be DestroyNotify")
    }

    @Test
    fun `remove window on destroy notify`() {
        val windowId: Window = 1.convert()

        val destroyNotifyEvent = nativeHeap.alloc<XEvent>()
        destroyNotifyEvent.xdestroywindow.window = windowId

        val windowRegistration = WindowRegistrationMock()
        windowRegistration.addWindow(windowId, false)
        windowRegistration.functionCalls.clear()
        val systemApi = SystemFacadeMock()
        val appMenuHandler = AppMenuHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)
        val statusBarHandler = StatusBarHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)

        val destroyNotifyHandler = DestroyNotifyHandler(LoggerMock(), windowRegistration, appMenuHandler, statusBarHandler)
        val requestShutdown = destroyNotifyHandler.handleEvent(destroyNotifyEvent)

        assertFalse(requestShutdown, "Destroy handling should not request shutdown of the window manager")

        val removeWindowCall = windowRegistration.functionCalls.removeAt(0)
        assertEquals("removeWindow", removeWindowCall.name, "Remove needs to be called on the window registration")
        assertEquals(windowId, removeWindowCall.parameters[0], "The window needs to be removed from the registration")
    }

    @Test
    fun `don't remove unknown window`() {
        val windowId: Window = 1.convert()

        val destroyNotifyEvent = nativeHeap.alloc<XEvent>()
        destroyNotifyEvent.xdestroywindow.window = windowId

        val windowRegistration = WindowRegistrationMock()
        val systemApi = SystemFacadeMock()
        val appMenuHandler = AppMenuHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)
        val statusBarHandler = StatusBarHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)

        val destroyNotifyHandler = DestroyNotifyHandler(LoggerMock(), windowRegistration, appMenuHandler, statusBarHandler)
        val requestShutdown = destroyNotifyHandler.handleEvent(destroyNotifyEvent)

        assertFalse(requestShutdown, "Destroy handling should not request shutdown of the window manager")

        assertTrue(windowRegistration.functionCalls.isEmpty(), "an unknown window shouldn't be removed")
    }
}
