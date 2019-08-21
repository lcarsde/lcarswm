package de.atennert.lcarswm

import xlib.*

const val X_FALSE = 0
const val X_TRUE = 1

const val NO_RANDR_BASE = -1

const val KEY_CONFIG_FILE = "key-config.properties"

const val WM_MODIFIER_KEY = Mod4Mask // should be windows key

const val ALT_MODIFIER_KEY = Mod1Mask

val LCARS_WM_KEY_SYMS = listOf(
    XK_Tab, // toggle through windows
    XK_Up, // move windows up the monitor list
    XK_Down, // move windows down the monitor list
    XK_M, // toggle screen mode
    XK_Q, // quit
    XK_F4, // close window
    XK_T, // terminal
    XK_B, // browser
    XK_I // IntelliJ
)

val LCARS_NO_MASK_KEY_SYMS = listOf(
    XF86XK_AudioMute,
    XF86XK_AudioLowerVolume,
    XF86XK_AudioRaiseVolume
)
