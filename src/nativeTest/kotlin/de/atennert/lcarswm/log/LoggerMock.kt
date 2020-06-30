package de.atennert.lcarswm.log

/**
 *
 */
class LoggerMock : Logger {
    var closed = false

    override fun logDebug(text: String) {}

    override fun logInfo(text: String) {}

    override fun logWarning(text: String) {}

    override fun logError(text: String) {}

    fun close() {
        this.closed = true
    }
}