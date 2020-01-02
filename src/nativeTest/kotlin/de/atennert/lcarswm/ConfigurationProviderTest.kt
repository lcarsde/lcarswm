package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

        val configurationProvider = ConfigurationProvider(systemApi, "my-config.properties")

        val posixCalls = systemApi.functionCalls
        val openFileCall = posixCalls.removeAt(0)
        assertEquals("fopen", openFileCall.name, "The config file needs to be opened")
        assertEquals("my-config.properties", openFileCall.parameters[0], "The opened file should be the given one")
        assertEquals("r", openFileCall.parameters[1], "The file should be opened read only")

        val closeFileCall = posixCalls.removeAt(0)
        assertEquals("fclose", closeFileCall.name, "The opened file needs to be closed")

        assertEquals("longValue1", configurationProvider["property1"], "The configuration provider should provide given properties")
        assertEquals("value2", configurationProvider["property2"], "The configuration provider should provide given properties")

        assertNull(configurationProvider["propertyNone"], "The configuration provider should return null on unknown properties")
    }
}