package de.atennert.lcarswm.time

import kotlinx.cinterop.*
import platform.posix.localtime
import platform.posix.strftime
import platform.posix.timeval

class PosixTime : Time {
    override fun getTime(format: String): String {
        val timeStruct = nativeHeap.alloc<timeval>()
        platform.posix.gettimeofday(timeStruct.ptr, null)
        val timeSeconds = timeStruct.tv_sec
        nativeHeap.free(timeStruct)
        val timeInfo = localtime(timeSeconds.toCPointer())

        return if (timeInfo != null) {
            val timeString = ByteArray(64)
                .apply {
                    this.usePinned { buffer ->
                        strftime(buffer.addressOf(0), 64, format, timeInfo)
                    }
                }
                .toKString()
            nativeHeap.free(timeInfo)
            timeString
        } else {
            "[unknown time]"
        }
    }
}