package de.atennert.lcarswm.window

import de.atennert.lcarswm.window.TextAtomReader
import kotlinx.cinterop.convert
import xlib.Window

/**
 * POJO for registered windows.
 */
data class FramedWindow(val id: Window, val borderWidth: Int) {
    var frame: Window = 0.convert()
    var titleBar: Window = 0.convert()
    var title: String = TextAtomReader.NO_NAME
    var wmClass: String = TextAtomReader.NO_NAME
}
