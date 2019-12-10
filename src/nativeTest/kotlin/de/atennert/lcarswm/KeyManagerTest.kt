package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
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
}