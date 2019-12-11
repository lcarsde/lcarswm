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
    }
}