package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.cinterop.toKString

/**
 *
 */
fun readFromConfig(posixApi: PosixApi, fileName: String, key: String): List<String>? {
    val configPathBytes = posixApi.getenv("XDG_CONFIG_HOME") ?: return null
    val configPath = configPathBytes.toKString()
    val configFilePath = "$configPath/lcarswm/$fileName"

    val configFile = posixApi.fopen(configFilePath, "r") ?: return null

    val entry = ByteArray(60).pin()

    var value: List<String>? = null
    while (posixApi.fgets(entry.addressOf(0), entry.get().size, configFile) != null) {
        val entryString = entry.get().takeWhile { it > 0 }.fold("") {acc, b -> acc + b.toChar()}.trim()

        val entryPair = entryString.split('=')
        if (entryPair[0] == key) {
            value = entryPair[1].split(' ')
            break
        }
    }
    posixApi.fclose(configFile)
    return value
}
