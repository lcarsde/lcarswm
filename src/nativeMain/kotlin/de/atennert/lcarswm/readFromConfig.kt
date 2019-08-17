package de.atennert.lcarswm

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.cinterop.toKString
import platform.posix.*

/**
 *
 */
fun readFromConfig(fileName: String, key: String): String? {
    val configPathBytes = getenv("XDG_CONFIG_HOME")
    val configPath = configPathBytes?.toKString() ?: return null
    val configFilePath = "$configPath/lcarswm/$fileName"

    val configFile = fopen(configFilePath, "r") ?: return null

    val entry = ByteArray(50).pin()

    var value: String? = null
    while (fscanf(configFile, "%s", entry.addressOf(0)) != EOF) {
        val entryString = entry.get().decodeToString()

        val entryPair = entryString.split('=')
        if (entryPair[0] == key) {
            listOf<String>()
            value = entryPair[1]
            break
        }
    }
    fclose(configFile)
    return value
}
