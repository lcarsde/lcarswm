package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import kotlinx.cinterop.*
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

    val modMasks = getAllModifierKeys()

    private val grabbedKeys = mutableMapOf<KeyCode, KeySym>()

    private fun getAllModifierKeys(): Map<Modifiers, Int> {
        val modifierKeymap = inputApi.getModifierMapping()?.pointed ?: return emptyMap()
        val (minKeyCodes, maxKeyCodes) = inputApi.getDisplayKeyCodeMinMaxCounts()

        val keySymsPerKeyCode = IntArray(1)
        val keymap = inputApi.getKeyboardMapping(
            minKeyCodes.convert(),
            (maxKeyCodes - minKeyCodes + 1).convert(),
            keySymsPerKeyCode.pin().addressOf(0))!!

        var superLUsed = false
        var hyperLUsed = false
        var altLUsed = false
        var metaLUsed = false

        val modifierMasks = mutableMapOf(
            Pair(Modifiers.CAPS_LOCK, LockMask),
            Pair(Modifiers.SHIFT, ShiftMask),
            Pair(Modifiers.CONTROL, ControlMask)
        )

        for (i in modifierIndexes.indices) {
            val mask = 1.shl(i)
            for (j in 0 until modifierKeymap.max_keypermod) {
                val keyCode = modifierKeymap.modifiermap!![i * modifierKeymap.max_keypermod + j].convert<Int>()
                if (keyCode != 0) {
                    for (k in 0 until keySymsPerKeyCode[0]) {
                        val keySym = keymap[(keyCode - minKeyCodes) * keySymsPerKeyCode[0] + k]
                        if (keySym.convert<Long>() != NoSymbol) {
                            when (keySym.convert<Int>()) {
                                XK_Num_Lock -> modifierMasks[Modifiers.NUM_LOCK] =
                                    modifierMasks.getOrElse(Modifiers.NUM_LOCK, {0}).or(mask)
                                XK_Scroll_Lock -> modifierMasks[Modifiers.SCROLL_LOCK] =
                                    modifierMasks.getOrElse(Modifiers.SCROLL_LOCK, {0}).or(mask)

                                XK_Super_L -> modifierMasks[Modifiers.SUPER] = if (superLUsed) {
                                    modifierMasks.getOrElse(Modifiers.SUPER, {0}).or(mask)
                                } else {
                                    superLUsed = true
                                    mask // overwrite any super-r stuff
                                }
                                XK_Super_R -> if (!superLUsed) {
                                    modifierMasks[Modifiers.SUPER] = modifierMasks.getOrElse(Modifiers.SUPER, {0}).or(mask)
                                }

                                XK_Hyper_L -> modifierMasks[Modifiers.HYPER] = if (hyperLUsed) {
                                    modifierMasks.getOrElse(Modifiers.HYPER, {0}).or(mask)
                                } else {
                                    hyperLUsed = true
                                    mask // overwrite any hyper-r stuff
                                }
                                XK_Hyper_R -> if (!hyperLUsed) {
                                    modifierMasks[Modifiers.HYPER] = modifierMasks.getOrElse(Modifiers.HYPER, {0}).or(mask)
                                }

                                XK_Alt_L -> modifierMasks[Modifiers.ALT] = if (altLUsed) {
                                    modifierMasks.getOrElse(Modifiers.ALT, {0}).or(mask)
                                } else {
                                    altLUsed = true
                                    mask // overwrite any alt-r stuff
                                }
                                XK_Alt_R -> if (!altLUsed) {
                                    modifierMasks[Modifiers.ALT] = modifierMasks.getOrElse(Modifiers.ALT, {0}).or(mask)
                                }

                                XK_Meta_L -> modifierMasks[Modifiers.META] = if (metaLUsed) {
                                    modifierMasks.getOrElse(Modifiers.META, {0}).or(mask)
                                } else {
                                    metaLUsed = true
                                    mask // overwrite any meta-r stuff
                                }
                                XK_Meta_R -> if (!metaLUsed) {
                                    modifierMasks[Modifiers.META] = modifierMasks.getOrElse(Modifiers.META, {0}).or(mask)
                                }
                            }
                        }
                    }
                }
            }
        }

        return modifierMasks
    }

    fun grabInputControls() {
//        grabModifierKeys()
        grabModifiedKeys()
        grabUnmodifiedKeys()
    }

//    private fun grabModifierKeys() {
//        modifiers.forEach { keyCode ->
//            inputApi.grabKey(keyCode.convert(), AnyModifier.convert(), rootWindowId, GrabModeAsync)
//        }
//    }

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
