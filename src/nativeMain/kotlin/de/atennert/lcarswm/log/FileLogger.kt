package de.atennert.lcarswm.log

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.CPointer
import platform.posix.FILE

class FileLogger(private val posixApi: PosixApi, logFilePath: String) : Logger {
    private val file: CPointer<FILE> = posixApi.fopen(logFilePath, "w")
        ?: error("FileLogger::init::unable to get the log file $logFilePath")

    override fun printLn(text: String) {
        posixApi.fputs("$text\n", file)
    }

    override fun close() {
        posixApi.fclose(file)
    }
}