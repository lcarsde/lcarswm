package de.atennert.lcarswm.events

import de.atennert.lcarswm.*
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
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
        val configureRequestHandler = ConfigureRequestHandler(SystemFacadeMock(), LoggerMock(), WindowManagerStateMock())

        assertEquals(ConfigureRequest, configureRequestHandler.xEventType, "The ConfigureRequestHandler should have the ConfigureRequest type")
    }

    @Test
    fun `handle known window`() {
        val system = object : SystemFacadeMock() {
            val display = nativeHeap.allocPointerTo<Display>().value
            override fun getDisplay(): CPointer<Display>? = display
        }
        val knownWindow: Window = 1.convert()
        val windowManagerState = WindowManagerStateTestImpl(knownWindow)
        val windowMeasurements = windowManagerState.windows[0].second.getCurrentWindowMeasurements(ScreenMode.NORMAL)

        val configureRequestHandler = ConfigureRequestHandler(system, LoggerMock(), windowManagerState)

        val configureRequestEvent = createConfigureRequestEvent()
        val shutdownValue = configureRequestHandler.handleEvent(configureRequestEvent)

        val configureNotifyCall = system.functionCalls[0]
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
        assertEquals(system.display, configureEvent.display, "The display should match the facades display")
        assertEquals(knownWindow, configureEvent.event, "The event should be the known window")
        assertEquals(knownWindow, configureEvent.window, "The window should be the known window")
        assertEquals(windowMeasurements[0], configureEvent.x, "The x value should match the corresponding window measurement")
        assertEquals(windowMeasurements[1], configureEvent.y, "The y value should match the corresponding window measurement")
        assertEquals(windowMeasurements[2], configureEvent.width, "The width should match the corresponding window measurement")
        assertEquals(windowMeasurements[3], configureEvent.height, "The height should match the corresponding window measurement")
        assertEquals(0, configureEvent.border_width, "The border width should be 0")
        assertEquals(None.convert(), configureEvent.above, "The above value should be None") // TODO this shouldn't be None, we need to keep that with the window data
        assertEquals(X_FALSE, configureEvent.override_redirect, "Override redirect should be false as we don't handle popups")
    }

    @Test
    fun `handle unknown window`() {
        val system = SystemFacadeMock()
        val windowManagerState = WindowManagerStateTestImpl()

        val configureRequestHandler = ConfigureRequestHandler(system, LoggerMock(), windowManagerState)

        val configureRequestEvent = createConfigureRequestEvent()
        val shutdownValue = configureRequestHandler.handleEvent(configureRequestEvent)

        val configureWindowCall = system.functionCalls[0] // there should be only one call for unknown windows
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
        assertEquals(0, windowChanges.border_width, "The windows border width should be 0")
    }

    private fun createConfigureRequestEvent(): XEvent {
        val configureRequestEvent = nativeHeap.alloc<XEvent>()
        configureRequestEvent.xconfigurerequest.window = 1.convert()
        configureRequestEvent.xconfigurerequest.value_mask = 123.convert()
        configureRequestEvent.xconfigurerequest.x = 2
        configureRequestEvent.xconfigurerequest.y = 3
        configureRequestEvent.xconfigurerequest.width = 4
        configureRequestEvent.xconfigurerequest.height = 5
        configureRequestEvent.xconfigurerequest.above = 6.convert()
        configureRequestEvent.xconfigurerequest.detail = 7
        return configureRequestEvent
    }

    private class WindowManagerStateTestImpl(private val knownWindow: Window? = null) : WindowManagerStateMock() {
        override val windows = mutableListOf<Pair<WindowContainer, Monitor>>()

        init {
            if (knownWindow != null) {
                windows.add(Pair(WindowContainer(knownWindow), Monitor(2.convert(), "", true)))
            }
        }
        override fun hasWindow(windowId: Window): Boolean = windowId == knownWindow
    }
}