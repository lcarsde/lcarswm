package de.atennert.lcarswm

import kotlinx.cinterop.convert
import xlib.Window

/**
 * POJO for registered windows.
 */
data class WindowContainer(val id: Window) {
    var frame: Window = 0.convert()
}
