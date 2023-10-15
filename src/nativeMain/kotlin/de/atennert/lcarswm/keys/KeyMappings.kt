package de.atennert.lcarswm.keys

import de.atennert.lcarswm.Environment
import de.atennert.lcarswm.system.wrapXKeysymToKeycode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import xlib.*

actual const val keyShiftL = XK_Shift_L
actual const val keyShiftR = XK_Shift_R
actual const val keyControlL = XK_Control_L
actual const val keyControlR = XK_Control_R
actual const val keySuperL = XK_Super_L
actual const val keySuperR = XK_Super_R
actual const val keyHyperL = XK_Hyper_L
actual const val keyHyperR = XK_Hyper_R
actual const val keyMetaL = XK_Meta_L
actual const val keyMetaR = XK_Meta_R
actual const val keyAltL = XK_Alt_L
actual const val keyAltR = XK_Alt_R

@OptIn(ExperimentalForeignApi::class)
actual fun keysymToKeycode(env: Environment, keySym: Int): UInt {
    return wrapXKeysymToKeycode(env.display, keySym.convert()).convert()
}
