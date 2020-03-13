package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import kotlinx.cinterop.*
import xlib.*

/**
 * This class handles the registration of keys for keyboard commands.
 */
class KeyManager(private val inputApi: InputApi) {
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

    private var modifierKeymapReference: CPointer<XModifierKeymap>? = null

    private var keymap: CPointer<KeySymVar>? = null

    private val grabbedKeys = mutableMapOf<KeyCode, KeySym>()

    private val grabbedKeyCombos = mutableListOf<Pair<KeyCode, Int>>()

    var modMasks = getAllModifierKeys()
        private set

    val filteredModifierCombinations = mutableListOf<Int>()

    private fun getAllModifierKeys(): Map<Modifiers, Int> {
        modifierKeymapReference = inputApi.getModifierMapping()
        val modifierKeymap = modifierKeymapReference?.pointed ?: return emptyMap()
        val (minKeyCodes, maxKeyCodes) = inputApi.getDisplayKeyCodeMinMaxCounts()

        val keySymsPerKeyCode = IntArray(1)
        keymap = inputApi.getKeyboardMapping(
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
                        val keySym = keymap!![(keyCode - minKeyCodes) * keySymsPerKeyCode[0] + k]
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

        val num = modifierMasks[Modifiers.NUM_LOCK]!!
        val caps = modifierMasks[Modifiers.CAPS_LOCK]!!
        val scroll = modifierMasks[Modifiers.SCROLL_LOCK]!!
        filteredModifierCombinations.clear()
        filteredModifierCombinations.add(0)
        filteredModifierCombinations.add(num)
        filteredModifierCombinations.add(caps)
        filteredModifierCombinations.add(scroll)
        filteredModifierCombinations.add(num or caps)
        filteredModifierCombinations.add(num or scroll)
        filteredModifierCombinations.add(caps or scroll)
        filteredModifierCombinations.add(num or caps or scroll)

        return modifierMasks
    }

    fun ungrabAllKeys(rootWindowId: Window) {
        inputApi.ungrabKey(rootWindowId)
        grabbedKeys.clear()
        grabbedKeyCombos.clear()
    }

    fun reloadConfig() {
        cleanup()
        modMasks = getAllModifierKeys()
    }

    /**
     * Grab the internal keys from the XServer.
     */
    fun grabInternalKeys(rootWindowId: Window) {
        grabKeysForKeySyms(LCARS_WM_KEY_SYMS, modMasks.getValue(Modifiers.SUPER), rootWindowId)
    }

    private fun grabKeysForKeySyms(keySyms: List<Int>, modifierKey: Int, rootWindowId: Window) {
        keySyms.map { keySym -> Pair(keySym, inputApi.keysymToKeycode(keySym.convert())) }
            .filterNot { (_, keyCode) -> keyCode.convert<Int>() == 0 }
            .onEach { (keySym, keyCode) -> grabbedKeys[keyCode] = keySym.convert() }
            .onEach { (_, keyCode) -> grabbedKeyCombos.add(Pair(keyCode, modifierKey)) }
            .forEach { (_, keyCode) ->
                filteredModifierCombinations.forEach {
                    inputApi.grabKey(keyCode.convert(), (modifierKey or it).convert(), rootWindowId, GrabModeAsync)
                }
            }
    }

    /**
     * Grab a key binding consisting of a key sym and a key modifier
     */
    fun grabKey(keySym: KeySym, modifiers: Int, rootWindowId: Window) {
        val keyCode = inputApi.keysymToKeycode(keySym)

        if (keyCode.convert<Int>() != 0) {
            grabbedKeys[keyCode] = keySym
            grabbedKeyCombos.add(Pair(keyCode, modifiers))
            filteredModifierCombinations.forEach {
                inputApi.grabKey(keyCode.convert(), (modifiers or it).convert(), rootWindowId, GrabModeAsync)
            }
        }
    }

    /**
     * @return key sym for a given key code
     */
    fun getKeySym(keyCode: KeyCode): KeySym? = grabbedKeys[keyCode]

    /**
     * Cleanup acquired X data
     */
    fun cleanup() {
        if (modifierKeymapReference != null) {
            inputApi.freeModifiermap(modifierKeymapReference)
            modifierKeymapReference = null
        }

        if (keymap != null) {
            inputApi.free(keymap)
            keymap = null
        }
    }

    fun filterMask(keyMask: UInt): Int {
        var filteredMask = keyMask.convert<Int>() and 0xFF
        filteredMask = filteredMask and modMasks.getOrElse(Modifiers.CAPS_LOCK, { 0 }).inv()
        filteredMask = filteredMask and modMasks.getOrElse(Modifiers.NUM_LOCK, { 0 }).inv()
        filteredMask = filteredMask and modMasks.getOrElse(Modifiers.SCROLL_LOCK, { 0 }).inv()
        return filteredMask
    }
}
