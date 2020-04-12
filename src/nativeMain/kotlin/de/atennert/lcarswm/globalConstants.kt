package de.atennert.lcarswm

import xlib.*

const val LOG_FILE_PATH = "/var/log/lcarswm.log"

const val HOME_CONFIG_DIR_PROPERTY = "XDG_CONFIG_HOME"

const val LCARS_WM_DIR = "lcarswm"

const val KEY_CONFIG_FILE = "/$LCARS_WM_DIR/key-config.properties"

const val X_FALSE = 0
const val X_TRUE = 1

val LCARS_WM_KEY_SYMS = listOf(
    XK_Tab, // toggle through windows
    XK_Up, // move windows up the monitor list
    XK_Down, // move windows down the monitor list
    XK_M, // toggle screen mode
    XK_Q, // quit
    XK_F4 // close window
)

// layout values

const val BAR_HEIGHT = 40
const val TITLE_BAR_OFFSET = 1
const val BAR_HEIGHT_WITH_OFFSET = BAR_HEIGHT + TITLE_BAR_OFFSET // accommodate the font layout with a required extra pixel

const val WINDOW_TITLE_FONT_SIZE = BAR_HEIGHT
