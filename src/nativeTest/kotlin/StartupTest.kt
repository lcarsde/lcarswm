import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StartupTest {
    @Test
    fun `set DISPLAY environment variable during startup`() {
        val systemFacade = StartupFacadeMock()

        runWindowManager(systemFacade, LoggerMock())

        val startupCalls = systemFacade.functionCalls.takeWhile { it.name != "nextEvent" }

        val setenvCall = startupCalls.singleOrNull { it.name == "setenv" && it.parameters[0] == "DISPLAY" }

        assertNotNull(setenvCall, "setenv should be called to set the DISPLAY name")

        assertEquals(systemFacade.displayString, setenvCall.parameters[1], "the DISPLAY environment variable should be set to the return value of getDisplayString")
    }

    private class StartupFacadeMock : SystemFacadeMock() {
        val displayString = "displayString"

        override fun nextEvent(event: CPointer<XEvent>): Int {
            // send closing key event to stop the window manager
            super.nextEvent(event)
            event.pointed.type = KeyRelease
            event.pointed.xkey.keycode = keySyms.getValue(XK_Q).convert()
            event.pointed.xkey.state = modifiers[winModifierPosition].convert()
            return 0
        }

        override fun getDisplayString(): String {
            return this.displayString
        }
    }
}