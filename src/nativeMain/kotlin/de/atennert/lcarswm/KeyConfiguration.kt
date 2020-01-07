package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import kotlinx.cinterop.convert
import xlib.KeySym

class KeyConfiguration(inputApi: InputApi, configurationProvider: Properties, private val keyManager: KeyManager) {

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
            val keyConfigParts = keyConfig.split('+')

            val mask = if (keyConfigParts.size > 1) {
                val modifier = modKeyBindings.entries
                    .single { it.value.contains(keyConfigParts.first()) }
                    .key
                keyManager.modMasks.getOrElse(modifier) { 0 }
            } else {
                0
            }

            val keySym = inputApi.stringToKeysym(keyConfigParts.last())

            keySymCommands[Pair(keySym, mask)] = configurationProvider[keyConfig]!!
        }
    }

    fun getCommandForKey(keySym: KeySym, keyMask: UInt): String? {
        return keySymCommands[Pair(keySym, filterMask(keyMask))]
    }

    private fun filterMask(keyMask: UInt): Int {
        var filteredMask = keyMask.convert<Int>() and 0xFF
        filteredMask = filteredMask and keyManager.modMasks.getOrElse(Modifiers.CAPS_LOCK, {0}).inv()
        filteredMask = filteredMask and keyManager.modMasks.getOrElse(Modifiers.NUM_LOCK, {0}).inv()
        filteredMask = filteredMask and keyManager.modMasks.getOrElse(Modifiers.SCROLL_LOCK, {0}).inv()
        return filteredMask
    }
}
