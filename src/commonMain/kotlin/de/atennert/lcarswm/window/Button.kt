package de.atennert.lcarswm.window

abstract class Button (
    val onClick: () -> Unit
) {
    abstract fun changePosition(x: Int, y: Int)
}
