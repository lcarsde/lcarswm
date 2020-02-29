package de.atennert.lcarswm.signal

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.*
import platform.posix.SA_NOCLDSTOP
import platform.posix.sigaction
import platform.posix.sigset_t

class SignalHandler(posixApi: PosixApi) {

    private val allSignals: sigset_t = nativeHeap.alloc()

    init {
        val action = nativeHeap.alloc<sigaction>()

        posixApi.sigFillSet(allSignals.ptr)
        posixApi.sigEmptySet(action.sa_mask.ptr)

        action.__sigaction_handler.sa_handler = staticCFunction { signal: Int -> handleSignal(signal) }
        action.sa_flags = SA_NOCLDSTOP

        Signal.CORE_DUMP_SIGNALS.forEach {
            // TODO register actions
        }
    }

    companion object {
        fun handleSignal(signal: Int) {
            // TODO handle signal
        }
    }
}