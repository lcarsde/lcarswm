package de.atennert.lcarswm.settings

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

/**
 * Used to read files from the file system.
 */
class FileReader(private val posixApi: PosixApi, private val filePath: String) {

    private val readBufferSize = 60

    /**
     * Read a file in lines and provide the lines (without trailing line break) to a given consumer.
     *
     * @param consumer Consumer that will receive the read lines
     */
    fun readLines(consumer: (String) -> Unit) {
        val filePointer = posixApi.fopen(filePath, "r") ?: return

        var bufferString = ""

        ByteArray(readBufferSize).usePinned { entry ->
            while (posixApi.fgets(entry.addressOf(0), readBufferSize, filePointer) != null) {
                bufferString += entry.get()
                    .takeWhile { it > 0 }
                    .fold("") {acc, b -> acc + b.toChar()}

                if (bufferString.endsWith('\n') || posixApi.feof(filePointer) != 0) {
                    consumer(bufferString.trimEnd('\r', '\n'))
                    bufferString = ""
                }
            }
        }

        posixApi.fclose(filePointer)
    }
}