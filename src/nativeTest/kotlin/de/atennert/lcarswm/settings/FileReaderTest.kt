package de.atennert.lcarswm.settings

import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import platform.posix.FILE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileReaderTest {
    @Test
    fun `load a configuration`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getLines(fileName: String): List<String> {
                return if (fileName == "myfile") {
                    listOf("thisIsLine1\n", "thisIsLine2\n")
                } else {
                    emptyList()
                }
            }
        }

        val fileReader = FileReader(systemApi, "myfile")
        val readLines = mutableListOf<String>()
        fileReader.readLines { readLines.add(it) }

        assertEquals(systemApi.getLines("myfile"), readLines)
    }

    @Test
    fun `read loooong entries`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getLines(fileName: String): List<String> {
                return if (fileName == "myfile") {
                    listOf("looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongLine1\n",
                        "looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongLine2")
                } else {
                    emptyList()
                }
            }
        }

        val fileReader = FileReader(systemApi, "myfile")
        val readLines = mutableListOf<String>()
        fileReader.readLines { readLines.add(it) }

        assertEquals(systemApi.getLines("myfile"), readLines)
    }

    @Test
    fun `handle not existing files`() {
        val systemApi = object : SystemFacadeMock() {
            override fun fopen(fileName: String, modes: String): CPointer<FILE>? = null

            override fun fgets(buffer: CPointer<ByteVar>, bufferSize: Int, file: CPointer<FILE>): CPointer<ByteVar>? {
                functionCalls.add(FunctionCall("fgets"))
                return null
            }
        }

        val fileReader = FileReader(systemApi, "myfile")
        val readLines = mutableListOf<String>()
        fileReader.readLines { readLines.add(it) }

        assertTrue(readLines.isEmpty())
    }
}