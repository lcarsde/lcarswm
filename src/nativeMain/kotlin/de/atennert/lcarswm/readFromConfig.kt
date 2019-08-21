package de.atennert.lcarswm

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.cinterop.toKString
import platform.posix.*

/**
 *
 */
fun readFromConfig(fileName: String, key: String): List<String>? {
    val configPathBytes = getenv("XDG_CONFIG_HOME")
    val configPath = configPathBytes?.toKString() ?: return null
    val configFilePath = "$configPath/lcarswm/$fileName"

    val configFile = fopen(configFilePath, "r") ?: return null

    val entry = ByteArray(60).pin()

    var value: List<String>? = null
    while (fgets(entry.addressOf(0), entry.get().size, configFile) != null) {
        val entryString = entry.get().takeWhile { it > 0 }.fold("") {acc, b -> acc + b.toChar()}.trim()
        println("::readFromConfig::entry: $entryString")

        val entryPair = entryString.split('=')
        if (entryPair[0] == key) {
            value = entryPair[1].split(' ')
            break
        }
    }
    fclose(configFile)
    return value
}
