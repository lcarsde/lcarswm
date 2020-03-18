package de.atennert.lcarswm.events

import de.atennert.lcarswm.KeyConfiguration
import de.atennert.lcarswm.KeyManager
import de.atennert.lcarswm.Properties
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.SystemFacadeMock
import xlib.MappingNotify
import kotlin.test.Test
import kotlin.test.assertEquals

class MappingNotifyHandlerTest {

    private val configurationProvider = object : Properties {
        override fun get(propertyKey: String): String? {
            return when (propertyKey) {
                "Ctrl+F4" -> "command arg1 arg2"
                else -> error("unknown key configuration: $propertyKey")
            }
        }

        override fun getPropertyNames(): Set<String> {
            return setOf("Ctrl+F4")
        }
    }

    @Test
    fun `return the event type MappingNotify`() {
        val system = SystemFacadeMock()
        val keyManager = KeyManager(system)
        val keyConfiguration = KeyConfiguration(system, configurationProvider, keyManager, system.rootWindowId)
        val mappingNotifyHandler = MappingNotifyHandler(LoggerMock(), keyManager, keyConfiguration, system.rootWindowId)

        assertEquals(MappingNotify, mappingNotifyHandler.xEventType, "The MappingNotifyHandler should have the correct type")
    }
}