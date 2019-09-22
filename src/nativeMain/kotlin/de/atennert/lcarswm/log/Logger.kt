package de.atennert.lcarswm.log

interface Logger {
    fun logDebug(text: String)

    fun logInfo(text: String)

    fun logWarning(text: String)

    fun logError(text: String)

    fun close()
}