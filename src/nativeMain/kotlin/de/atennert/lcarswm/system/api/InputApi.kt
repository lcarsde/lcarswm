package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import xlib.*

/**
 *
 */
interface InputApi {
    fun selectInput(window: Window, mask: Long): Int

    fun setInputFocus(window: Window, revertTo: Int, time: Time): Int

    fun grabKey(keyCode: Int, modifiers: UInt, window: Window, ownerEvents: Boolean,
                pointerMode: Int, keyboardMode: Int): Int

    fun grabButton(button: UInt, modifiers: UInt, window: Window, ownerEvents: Boolean,
                   mask: UInt, pointerMode: Int, keyboardMode: Int, windowToConfineTo: Window, cursor: Cursor): Int

    fun getModifierMapping(): CPointer<XModifierKeymap>?

    fun keysymToKeycode(keySym: KeySym): KeyCode
}