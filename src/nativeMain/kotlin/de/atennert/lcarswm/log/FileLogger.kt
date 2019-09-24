package de.atennert.lcarswm.log

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.CPointer
import platform.posix.FILE

class FileLogger(private val posixApi: PosixApi, logFilePath: String) : Logger {
    private val file: CPointer<FILE> = posixApi.fopen(logFilePath, "w")
        ?: error("FileLogger::init::unable to get the log file $logFilePath")

    override fun logDebug(text: String) {
        writeLog("DEBUG", text)
    }

    override fun logInfo(text: String) {
        writeLog(" INFO", text)
    }

    override fun logWarning(text: String) {
        writeLog(" WARN", text)
    }

    override fun logError(text: String) {
        writeLog("ERROR", text)
    }

    override fun close() {
        posixApi.fclose(file)
    }

    private fun writeLog(prefix: String, text: String) {
        posixApi.fputs("${posixApi.gettimeofday()} - $prefix: $text\n", file)
    }
}