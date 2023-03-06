package de.atennert.lcarswm.window

import de.atennert.lcarswm.drawing.Color

interface WindowFactory<ID> {
    fun createButton(
        text: String,
        backgroundColor: Color,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        onClick: () -> Unit
    ): Button<ID>
}