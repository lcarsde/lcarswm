package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.convert
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class KeyConfigurationTest {
    @Test
    fun `load simple key configuration`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi)

        systemApi.functionCalls.clear()

        val configurationProvider = object : Properties {
            override fun get(propertyKey: String): String? {
                return when (propertyKey) {
                    "A" -> "commandA"
                    "B" -> "commandB"
                    "X" -> "commandX"
                    else -> error("unknown key configuration: $propertyKey")
                }
            }

            override fun getPropertyNames(): Set<String> {
                return setOf("A", "B", "X")
            }
        }
        val keyConfiguration = KeyConfiguration(systemApi, configurationProvider, keyManager, systemApi.rootWindowId)

        assertEquals(
            "commandA",
            keyConfiguration.getCommandForKey(XK_A.convert(), 0),
            "The config should load the first key binding"
        )
        assertEquals(
            "commandB",
            keyConfiguration.getCommandForKey(XK_B.convert(), 0),
            "The config should load the second key binding"
        )
        assertEquals(
            "commandX",
            keyConfiguration.getCommandForKey(XK_X.convert(), 0),
            "The config should load the third key binding"
        )

        assertNull(
            keyConfiguration.getCommandForKey(XK_C.convert(), 0),
            "The config should not provide an unknown key binding"
        )

        configurationProvider.getPropertyNames()
            .forEach { key -> checkGrabKey(systemApi, keyManager, key, emptyList()) }
    }

    @Test
    fun `load key config including one modifier`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi)
        systemApi.functionCalls.clear()

        val configurationProvider = object : Properties {
            override fun get(propertyKey: String): String? {
                return when (propertyKey) {
                    "A" -> "commandA"
                    "Ctrl+B" -> "commandB"
                    "Alt+X" -> "commandX"
                    else -> error("unknown key configuration: $propertyKey")
                }
            }

            override fun getPropertyNames(): Set<String> {
                return setOf("A", "Ctrl+B", "Alt+X")
            }
        }
        val keyConfiguration = KeyConfiguration(systemApi, configurationProvider, keyManager, systemApi.rootWindowId)

        assertEquals(
            "commandA",
            keyConfiguration.getCommandForKey(XK_A.convert(), 0),
            "The config should load the first key binding"
        )
        assertEquals(
            "commandB",
            keyConfiguration.getCommandForKey(
                XK_B.convert(),
                keyManager.modMasks.getValue(Modifiers.CONTROL)
            ),
            "The config should load the second key binding"
        )
        assertEquals(
            "commandX",
            keyConfiguration.getCommandForKey(XK_X.convert(), keyManager.modMasks.getValue(Modifiers.ALT)),
            "The config should load the third key binding"
        )

        configurationProvider.getPropertyNames()
            .zip(listOf(emptyList(), listOf(Modifiers.CONTROL), listOf(Modifiers.ALT)))
            .forEach { (key, modifiers) -> checkGrabKey(systemApi, keyManager, key, modifiers) }
    }

    @Test
    fun `load key config with multiple modifiers`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi)
        systemApi.functionCalls.clear()

        val configurationProvider = object : Properties {
            override fun get(propertyKey: String): String? {
                return when (propertyKey) {
                    "Ctrl+Alt+A" -> "commandA"
                    "Win+Shift+Meta+B" -> "commandB"
                    "Hyper+Super+X" -> "commandX"
                    else -> error("unknown key configuration: $propertyKey")
                }
            }

            override fun getPropertyNames(): Set<String> {
                return setOf("Ctrl+Alt+A", "Win+Shift+Meta+B", "Hyper+Super+X")
            }
        }
        val keyConfiguration = KeyConfiguration(systemApi, configurationProvider, keyManager, systemApi.rootWindowId)

        assertEquals(
            "commandA",
            keyConfiguration.getCommandForKey(
                XK_A.convert(),
                getMask(
                    keyManager,
                    listOf(Modifiers.CONTROL, Modifiers.ALT)
                )
            ),
            "The config should load the first key binding"
        )
        assertEquals(
            "commandB",
            keyConfiguration.getCommandForKey(
                XK_B.convert(),
                getMask(keyManager, listOf(Modifiers.SUPER, Modifiers.SHIFT, Modifiers.META))
            ),
            "The config should load the second key binding"
        )
        assertEquals(
            "commandX",
            keyConfiguration.getCommandForKey(
                XK_X.convert(),
                getMask(keyManager, listOf(Modifiers.SUPER, Modifiers.HYPER))
            ),
            "The config should load the third key binding"
        )

        configurationProvider.getPropertyNames()
            .zip(listOf(
                listOf(Modifiers.CONTROL, Modifiers.ALT),
                listOf(Modifiers.SUPER, Modifiers.SHIFT, Modifiers.META),
                listOf(Modifiers.HYPER, Modifiers.SUPER)))
            .forEach { (key, modifiers) -> checkGrabKey(systemApi, keyManager, key, modifiers) }
    }

    private fun getMask(keyManager: KeyManager, l: List<Modifiers>): Int {
        return l.fold(0) { acc, m ->
            acc or keyManager.modMasks.getValue(m)
        }
    }

    private fun checkGrabKey(
        systemApi: SystemFacadeMock,
        keyManager: KeyManager,
        key: String,
        modifiers: List<Modifiers>
    ) {
        val keyPart = key.split('+').last()
        val grabKeyCall1 = systemApi.functionCalls.removeAt(0)

        assertEquals("grabKey", grabKeyCall1.name, "Grab key needs to be called to grab $key with ...")
        assertEquals(systemApi.keySyms[systemApi.keyStrings[keyPart]], grabKeyCall1.parameters[0], "Grab key needs to be called with the keyCode for $key (modifier ...)")
        assertEquals(getMask(keyManager, modifiers).toUInt(), grabKeyCall1.parameters[1], "The modifier for $key should be ...")
        assertEquals(systemApi.rootWindowId, grabKeyCall1.parameters[2], "The key should be grabbed for the root window")
        assertEquals(GrabModeAsync, grabKeyCall1.parameters[3], "The mode for the grabbed key should be GrabModeAsync")
    }

    @Test
    fun `reload configuration`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi)

        systemApi.functionCalls.clear()

        val configurationProvider = object : Properties {
            val changingEntries = listOf("A", "B")
            var entryIndex = -1

            override fun get(propertyKey: String): String? {
                if (propertyKey == changingEntries[entryIndex]) {
                    return "command${changingEntries[entryIndex]}"
                } else {
                    fail("unknown propertyKey $propertyKey")
                }
            }

            override fun getPropertyNames(): Set<String> {
                ++entryIndex
                return setOf(changingEntries[entryIndex])
            }
        }
        val keyConfiguration = KeyConfiguration(systemApi, configurationProvider, keyManager, systemApi.rootWindowId)

        assertEquals(
            "commandA",
            keyConfiguration.getCommandForKey(XK_A.convert(), 0),
            "The config should load the first key binding"
        )

        keyConfiguration.reloadConfig()

        assertEquals(
            "commandB",
            keyConfiguration.getCommandForKey(XK_B.convert(), 0),
            "The config should load the second key binding"
        )
    }
}