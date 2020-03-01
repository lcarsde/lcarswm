package de.atennert.lcarswm.signal

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlin.test.Test
import kotlin.test.assertEquals

class SignalHandlerTest {
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