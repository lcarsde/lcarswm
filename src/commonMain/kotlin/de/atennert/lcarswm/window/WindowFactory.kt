package de.atennert.lcarswm.window

import de.atennert.lcarswm.ColorSet

interface WindowFactory<ID> {
    fun createButton(
        text: String,
        colorSet: ColorSet,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        onClick: () -> Unit
    ): Button<ID>

    fun createWindow(
        id: ID,
        isSetup: Boolean = false
    ): WmWindow<ID>?
}