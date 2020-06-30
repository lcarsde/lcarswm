package de.atennert.lcarswm.log

import de.atennert.lcarswm.closeWith
import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.CPointer
import platform.posix.FILE

/**
 * Logger that logs into a file at the given path.
 */
class FileLogger(private val posixApi: PosixApi, logFilePath: String) : Logger {
    private val file: CPointer<FILE> = posixApi.fopen(logFilePath, "w")
        ?: error("FileLogger::init::unable to get the log file $logFilePath")

    init {
        closeWith(FileLogger::close)
    }

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

    private fun writeLog(prefix: String, text: String) {
        posixApi.fputs("${posixApi.gettimeofday()} - $prefix: $text\n", file)
    }

    private fun close() {
        logInfo("FileLogger::close::lcarswm stopped $this")
        posixApi.fclose(file)
    }
}