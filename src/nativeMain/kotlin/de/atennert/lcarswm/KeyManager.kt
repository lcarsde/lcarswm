package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import xlib.*

/**
 * This class handles the registration of keys for keyboard commands.
 */
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

    private val grabbedKeys = mutableMapOf<KeyCode, KeySym>()

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
        grabModifiedKeys()
        grabUnmodifiedKeys()
    }

    private fun grabModifierKeys() {
        modifiers.forEach { keyCode ->
            inputApi.grabKey(keyCode.convert(), AnyModifier.convert(), rootWindowId, GrabModeAsync)
        }
    }

    private fun grabModifiedKeys() {
        grabKeysForKeySyms(LCARS_WM_KEY_SYMS, WM_MODIFIER_KEY)
    }

    private fun grabUnmodifiedKeys() {
        grabKeysForKeySyms(LCARS_NO_MASK_KEY_SYMS, AnyModifier)
    }

    private fun grabKeysForKeySyms(keySyms: List<Int>, modifierKey: Int) {
        keySyms.map { keySym -> Pair(keySym, inputApi.keysymToKeycode(keySym.convert())) }
            .filterNot { (_, keyCode) -> keyCode.convert<Int>() == 0 }
            .onEach { (keySym, keyCode) -> grabbedKeys[keyCode] = keySym.convert() }
            .forEach { (_, keyCode) ->
                inputApi.grabKey(keyCode.convert(), modifierKey.convert(), rootWindowId, GrabModeAsync)
            }
    }

    fun getKeySym(keyCode: KeyCode): KeySym? = grabbedKeys[keyCode]
}
