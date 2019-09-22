package de.atennert.lcarswm.log

/**
 *
 */
class LoggerMock : Logger {
    override fun logDebug(text: String) {}

    override fun logInfo(text: String) {}

    override fun logWarning(text: String) {}

    override fun logError(text: String) {}

    override fun close() {}
}