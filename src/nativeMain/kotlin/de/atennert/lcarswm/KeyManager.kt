package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.*

class KeyManager(private val inputApi: InputApi, private val rootWindowId: Window) {
    private val modifierIndexes = arrayOf(
        ShiftMask,
        LockMask,
        ControlMask,
        Mod1Mask,
        Mod2Mask,
        Mod3Mask,
        Mod4Mask,
        Mod5Mask
    )

    val modifiers: List<UByte> = getModifierKeys(WM_MODIFIER_KEY)

    private fun getModifierKeys(modifierKey: Int): List<UByte> {
        val modifierKeymap = inputApi.getModifierMapping()?.pointed ?: return emptyList()

        val startPosition = modifierIndexes.indexOf(modifierKey) * modifierKeymap.max_keypermod
        val endPosition = startPosition + modifierKeymap.max_keypermod
        val modKeys = ArrayList<UByte>(modifierKeymap.max_keypermod)

        for (i in startPosition until endPosition) {
            modKeys.add(modifierKeymap.modifiermap!![i])
        }

        return modKeys
    }

    fun grabInputControls() {
        grabModifierKeys()
    }

    private fun grabModifierKeys() {
        modifiers.forEach { keyCode ->
            inputApi.grabKey(
                keyCode.convert(),
                AnyModifier.convert(),
                rootWindowId,
                false,
                GrabModeAsync,
                GrabModeAsync
            )
        }
    }
}
