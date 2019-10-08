import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.LoggingSystemFacadeMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShutdownTest {
    class NoDisplayFacade : LoggingSystemFacadeMock() {
        override fun openDisplay(): Boolean {
            super.functionCalls.add(FunctionCall("openDisplay"))
            return false
        }
    }

    @Test
    fun `shutdown when there's no display to get`() {
        val testFacade = NoDisplayFacade()
        runWindowManager(testFacade, LoggerMock())

        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system, when we can't get a display")
    }
}