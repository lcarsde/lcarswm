import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.signal.Signal
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.*

class StartupTest {
    @Test
    fun `check startup`() {
        val systemFacade = StartupFacadeMock()

        runWindowManager(systemFacade, LoggerMock())

        val startupCalls = systemFacade.functionCalls

        checkCoreSignalRegistration(startupCalls)

        assertEquals("openDisplay", startupCalls.removeAt(0).name, "try to open the display")

        assertEquals("defaultScreenOfDisplay", startupCalls.removeAt(0).name, "try to get the displays screen")

        assertEquals("synchronize", startupCalls.removeAt(0).name, "synchronize after getting the display and randr resources")
    }

    private fun checkCoreSignalRegistration(startupCalls: MutableList<FunctionCall>) {
        assertEquals("sigFillSet", startupCalls.removeAt(0).name, "Initialize the signal set")
        assertEquals("sigEmptySet", startupCalls.removeAt(0).name, "Initialize the action signal set")

        Signal.CORE_DUMP_SIGNALS
            .filter { it != Signal.ABRT }
            .forEach {
                val signalRegistrationCall = startupCalls.removeAt(0)
                assertEquals("sigAction", signalRegistrationCall.name, "Initialize signal $it")
                assertEquals(it, signalRegistrationCall.parameters[0], "Initialize signal _${it}_")
            }
    }

    @Test
    fun `set DISPLAY environment variable during startup`() {
        val systemFacade = StartupFacadeMock()

        runWindowManager(systemFacade, LoggerMock())

        val startupCalls = systemFacade.functionCalls.takeWhile { it.name != "nextEvent" }

        val setenvCall = startupCalls.singleOrNull { it.name == "setenv" && it.parameters[0] == "DISPLAY" }

        assertNotNull(setenvCall, "setenv should be called to set the DISPLAY name")

        assertEquals(
            systemFacade.displayString,
            setenvCall.parameters[1],
            "the DISPLAY environment variable should be set to the return value of getDisplayString"
        )
    }

    @Test
    fun `send client message informing that we are the WM`() {
        val systemFacade = StartupFacadeMock()

        runWindowManager(systemFacade, LoggerMock())

        val startupCalls = systemFacade.functionCalls.takeWhile { it.name != "nextEvent" }

        val sendEventCall = startupCalls.singleOrNull { it.name == "sendEvent" && it.parameters[0] == systemFacade.rootWindowId }

        assertNotNull(sendEventCall, "We need to send an event to notify about lcarswm being the WM")

        assertFalse(sendEventCall.parameters[1] as Boolean, "Don't propagate")

        assertEquals(SubstructureNotifyMask, sendEventCall.parameters[2], "The event mask is substructure notify")

        val eventData = sendEventCall.parameters[3] as CPointer<XEvent>
        assertEquals(ClientMessage, eventData.pointed.xclient.type, "The event needs to be a client message")
    }

    @Test
    fun `set required properties`() {
        val systemFacade = StartupFacadeMock()
        val rootWindow: Window = 1.convert() // hard coded in SystemFacadeMock
        val supportWindow: Window = systemFacade.nextWindowId // first created window starts at 2 in SystemFacadeMock

        runWindowManager(systemFacade, LoggerMock())

        val propertyCalls = systemFacade.functionCalls.filter { it.name == "changeProperty" }
        val atoms = systemFacade.atomMap

        val expectedProperties = listOf(
            Pair(rootWindow, "_NET_SUPPORTING_WM_CHECK"),
            Pair(rootWindow, "_NET_SUPPORTED"),
            Pair(supportWindow, "_NET_SUPPORTING_WM_CHECK"),
            Pair(supportWindow, "_NET_WM_NAME")
        )

        expectedProperties.forEach { (window, atomName) ->
            assertNotNull(
                propertyCalls.find { it.parameters[0] == window && it.parameters[1] == atoms[atomName] },
                "The property $atomName should set/changed on window $window"
            )
        }

    }

    private class StartupFacadeMock : SystemFacadeMock() {
        var eventCount = 1
        override fun nextEvent(event: CPointer<XEvent>): Int {
            when (eventCount) {
                1, 2 -> {
                    event.pointed.type = PropertyNotify
                    event.pointed.xproperty.time = eventCount.convert()
                }
                else -> {
                    super.nextEvent(event)
                    event.pointed.type = KeyRelease
                    event.pointed.xkey.time = 234.convert()
                    event.pointed.xkey.keycode = keySyms.getValue(XK_Q).convert()
                    event.pointed.xkey.state = 0x40.convert()
                }
            }
            eventCount++
            return Success
        }
    }
}