package de.atennert.lcarswm

import xlib.*

const val LOG_FILE_PATH = "/var/log/lcarswm.log"

const val HOME_CONFIG_DIR_PROPERTY = "XDG_CONFIG_HOME"

const val LCARS_WM_DIR = "lcarswm"

const val KEY_CONFIG_FILE = "/$LCARS_WM_DIR/key-config.properties"

const val WM_FONT = "Ubuntu Condensed"

const val WM_LOGO = "/usr/share/pixmaps/lcarswm.xpm"

const val X_FALSE = 0
const val X_TRUE = 1

val LCARS_WM_KEY_SYMS = mapOf(
    Pair(XK_Tab, Modifiers.ALT), // toggle through windows
    Pair(XK_Up, Modifiers.ALT), // move windows up the monitor list
    Pair(XK_Down, Modifiers.ALT), // move windows down the monitor list
    Pair(XK_F4, Modifiers.ALT), // close window
    Pair(XK_M, Modifiers.SUPER), // toggle screen mode
    Pair(XK_Q, Modifiers.SUPER) // quit
)

// layout values

const val BAR_HEIGHT = 40
const val BAR_GAP_SIZE = 8
const val BAR_HEIGHT_SMALL = (BAR_HEIGHT - BAR_GAP_SIZE) / 2
const val BAR_END_WIDTH = BAR_HEIGHT - BAR_GAP_SIZE
const val OUTER_CORNER_RADIUS_BIG = BAR_HEIGHT
const val OUTER_CORNER_RADIUS_SMALL = BAR_HEIGHT_SMALL
const val INNER_CORNER_RADIUS = BAR_HEIGHT_SMALL
const val DATA_AREA_HEIGHT = 88
const val SIDE_BAR_WIDTH = 184
const val TITLE_BAR_OFFSET = 1 // extra pixel for overflowing fonts
const val BAR_HEIGHT_WITH_OFFSET = BAR_HEIGHT + TITLE_BAR_OFFSET // accommodate the font layout with a required extra pixel

const val NORMAL_WINDOW_LEFT_OFFSET = SIDE_BAR_WIDTH + BAR_GAP_SIZE + BAR_END_WIDTH + BAR_GAP_SIZE
const val NORMAL_WINDOW_UPPER_OFFSET = 2 * BAR_HEIGHT + 3 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_AREA_HEIGHT
const val NORMAL_WINDOW_NON_APP_HEIGHT = NORMAL_WINDOW_UPPER_OFFSET + BAR_GAP_SIZE + INNER_CORNER_RADIUS + BAR_HEIGHT

const val LOWER_CORNER_WIDTH = OUTER_CORNER_RADIUS_BIG + 272

const val WINDOW_TITLE_FONT_SIZE = BAR_HEIGHT
const val WINDOW_TITLE_OFFSET = 30
