package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.convert
import xlib.XK_A
import xlib.XK_B
import xlib.XK_C
import xlib.XK_X
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyConfigurationTest {
    @Test
    fun `load simple key configuration`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        val configurationProvider = object : Properties {
            override fun get(propertyKey: String): String? {
                return when (propertyKey) {
                    "A" -> "commandA"
                    "B" -> "commandB"
                    "X" -> "commandX"
                    else -> error("unknown key configuration: $propertyKey")
                }
            }

            override fun getProperyNames(): Set<String> {
                return setOf("A", "B", "X")
            }
        }
        val keyConfiguration = KeyConfiguration(systemApi, configurationProvider, keyManager)

        assertEquals(
            "commandA",
            keyConfiguration.getCommandForKey(XK_A.convert(), 0.convert()),
            "The config should load the first key binding"
        )
        assertEquals(
            "commandB",
            keyConfiguration.getCommandForKey(XK_B.convert(), 0.convert()),
            "The config should load the second key binding"
        )
        assertEquals(
            "commandX",
            keyConfiguration.getCommandForKey(XK_X.convert(), 0.convert()),
            "The config should load the third key binding"
        )

        assertNull(
            keyConfiguration.getCommandForKey(XK_C.convert(), 0.convert()),
            "The config should not provide an unknown key binding"
        )
    }

    @Test
    fun `load key config including one modifier`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        val configurationProvider = object : Properties {
            override fun get(propertyKey: String): String? {
                return when (propertyKey) {
                    "A" -> "commandA"
                    "Ctrl+B" -> "commandB"
                    "Alt+X" -> "commandX"
                    else -> error("unknown key configuration: $propertyKey")
                }
            }

            override fun getProperyNames(): Set<String> {
                return setOf("A", "Ctrl+B", "Alt+X")
            }
        }
        val keyConfiguration = KeyConfiguration(systemApi, configurationProvider, keyManager)

        assertEquals(
            "commandA",
            keyConfiguration.getCommandForKey(XK_A.convert(), 0.convert()),
            "The config should load the first key binding"
        )
        assertEquals(
            "commandB",
            keyConfiguration.getCommandForKey(XK_B.convert(), keyManager.modMasks.getValue(Modifiers.CONTROL).convert()),
            "The config should load the second key binding"
        )
        assertEquals(
            "commandX",
            keyConfiguration.getCommandForKey(XK_X.convert(), keyManager.modMasks.getValue(Modifiers.ALT).convert()),
            "The config should load the third key binding"
        )
    }

    // TODO two modifier key config

    // TODO three modifier key config
}