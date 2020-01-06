package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.convert
import xlib.AnyModifier
import xlib.ControlMask
import xlib.LockMask
import xlib.ShiftMask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyManagerTest {
    @Test
    fun `load modifier keys`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        val expectedModifiers = mutableMapOf(
            Pair(Modifiers.CAPS_LOCK, LockMask),
            Pair(Modifiers.SHIFT, ShiftMask),
            Pair(Modifiers.CONTROL, ControlMask),
            Pair(Modifiers.ALT, 0x8),
            Pair(Modifiers.HYPER, 0x10),
            Pair(Modifiers.META, 0x20),
            Pair(Modifiers.NUM_LOCK, 0x40),
            Pair(Modifiers.SCROLL_LOCK, 0x80)
        )

        assertEquals(expectedModifiers, keyManager.modMasks, "The KeyManager should get the required modifier keys")
    }

    @Test
    fun `grab input controls`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        keyManager.grabInputControls()

        val inputCalls = systemApi.functionCalls

        LCARS_WM_KEY_SYMS
            .filterNot { systemApi.keySyms[it] == 0 } // 0s are not available
            .onEach { keySym ->
                val grabKeyCall = inputCalls.removeAt(0)
                assertEquals("grabKey", grabKeyCall.name, "The modifier key needs to be grabbed")
                assertEquals(
                    systemApi.keySyms[keySym],
                    grabKeyCall.parameters[0],
                    "The key needs to be ${systemApi.keySyms[keySym]}"
                )
                assertEquals(
                    WM_MODIFIER_KEY.toUInt(),
                    grabKeyCall.parameters[1],
                    "The modifier key needs to be $WM_MODIFIER_KEY"
                )
                assertEquals(
                    systemApi.rootWindowId,
                    grabKeyCall.parameters[2],
                    "The key needs to be grabbed for the root window"
                )
            }
            .forEach { keySym ->
                assertEquals(
                    keySym,
                    keyManager.getKeySym(systemApi.keySyms.getValue(keySym).convert())!!.convert()
                )
            }

        LCARS_NO_MASK_KEY_SYMS
            .filterNot { systemApi.keySyms[it] == 0 } // 0s are not available
            .onEach { keySym ->
                val grabKeyCall = inputCalls.removeAt(0)
                assertEquals("grabKey", grabKeyCall.name, "The modifier key needs to be grabbed")
                assertEquals(
                    systemApi.keySyms[keySym],
                    grabKeyCall.parameters[0],
                    "The key needs to be ${systemApi.keySyms[keySym]}"
                )
                assertEquals(
                    AnyModifier.toUInt(),
                    grabKeyCall.parameters[1],
                    "The modifier key needs to be any"
                )
                assertEquals(
                    systemApi.rootWindowId,
                    grabKeyCall.parameters[2],
                    "The key needs to be grabbed for the root window"
                )
            }
            .forEach { keySym ->
                assertEquals(
                    keySym,
                    keyManager.getKeySym(systemApi.keySyms.getValue(keySym).convert())!!.convert()
                )
            }
    }

    @Test
    fun `return null on unknown key`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        assertNull(keyManager.getKeySym((-1).convert()), "The key manager should return null for unknown key codes")
    }
}