package de.atennert.lcarswm

import xlib.*

const val LOG_FILE_PATH = "/var/log/lcarswm.log"

const val X_FALSE = 0
const val X_TRUE = 1

const val KEY_CONFIG_FILE = "key-config.properties"

val LCARS_WM_KEY_SYMS = listOf(
    XK_Tab, // toggle through windows
    XK_Up, // move windows up the monitor list
    XK_Down, // move windows down the monitor list
    XK_M, // toggle screen mode
    XK_Q, // quit
    XK_F4 // close window
)
