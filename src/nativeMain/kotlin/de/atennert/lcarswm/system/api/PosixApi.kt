package de.atennert.lcarswm.system.api

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import platform.posix.FILE
import platform.posix.__pid_t

/**
 *
 */
interface PosixApi {
    fun getenv(name: String): CPointer<ByteVar>?

    fun fopen(fileName: String, modes: String): CPointer<FILE>?

    fun fgets(buffer: CValuesRef<ByteVar>, bufferSize: Int, file: CValuesRef<FILE>): CPointer<ByteVar>?

    fun fclose(file: CValuesRef<FILE>): Int

    fun fork(): __pid_t

    fun setsid(): __pid_t

    fun perror(s: String)

    fun exit(status: Int)

    fun execvp(fileName: String, args: CValuesRef<CPointerVar<ByteVar>>): Int
}