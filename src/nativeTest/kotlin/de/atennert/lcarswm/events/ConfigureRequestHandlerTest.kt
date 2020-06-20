package de.atennert.lcarswm.events

import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import de.atennert.lcarswm.window.AppMenuHandler
import de.atennert.lcarswm.window.WindowCoordinatorMock
import de.atennert.lcarswm.window.WindowRegistrationMock
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 *
 */
class ConfigureRequestHandlerTest {
    @Test
    fun `has ConfigureRequest type`() {
        val systemApi = SystemFacadeMock()
        val appMenuHandler = AppMenuHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)
        val configureRequestHandler = ConfigureRequestHandler(systemApi, LoggerMock(), WindowRegistrationMock(), WindowCoordinatorMock(), appMenuHandler)

        assertEquals(ConfigureRequest, configureRequestHandler.xEventType, "The ConfigureRequestHandler should have the ConfigureRequest type")
    }

    @Test
    fun `handle known window`() {
        val systemApi = SystemFacadeMock()
        val knownWindow = systemApi.getNewWindowId()
        val windowRegistration = object : WindowRegistrationMock() {
            override fun isWindowManaged(windowId: Window): Boolean = windowId == knownWindow
        }
        val monitorCoordinator = WindowCoordinatorMock()
        val windowMeasurements = monitorCoordinator.getWindowMeasurements(knownWindow)
        val appMenuHandler = AppMenuHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)

        val configureRequestHandler = ConfigureRequestHandler(systemApi, LoggerMock(), windowRegistration, monitorCoordinator, appMenuHandler)
        systemApi.functionCalls.clear()

        val configureRequestEvent = createConfigureRequestEvent(knownWindow)
        val shutdownValue = configureRequestHandler.handleEvent(configureRequestEvent)

        val configureNotifyCall = systemApi.functionCalls[0]
        val configureWindow = configureNotifyCall.parameters[0]
        val valueMask = configureNotifyCall.parameters[2]
        @Suppress("UNCHECKED_CAST")
        val sentEvent = (configureNotifyCall.parameters[3] as CPointer<XEvent>).pointed
        val configureEvent = sentEvent.xconfigure

        assertFalse(shutdownValue, "The ConfigureRequestHandler shouldn't trigger a shutdown")

        assertEquals("sendEvent", configureNotifyCall.name, "The ConfigureRequestHandler should send a configure notify event for known windows")
        assertEquals(knownWindow, configureWindow, "The receiver window should be the known window")
        assertEquals(StructureNotifyMask, valueMask, "The value mask should be StructureNotify")

        assertEquals(ConfigureNotify, sentEvent.type, "The event type needs to match ConfigureNotify")
        assertEquals(knownWindow, configureEvent.event, "The event should be the known window")
        assertEquals(knownWindow, configureEvent.window, "The window should be the known window")
        assertEquals(windowMeasurements.x, configureEvent.x, "The x value should match the corresponding window measurement")
        assertEquals(windowMeasurements.y, configureEvent.y, "The y value should match the corresponding window measurement")
        assertEquals(windowMeasurements.width, configureEvent.width, "The width should match the corresponding window measurement")
        assertEquals(windowMeasurements.height, configureEvent.height, "The height should match the corresponding window measurement")
        assertEquals(configureRequestEvent.xconfigurerequest.border_width, configureEvent.border_width, "The border width should be the same")
        assertEquals(None.convert(), configureEvent.above, "The above value should be None") // TODO this shouldn't be None, we need to keep that with the window data
        assertEquals(X_FALSE, configureEvent.override_redirect, "Override redirect should be false as we don't handle popups")
    }

    @Test
    fun `handle unknown window`() {
        val systemApi = SystemFacadeMock()
        val windowRegistration = WindowRegistrationMock()
        val monitorCoordinator = WindowCoordinatorMock()
        val appMenuHandler = AppMenuHandler(systemApi, AtomLibrary(systemApi), MonitorManagerMock(), systemApi.rootWindowId)

        val configureRequestHandler = ConfigureRequestHandler(systemApi, LoggerMock(), windowRegistration, monitorCoordinator, appMenuHandler)
        systemApi.functionCalls.clear()

        val configureRequestEvent = createConfigureRequestEvent(systemApi.getNewWindowId())
        val shutdownValue = configureRequestHandler.handleEvent(configureRequestEvent)

        val configureWindowCall = systemApi.functionCalls[0] // there should be only one call for unknown windows
        val configuredWindow = configureWindowCall.parameters[0]
        val valueMask = (configureWindowCall.parameters[1] as UInt).toULong()
        @Suppress("UNCHECKED_CAST")
        val windowChanges: XWindowChanges = (configureWindowCall.parameters[2] as CPointer<XWindowChanges>).pointed

        assertFalse(shutdownValue, "The ConfigureRequestHandler shouldn't trigger a shutdown")

        assertEquals("configureWindow", configureWindowCall.name, "The ConfigureRequestHandler should forward calls for unknown windows to configureWindow")
        assertEquals(configureRequestEvent.xconfigurerequest.window, configuredWindow, "The window should be the same")
        assertEquals(configureRequestEvent.xconfigurerequest.value_mask, valueMask, "The value mask should be the same")
        assertEquals(configureRequestEvent.xconfigurerequest.x, windowChanges.x, "The x value should be the same")
        assertEquals(configureRequestEvent.xconfigurerequest.y, windowChanges.y, "The y value should be the same")
        assertEquals(configureRequestEvent.xconfigurerequest.width, windowChanges.width, "The width should be the same")
        assertEquals(configureRequestEvent.xconfigurerequest.height, windowChanges.height, "The height should be the same")
        assertEquals(configureRequestEvent.xconfigurerequest.above, windowChanges.sibling, "The sibling should be the same as the above value")
        assertEquals(configureRequestEvent.xconfigurerequest.detail, windowChanges.stack_mode, "The stack mode should be the same as the detail value")
        assertEquals(configureRequestEvent.xconfigurerequest.border_width, windowChanges.border_width, "The windows border width should be the same")
    }

    private fun createConfigureRequestEvent(windowId: Window): XEvent {
        val configureRequestEvent = nativeHeap.alloc<XEvent>()
        configureRequestEvent.xconfigurerequest.window = windowId
        configureRequestEvent.xconfigurerequest.value_mask = 123.convert()
        configureRequestEvent.xconfigurerequest.x = 2
        configureRequestEvent.xconfigurerequest.y = 3
        configureRequestEvent.xconfigurerequest.width = 4
        configureRequestEvent.xconfigurerequest.height = 5
        configureRequestEvent.xconfigurerequest.border_width = 10
        configureRequestEvent.xconfigurerequest.above = 6.convert()
        configureRequestEvent.xconfigurerequest.detail = 7
        return configureRequestEvent
    }
}