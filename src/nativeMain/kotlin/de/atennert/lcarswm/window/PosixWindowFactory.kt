package de.atennert.lcarswm.window

import de.atennert.lcarswm.drawing.Color
import de.atennert.lcarswm.drawing.ColorFactory
import de.atennert.lcarswm.drawing.FontProvider
import de.atennert.lcarswm.keys.KeyManager
import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.Screen
import xlib.Window

class PosixWindowFactory(
    private val display: CPointer<Display>?,
    private val screen: Screen,
    private val colorFactory: ColorFactory,
    private val fontProvider: FontProvider,
    private val keyManager: KeyManager
) : WindowFactory<Window> {
    override fun createButton(
        text: String,
        backgroundColor: Color,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        onClick: () -> Unit
    ): PosixButton {
        return PosixButton(
            display, screen, colorFactory, fontProvider, keyManager,
            text, backgroundColor, x, y, width, height, onClick
        )
    }
}