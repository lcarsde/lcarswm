package de.atennert.lcarswm

import kotlinx.cinterop.convert
import xlib.Window

/**
 * POJO for registered windows.
 */
data class FramedWindow(val id: Window, var name: String, val borderWidth: Int) {
    var frame: Window = 0.convert()
    var titleBar: Window = 0.convert()
}
