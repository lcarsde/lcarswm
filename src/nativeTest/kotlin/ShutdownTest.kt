import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.LoggingSystemFacadeMock
import kotlinx.cinterop.CPointer
import xlib.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShutdownTest {
    @Test
    fun `shutdown when there's no display to get`() {
        val testFacade = NoDisplayFacade()
        runWindowManager(testFacade, LoggerMock())

        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system, when we can't get a display")
    }

    class NoDisplayFacade : LoggingSystemFacadeMock() {
        override fun openDisplay(): Boolean {
            super.functionCalls.add(FunctionCall("openDisplay"))
            return false
        }
    }

    @Test
    fun `shutdown when there's no default screen to get`() {
        val testFacade = NoScreenFacade()
        runWindowManager(testFacade, LoggerMock())

        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertEquals("defaultScreenOfDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get the default screen")
        assertEquals("closeDisplay", testFacade.functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to no default screen")

        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }

    class NoScreenFacade : LoggingSystemFacadeMock() {
        override fun defaultScreenOfDisplay(): CPointer<Screen>? {
            super.functionCalls.add(FunctionCall("defaultScreenOfDisplay"))
            return null
        }
    }
}