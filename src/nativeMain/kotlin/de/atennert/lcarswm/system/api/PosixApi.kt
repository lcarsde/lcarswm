package de.atennert.lcarswm.system.api

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import platform.posix.FILE
import platform.posix.__pid_t

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

    fun execvp(fileName: String, args: CPointer<CPointerVar<ByteVar>>): Int

    fun gettimeofday(): Long
}