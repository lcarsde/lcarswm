package de.atennert.lcarswm

import kotlinx.cinterop.convert
import xlib.Window

/**
 * POJO for registered windows.
 */
data class FramedWindow(val id: Window, val borderWidth: Int) {
    var frame: Window = 0.convert()
    var name: String = ""
}
