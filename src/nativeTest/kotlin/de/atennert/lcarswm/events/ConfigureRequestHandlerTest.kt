package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateMock
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.*
import xlib.ConfigureRequest
import xlib.Window
import xlib.XEvent
import xlib.XWindowChanges
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
        override fun hasWindow(windowId: Window): Boolean = windowId == knownWindow
    }
}