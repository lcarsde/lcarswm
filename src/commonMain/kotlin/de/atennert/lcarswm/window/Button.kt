package de.atennert.lcarswm.window

interface Button<ID> {
    val id: ID

    fun changePosition(x: Int, y: Int)

    fun press()

    fun release()
}
