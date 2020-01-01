package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigurationProviderTest {
    @Test
    fun `load a configuration`() {
        val systemApi = SystemFacadeMock()

        val configurationProvider = ConfigurationProvider(systemApi, "my-config.properties")

        val posixCalls = systemApi.functionCalls
        val openFileCall = posixCalls.removeAt(0)


        val closeFileCall = posixCalls.removeAt(0)

//        assertEquals("value1", configurationProvider["property1"], "The configuration provider should provide given properties")

//        assertNull(configurationProvider["propertyNone"], "The configuration provider should return null on unknown properties")
    }
}