package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.InputApi
import xlib.KeySym

class KeyConfiguration(inputApi: InputApi, configurationProvider: Properties) {

    private val keySymCommands = mutableMapOf<KeySym, String>()

    init {
        for (keyConfig in configurationProvider.getProperyNames()) {
            val keySym = inputApi.stringToKeysym(keyConfig)
            keySymCommands[keySym] = configurationProvider[keyConfig]!!
        }
    }

    fun getCommandForKey(keySym: KeySym, keyMask: Int): String? {
        return keySymCommands[keySym]
    }
}
