package de.atennert.lcarswm.window

import de.atennert.lcarswm.ScreenMode

interface WmWindow<ID> {
    val id: ID

    fun open(measurements: WindowMeasurements, screenMode: ScreenMode)

    fun show()

    fun moveResize(measurements: WindowMeasurements, screenMode: ScreenMode)

    fun updateTitle()

    fun focus()

    fun unfocus()

    fun hide()

    fun close()
}