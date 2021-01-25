package de.atennert.lcarswm.keys

import kotlinx.cinterop.convert

class KeySessionManager {
    private val listeners = mutableListOf<KeySessionListener>()

    private var lastMask: Int = 0
    private var lastKeyCode: UInt = 0.convert()

    fun pressKeys(keyCode: UInt, keyMask: Int) {
        val isSameSession = (keyMask == lastMask) and (keyCode == lastKeyCode)
        if (!isSameSession) {
            listeners.forEach { it.stopSession() }
        }
        lastMask = keyMask
        lastKeyCode = keyCode
    }

    fun addListener(listener: KeySessionListener) {
        listeners.add(listener)
    }

    interface KeySessionListener {
        fun stopSession()
    }
}
