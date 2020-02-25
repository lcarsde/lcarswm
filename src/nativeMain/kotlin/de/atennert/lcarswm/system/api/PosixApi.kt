package de.atennert.lcarswm.system.api

import de.atennert.lcarswm.signal.Signal
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import platform.posix.FILE
import platform.posix.__pid_t
import platform.posix.sigaction
import platform.posix.sigset_t

/**
 * Interface for accessing POSIX functions
 */
interface PosixApi {
    fun getenv(name: String): CPointer<ByteVar>?

    fun fopen(fileName: String, modes: String): CPointer<FILE>?

    fun fgets(buffer: CPointer<ByteVar>, bufferSize: Int, file: CPointer<FILE>): CPointer<ByteVar>?

    fun fputs(s: String, file: CPointer<FILE>): Int

    fun fclose(file: CPointer<FILE>): Int

    fun feof(file: CPointer<FILE>): Int

    fun fork(): __pid_t

    fun setsid(): __pid_t

    fun setenv(name: String, value: String): Int

    fun perror(s: String)

    fun exit(status: Int)

    /**
     * Run a program
     * @param fileName The programs executable file name
     * @param args The program arguments (including the fileName)
     */
    fun execvp(fileName: String, args: List<String>): Int

    fun gettimeofday(): Long

    fun usleep(time: UInt)

    fun abort()

    fun sigFillSet(sigset: CPointer<sigset_t>)

    fun sigEmptySet(sigset: CPointer<sigset_t>)

    fun sigAction(signal: Signal, newSigAction: CPointer<sigaction>?, oldSigAction: CPointer<sigaction>?)

    fun sigProcMask(how: Int, newSigset: CPointer<sigset_t>?, oldSigset: CPointer<sigset_t>?)
}