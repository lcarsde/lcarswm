package de.atennert.lcarswm.signal

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlin.test.Test
import kotlin.test.assertEquals

class SignalHandlerTest {
    @Test
    fun `register for core signals on startup`() {
        val system = SystemFacadeMock()
        SignalHandler(system)

        val signalInitCalls = system.functionCalls
        assertEquals("sigFillSet", signalInitCalls.removeAt(0).name, "Initialize the signal set")
        assertEquals("sigEmptySet", signalInitCalls.removeAt(0).name, "Initialize the action signal set")

        Signal.CORE_DUMP_SIGNALS
            .filter { it != Signal.ABRT }
            .forEach { signal ->
                val signalActionCall = signalInitCalls.removeAt(0)
                assertEquals("sigAction", signalActionCall.name, "Register action for $signal")
                assertEquals(signal, signalActionCall.parameters[0], "Register action for _${signal}_")
            }
    }

    @Test
    fun `handle core signal`() {
        val system = SystemFacadeMock()
        SignalHandler(system)

        system.functionCalls.clear()

        system.signalActions
            .onEach { (signal, actionPtr) -> actionPtr.pointed.__sigaction_handler.sa_handler?.invoke(signal.signalValue) }
            .forEach { (signal, _) ->
                val abortCall = system.functionCalls.removeAt(0)
                assertEquals("abort", abortCall.name, "Call abort for signal $signal")
            }
    }
}