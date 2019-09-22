package de.atennert.lcarswm.log

interface Logger {
    fun logInfo(text: String)

    fun logError(text: String)

    fun close()
}