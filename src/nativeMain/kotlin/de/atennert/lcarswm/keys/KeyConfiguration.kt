package de.atennert.lcarswm.keys

import de.atennert.lcarswm.system.api.InputApi
import xlib.AnyModifier
import xlib.KeySym
import xlib.Window

/**
 * Loads the key configuration of the users and provides the corresponding key bindings.
 */
class KeyConfiguration(
    private val inputApi: InputApi,
    private val keyConfiguration: Set<KeyBinding>,
    private val keyManager: KeyManager,
    private val rootWindowId: Window
) {

    private val keySymCommands = mutableMapOf<Pair<KeySym, Int>, KeyBinding>()

    private val modKeyBindings = mapOf(
        Pair(Modifiers.CONTROL, setOf("Ctrl")),
        Pair(Modifiers.SHIFT, setOf("Shift")),
        Pair(Modifiers.ALT, setOf("Alt")),
        Pair(Modifiers.SUPER, setOf("Super", "Win", "Lin")),
        Pair(Modifiers.META, setOf("Meta")),
        Pair(Modifiers.HYPER, setOf("Hyper"))
    )

    private val windowManagerActions = setOf(
        "window-toggle-forward",
        "window-move-up",
        "window-move-down",
        "window-close",
        "screen-mode-toggle",
        "lcarswm-quit"
    )

    init {
        reloadConfig()
    }

    fun reloadConfig() {
        for (keyBinding in keyConfiguration) {
            val (modifierStrings, keyString) = separateKeySymAndModifiers(keyBinding.keys)

            val mask = getMask(modifierStrings)
            val grabbedMask = if (mask == 0) AnyModifier else mask
            val keySym = getKeySym(keyString)

            keyManager.grabKey(keySym, grabbedMask, rootWindowId)

            keySymCommands[Pair(keySym, mask)] = keyBinding
        }
    }

    private fun separateKeySymAndModifiers(keyConfig: String): Pair<List<String>, String> {
        val keyStrings = keyConfig.split('+')
        return Pair(keyStrings.dropLast(1), keyStrings.last())
    }

    private fun getMask(modifierStrings: List<String>): Int {
        return modifierStrings.fold(0) { acc, modifierString ->
            val modifier = modKeyBindings.entries
                .single { it.value.contains(modifierString) }
                .key
            acc or keyManager.modMasks.getValue(modifier)
        }
    }

    private fun getKeySym(keyString: String): KeySym {
        return inputApi.stringToKeysym(keyString)
    }

    /**
     * @return command for a key binding consisting of key sym and key mask. null if there's no command registered for the given key sym+key mask
     */
    fun getBindingForKey(keySym: KeySym, keyMask: Int): KeyBinding? {
        return keySymCommands[Pair(keySym, keyMask)]
    }
}
