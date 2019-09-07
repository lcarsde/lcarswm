package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import xlib.*

/**
 *
 */
interface InputApi {
    fun selectInput(display: CValuesRef<Display>, window: Window, mask: Long): Int

    fun setInputFocus(display: CValuesRef<Display>, window: Window, revertTo: Int, time: Time): Int

    fun grabKey(display: CValuesRef<Display>, keyCode: Int, modifiers: UInt, window: Window, ownerEvents: Boolean,
                pointerMode: Int, keyboardMode: Int): Int

    fun grabButton(display: CValuesRef<Display>, button: UInt, modifiers: UInt, window: Window, ownerEvents: Boolean,
                   mask: UInt, pointerMode: Int, keyboardMode: Int, windowToConfineTo: Window, cursor: Cursor): Int

    fun getModifierMapping(display: CValuesRef<Display>): CPointer<XModifierKeymap>?

    fun keysymToKeycode(display: CValuesRef<Display>, keySym: KeySym): KeyCode
}