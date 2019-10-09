import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.LoggingSystemFacadeMock
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import xlib.KeyRelease
import xlib.Screen
import xlib.XErrorHandler
import xlib.XEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShutdownTest {
    @Test
    fun `shutdown when there's no display to get`() {
        val testFacade = object : LoggingSystemFacadeMock() {
            override fun openDisplay(): Boolean {
                super.functionCalls.add(FunctionCall("openDisplay"))
                return false
            }
        }
        runWindowManager(testFacade, LoggerMock())

        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system, when we can't get a display")
    }

    @Test
    fun `shutdown when there's no default screen to get`() {
        val testFacade = object : LoggingSystemFacadeMock() {
            override fun defaultScreenOfDisplay(): CPointer<Screen>? {
                super.functionCalls.add(FunctionCall("defaultScreenOfDisplay"))
                return null
            }
        }
        runWindowManager(testFacade, LoggerMock())

        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertEquals("defaultScreenOfDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get the default screen")
        assertEquals("closeDisplay", testFacade.functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to no default screen")

        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }

    @Test
    fun `shutdown when there is an error response for select input`() {
        val testFacade = object : LoggingSystemFacadeMock() {
            private lateinit var errorHandler: XErrorHandler

            override fun setErrorHandler(handler: XErrorHandler): XErrorHandler? {
                this.errorHandler = handler
                return super.setErrorHandler(handler)
            }

            override fun sync(discardQueuedEvents: Boolean): Int {
                this.errorHandler.invoke(null, null)
                return super.sync(discardQueuedEvents)
            }
        }
        runWindowManager(testFacade, LoggerMock())

        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertEquals("defaultScreenOfDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get the default screen")
        assertEquals("setErrorHandler", testFacade.functionCalls.removeAt(0).name, "startup should set an error handler to get notified if another WM is already active (selected the input on the root window)")
        assertEquals("selectInput", testFacade.functionCalls.removeAt(0).name, "startup should try to select the input on the root window")
        assertEquals("sync", testFacade.functionCalls.removeAt(0).name, "startup should sync after select input to get notified for other WMs")
        assertEquals("closeDisplay", testFacade.functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to another active WM")

        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }

    @Test
    fun `shutdown after sending shutdown key combo`() {
        val testFacade = object : LoggingSystemFacadeMock() {
            override fun nextEvent(event: CPointer<XEvent>): Int {
                super.nextEvent(event)
                event.pointed.type = KeyRelease
                event.pointed.xkey.keycode = 0.convert()
                return 0
            }
        }

        runWindowManager(testFacade, LoggerMock())
    }

    // TODO test when shutdown key-combo was pressed

    // TODO check for logger open and close
}