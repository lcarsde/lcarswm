package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import xlib.*

/**
 *
 */
interface InputApi {
    fun selectInput(window: Window, mask: Long): Int

    fun setInputFocus(window: Window, revertTo: Int, time: Time): Int

    fun grabKey(keyCode: Int, modifiers: UInt, window: Window, keyboardMode: Int): Int

    fun grabButton(button: UInt, modifiers: UInt, window: Window, ownerEvents: Boolean,
                   mask: UInt, pointerMode: Int, keyboardMode: Int, windowToConfineTo: Window, cursor: Cursor): Int

    fun getModifierMapping(): CPointer<XModifierKeymap>?

    fun getDisplayKeyCodeMinMaxCounts(): Pair<Int, Int>

    fun getKeyboardMapping(firstKeyCode: KeyCode, keyCodeCount: Int, keySymsPerKeyCode: CPointer<IntVar>): CPointer<KeySymVar>?

    fun keysymToKeycode(keySym: KeySym): KeyCode

    fun stringToKeysym(s: String): KeySym
}