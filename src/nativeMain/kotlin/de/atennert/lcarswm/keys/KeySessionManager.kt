package de.atennert.lcarswm.keys

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.InputApi
import kotlinx.cinterop.convert
import xlib.*

class KeySessionManager(private val logger: Logger, private val inputApi: InputApi) {
    private val listeners = mutableListOf<KeySessionListener>()

    private var lastMask: Int = 0
    private var lastKeyCode: UInt = 0.convert()

    private var keyCodes = emptyList<KeyCode>()

    fun setModifiers(modifiers: List<Modifiers>) {
        val keySyms = mutableListOf<Int>()

        for (modifier in modifiers) {
            when (modifier) {
                Modifiers.SHIFT -> { keySyms.add(XK_Shift_L)
                    keySyms.add(XK_Shift_R)}
                Modifiers.CONTROL -> { keySyms.add(XK_Control_L)
                    keySyms.add(XK_Control_R)}
                Modifiers.SUPER -> { keySyms.add(XK_Super_L)
                    keySyms.add(XK_Super_R)}
                Modifiers.HYPER -> { keySyms.add(XK_Hyper_L)
                    keySyms.add(XK_Hyper_R)}
                Modifiers.META -> { keySyms.add(XK_Meta_L)
                    keySyms.add(XK_Meta_R)}
                Modifiers.ALT -> { keySyms.add(XK_Alt_L)
                    keySyms.add(XK_Alt_R)}
                else -> throw IllegalArgumentException("Unsupported modifier: $modifier")
            }
        }

        keyCodes = keySyms.map { inputApi.keysymToKeycode(it.convert()) }
    }

    fun addListener(listener: KeySessionListener) {
        listeners.add(listener)
    }

    fun pressKeys(keyCode: UInt, keyMask: Int) {
        val isSameSession = (keyMask == lastMask) and (keyCode == lastKeyCode)
        if (!isSameSession) {
            logger.logDebug("KeySessionManager::pressKeys::stop session")
            listeners.forEach(KeySessionListener::stopSession)
        }
        lastMask = keyMask
        lastKeyCode = keyCode
    }

    fun releaseKeys(keyCode: UInt) {
        if (keyCodes.contains(keyCode.convert())) {
            logger.logDebug("KeySessionManager::releaseKeys::stop session")
            listeners.forEach(KeySessionListener::stopSession)
        }
    }

    interface KeySessionListener {
        fun stopSession()
    }
}
