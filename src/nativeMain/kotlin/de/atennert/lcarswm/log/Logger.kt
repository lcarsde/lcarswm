package de.atennert.lcarswm.log

interface Logger {
    fun printLn(text: String)

    fun close()
}