package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
import xlib.AnyModifier
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyManagerTest {
    @Test
    fun `register modifier keys`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        assertEquals(
            listOf(systemApi.modifiers[6]), keyManager.modifiers,
            "The KeyManager should get the required modifier keys"
        )
    }

    @Test
    fun `grab input controls`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        keyManager.grabInputControls()

        val inputCalls = systemApi.functionCalls

        val grabModifierKeyCall = inputCalls.removeAt(0)
        assertEquals("grabKey", grabModifierKeyCall.name, "The modifier key needs to be grabbed")
        assertEquals(systemApi.modifiers[6].toInt(), grabModifierKeyCall.parameters[0], "The modifier key needs to be grabbed")
        assertEquals(AnyModifier.toUInt(), grabModifierKeyCall.parameters[1], "The modifier modifier key needs to be any")
        assertEquals(systemApi.rootWindowId, grabModifierKeyCall.parameters[2], "The modifier key needs to be grabbed for the root window")

        LCARS_WM_KEY_SYMS
            .filterNot { systemApi.keySyms[it] == 0 } // 0s are not available
            .onEach { keySym ->
            val grabKeyCall = inputCalls.removeAt(0)
            assertEquals("grabKey", grabKeyCall.name, "The modifier key needs to be grabbed")
            assertEquals(systemApi.keySyms[keySym], grabKeyCall.parameters[0], "The key needs to be ${systemApi.keySyms[keySym]}")
            assertEquals(WM_MODIFIER_KEY.toUInt(), grabKeyCall.parameters[1], "The modifier key needs to be $WM_MODIFIER_KEY")
            assertEquals(systemApi.rootWindowId, grabKeyCall.parameters[2], "The key needs to be grabbed for the root window")
        }
    }
}