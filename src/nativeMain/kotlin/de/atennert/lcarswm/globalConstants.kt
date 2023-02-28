package de.atennert.lcarswm

import de.atennert.lcarswm.drawing.Color
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

// layout values

const val BAR_HEIGHT = 40
const val BAR_GAP_SIZE = 8
const val BAR_HEIGHT_SMALL = (BAR_HEIGHT - BAR_GAP_SIZE) / 2
const val BAR_END_WIDTH = BAR_HEIGHT - BAR_GAP_SIZE
const val OUTER_CORNER_RADIUS_BIG = BAR_HEIGHT
const val OUTER_CORNER_RADIUS_SMALL = BAR_HEIGHT_SMALL
const val INNER_CORNER_RADIUS = BAR_HEIGHT_SMALL
const val DATA_AREA_HEIGHT = 3 * 40 + 2 * 8
const val DATA_BAR_HEIGHT = DATA_AREA_HEIGHT - INNER_CORNER_RADIUS
const val SIDE_BAR_WIDTH = 184
const val TITLE_BAR_OFFSET = 1 // extra pixel for overflowing fonts
const val BAR_HEIGHT_WITH_OFFSET = BAR_HEIGHT + TITLE_BAR_OFFSET // accommodate the font layout with a required extra pixel

const val NORMAL_WINDOW_LEFT_OFFSET = SIDE_BAR_WIDTH + BAR_GAP_SIZE + BAR_END_WIDTH + BAR_GAP_SIZE
const val NORMAL_WINDOW_UPPER_OFFSET = BAR_HEIGHT * 2 + INNER_CORNER_RADIUS + BAR_GAP_SIZE * 3 + DATA_BAR_HEIGHT
const val NORMAL_WINDOW_NON_APP_HEIGHT = NORMAL_WINDOW_UPPER_OFFSET + BAR_GAP_SIZE + INNER_CORNER_RADIUS * 2 + BAR_HEIGHT

const val LOWER_CORNER_WIDTH = OUTER_CORNER_RADIUS_BIG + 272

const val WINDOW_TITLE_FONT_SIZE = BAR_HEIGHT
const val WINDOW_TITLE_OFFSET = 30

// Base colors
val BLACK = Color(0, 0, 0)
val YELLOW = Color(0xFFFF, 0x9999, 0)
val ORCHID = Color(0xCCCC, 0x9999, 0xCCCC)
val DAMPENED_PURPLE = Color(0x9999, 0x9999, 0xCCCC)
val DARK_RED = Color(0xCCCC, 0x6666, 0x6666)
val SAND = Color(0xFFFF, 0xCCCC, 0x9999)
val BRIGHT_PURPLE = Color(0x9999, 0x9999, 0xFFFF)
val ORANGE = Color(0xFFFF, 0x9999, 0x6666)
val DARK_PINK = Color(0xCCCC, 0x6666, 0x9999)

val COLOR_LOGO = YELLOW
val COLOR_ACTIVE_TITLE = YELLOW
val COLOR_INACTIVE_TITLE = DARK_RED
val COLOR_BACKGROUND = BLACK

val COLOR_NORMAL_BAR_DOWN = BRIGHT_PURPLE
val COLOR_NORMAL_SIDEBAR_UP = DAMPENED_PURPLE
val COLOR_NORMAL_SIDEBAR_DOWN = DAMPENED_PURPLE
val COLOR_NORMAL_BAR_MIDDLE_1 = DAMPENED_PURPLE
val COLOR_NORMAL_BAR_MIDDLE_2 = DARK_RED
val COLOR_NORMAL_BAR_MIDDLE_3 = BRIGHT_PURPLE
val COLOR_NORMAL_BAR_MIDDLE_4 = ORCHID
val COLOR_NORMAL_CORNER_1 = ORCHID
val COLOR_NORMAL_CORNER_2 = ORCHID
val COLOR_NORMAL_CORNER_3 = ORCHID

val COLOR_MAX_BAR_DOWN = ORCHID
