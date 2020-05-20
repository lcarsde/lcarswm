package de.atennert.lcarswm.system.api

import de.atennert.lcarswm.signal.Signal
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UIntVar
import platform.linux.mq_attr
import platform.linux.mqd_t
import platform.posix.*

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

    fun sigAction(signal: Signal, newSigAction: CPointer<sigaction>, oldSigAction: CPointer<sigaction>?)

    fun sigProcMask(how: Int, newSigset: CPointer<sigset_t>?, oldSigset: CPointer<sigset_t>?)

    fun mqOpen(name: String, oFlag: Int, mode: mode_t, attributes: CPointer<mq_attr>): mqd_t

    fun mqClose(mq: mqd_t): Int

    fun mqSend(mq: mqd_t, msg: String, msgPrio: UInt): Int

    fun mqReceive(mq: mqd_t, msgPtr: CPointer<ByteVar>, msgSize: size_t, msgPrio: CPointer<UIntVar>?): ssize_t

    fun mqUnlink(name: String): Int
}