package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import kotlinx.cinterop.convert
import xlib.KeySym

class KeyConfiguration(
    private val inputApi: InputApi,
    configurationProvider: Properties,
    private val keyManager: KeyManager
) {

    private val keySymCommands = mutableMapOf<Pair<KeySym, Int>, String>()

    private val modKeyBindings = mapOf(
        Pair(Modifiers.CONTROL, setOf("Ctrl")),
        Pair(Modifiers.SHIFT, setOf("Shift")),
        Pair(Modifiers.ALT, setOf("Alt")),
        Pair(Modifiers.SUPER, setOf("Super", "Win", "Lin")),
        Pair(Modifiers.META, setOf("Meta")),
        Pair(Modifiers.HYPER, setOf("Hyper"))
    )

    init {
        for (keyConfig in configurationProvider.getProperyNames()) {
            val (modifierStrings, keyString) = separateKeySymAndModifiers(keyConfig)

            val mask = getMask(modifierStrings)
            val keySym = getKeySym(keyString)

            keySymCommands[Pair(keySym, mask)] = configurationProvider[keyConfig]!!
        }
    }

    private fun separateKeySymAndModifiers(keyConfig: String): Pair<List<String>, String> {
        val keyStrings = keyConfig.split('+')
        return Pair(keyStrings.dropLast(1), keyStrings.last())
    }

    private fun getMask(modifierStrings: List<String>): Int {
        return if (modifierStrings.isNotEmpty()) {
            val modifier = modKeyBindings.entries
                .single { it.value.contains(modifierStrings.first()) }
                .key
            keyManager.modMasks.getOrElse(modifier) { 0 }
        } else {
            0
        }
    }

    private fun getKeySym(keyString: String): KeySym {
        return inputApi.stringToKeysym(keyString)
    }

    fun getCommandForKey(keySym: KeySym, keyMask: UInt): String? {
        return keySymCommands[Pair(keySym, filterMask(keyMask))]
    }

    private fun filterMask(keyMask: UInt): Int {
        var filteredMask = keyMask.convert<Int>() and 0xFF
        filteredMask = filteredMask and keyManager.modMasks.getOrElse(Modifiers.CAPS_LOCK, { 0 }).inv()
        filteredMask = filteredMask and keyManager.modMasks.getOrElse(Modifiers.NUM_LOCK, { 0 }).inv()
        filteredMask = filteredMask and keyManager.modMasks.getOrElse(Modifiers.SCROLL_LOCK, { 0 }).inv()
        return filteredMask
    }
}
