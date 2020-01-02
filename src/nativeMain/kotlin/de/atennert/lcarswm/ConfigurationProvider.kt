package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin

class ConfigurationProvider(posixApi: PosixApi, configurationFilePath: String) {

    private val properties: Map<String, String>

    init {
        val filePointer = posixApi.fopen(configurationFilePath, "r")

        properties = if (filePointer == null) {
            emptyMap()
        } else {
            val entry = ByteArray(60).pin()
            val entryMap = mutableMapOf<String, String>()

            while (posixApi.fgets(entry.addressOf(0), entry.get().size, filePointer) != null) {
                val entryPair = entry.get()
                    .takeWhile { it > 0 }
                    .fold("") {acc, b -> acc + b.toChar()}
                    .trim()
                    .split('=')

                entryMap[entryPair[0]] = entryPair[1]
            }

            posixApi.fclose(filePointer)
            entryMap
        }
    }

    operator fun get(propertyKey: String): String? = properties[propertyKey]
}