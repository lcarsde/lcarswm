package de.atennert.lcarswm.settings

import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import platform.posix.FILE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigurationProviderTest {
    @Test
    fun `load a configuration`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getLines(fileName: String): List<String> {
                return if (fileName == "my-config.properties") {
                    listOf("property1=longValue1\n", "property2=value2\n")
                } else {
                    emptyList()
                }
            }
        }

        val propertiesReader =
            PropertiesReader(systemApi, "my-config.properties")

        val posixCalls = systemApi.functionCalls
        val openFileCall = posixCalls.removeAt(0)
        assertEquals("fopen", openFileCall.name, "The config file needs to be opened")
        assertEquals("my-config.properties", openFileCall.parameters[0], "The opened file should be the given one")
        assertEquals("r", openFileCall.parameters[1], "The file should be opened read only")

        val closeFileCall = posixCalls.removeAt(0)
        assertEquals("fclose", closeFileCall.name, "The opened file needs to be closed")

        assertEquals("longValue1", propertiesReader["property1"], "The configuration provider should provide given properties")
        assertEquals("value2", propertiesReader["property2"], "The configuration provider should provide given properties")

        assertNull(propertiesReader["propertyNone"], "The configuration provider should return null on unknown properties")
    }

    @Test
    fun `read loooong entries`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getLines(fileName: String): List<String> {
                return if (fileName == "my-config.properties") {
                    listOf("property1=looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongValue1\n",
                        "property2=looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongValue2")
                } else {
                    emptyList()
                }
            }
        }

        val propertiesReader =
            PropertiesReader(systemApi, "my-config.properties")

        assertEquals("looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongValue1", propertiesReader["property1"], "The configuration provider should read loooooong entries with end of line")

        assertEquals("looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongValue2", propertiesReader["property2"], "The configuration provider should read loooooong entries with end of file")
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

        PropertiesReader(systemApi, "my-config.properties")

        assertTrue(systemApi.functionCalls.isEmpty(), "There should be no further calls to the system API")
    }

    @Test
    fun `get property names`() {
        val systemApi = object : SystemFacadeMock() {
            override fun getLines(fileName: String): List<String> {
                return listOf("property1=longValue1\n", "property2=value2\n")
            }
        }

        val propertiesReader =
            PropertiesReader(systemApi, "my-config.properties")

        assertEquals(setOf("property1", "property2"), propertiesReader.getPropertyNames(), "The provider should return the names of the available properties")
    }
}