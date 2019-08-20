package de.atennert.lcarswm

/**
 * POJO for registered windows.
 */
data class Window(val id: ULong) {
    var frame: ULong = 0.toULong()
}
