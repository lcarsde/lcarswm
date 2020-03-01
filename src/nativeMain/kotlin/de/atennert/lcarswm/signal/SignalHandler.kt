package de.atennert.lcarswm.signal

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.*
import platform.posix.SA_NOCLDSTOP
import platform.posix.sigaction
import platform.posix.sigset_t

private var globalPosixApi: PosixApi? = null

private fun handleCoreSignal() {
    globalPosixApi?.abort()
}

class SignalHandler(posixApi: PosixApi) {

    private val allSignals: sigset_t = nativeHeap.alloc()

    private val oldActions = mutableMapOf<Int, sigaction>()

    init {
        globalPosixApi = posixApi
        val action = nativeHeap.alloc<sigaction>()

        posixApi.sigFillSet(allSignals.ptr)
        posixApi.sigEmptySet(action.sa_mask.ptr)

        action.__sigaction_handler.sa_handler = staticCFunction<Int, Unit> { handleCoreSignal() }
        action.sa_flags = SA_NOCLDSTOP

        Signal.CORE_DUMP_SIGNALS
                // don't register for ABRT because it will be triggered by abort from other core signals
            .filter { it != Signal.ABRT }
            .forEach {
                val oldSignal = nativeHeap.alloc<sigaction>()
                posixApi.sigAction(it, action.ptr, oldSignal.ptr)
                oldActions[it.signalValue] = oldSignal
            }
    }
}