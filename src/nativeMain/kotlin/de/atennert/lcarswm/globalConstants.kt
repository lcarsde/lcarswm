package de.atennert.lcarswm

import de.atennert.lcarswm.keys.Modifiers
import xlib.*

const val X_FALSE = 0
const val X_TRUE = 1

val LCARS_WM_KEY_SYMS = mapOf(
    XK_Tab to Modifiers.ALT, // toggle through windows
    XK_Up to Modifiers.ALT, // move windows up the monitor list
    XK_Down to Modifiers.ALT, // move windows down the monitor list
    XK_F4 to Modifiers.ALT, // close window
    XK_M to Modifiers.SUPER, // toggle screen mode
    XK_Q to Modifiers.SUPER, // quit
)

