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
        val keyManager = KeyManager(systemApi)

        systemApi.functionCalls.clear()

        val keySetting = setOf(
            KeyExecution("A", "commandA"),
            KeyExecution("B", "commandB"),
            KeyExecution("X", "commandX")
        )

        val keyConfiguration = KeyConfiguration(systemApi, keySetting, keyManager, systemApi.rootWindowId)

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

        keySetting.forEach { setting -> checkGrabKey(systemApi, setting.keys, emptyList()) }
    }

    @Test
    fun `load key config including one modifier`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi)
        systemApi.functionCalls.clear()

        val keySetting = setOf(
            KeyExecution("A", "commandA"),
            KeyExecution("Ctrl+B", "commandB"),
            KeyExecution("Alt+X", "commandX")
        )
        val keyConfiguration = KeyConfiguration(systemApi, keySetting, keyManager, systemApi.rootWindowId)

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

        keySetting
            .zip(listOf(emptyList(), listOf(Modifiers.CONTROL), listOf(Modifiers.ALT)))
            .forEach { (setting, modifiers) ->
                checkGrabKey(
                    systemApi,
                    setting.keys,
                    modifiers
                )
            }
    }

    @Test
    fun `load key config with multiple modifiers`() {
        val systemApi = SystemFacadeMock()
        val keyManager = KeyManager(systemApi)
        systemApi.functionCalls.clear()

        val keySetting = setOf(
            KeyExecution("Ctrl+Alt+A", "commandA"),
            KeyExecution("Win+Shift+Meta+B", "commandB"),
            KeyExecution("Hyper+Super+X", "commandX")
        )
        val keyConfiguration = KeyConfiguration(systemApi, keySetting, keyManager, systemApi.rootWindowId)

        assertEquals(
            "commandA",
            keyConfiguration.getCommandForKey(
                XK_A.convert(),
                getMask(
                    listOf(Modifiers.CONTROL, Modifiers.ALT)
                )
            ),
            "The config should load the first key binding"
        )
        assertEquals(
            "commandB",
            keyConfiguration.getCommandForKey(
                XK_B.convert(),
                getMask(listOf(Modifiers.SUPER, Modifiers.SHIFT, Modifiers.META))
            ),
            "The config should load the second key binding"
        )
        assertEquals(
            "commandX",
            keyConfiguration.getCommandForKey(
                XK_X.convert(),
                getMask(listOf(Modifiers.SUPER, Modifiers.HYPER))
            ),
            "The config should load the third key binding"
        )

        keySetting
            .zip(listOf(
                listOf(Modifiers.CONTROL, Modifiers.ALT),
                listOf(Modifiers.SUPER, Modifiers.SHIFT, Modifiers.META),
                listOf(Modifiers.HYPER, Modifiers.SUPER)))
            .forEach { (setting, modifiers) ->
                checkGrabKey(
                    systemApi,
                    setting.keys,
                    modifiers
                )
            }
    }
}