package de.atennert.lcarswm.window

import de.atennert.lcarswm.BLACK
import de.atennert.lcarswm.drawing.Color
import de.atennert.lcarswm.drawing.ColorFactory
import de.atennert.lcarswm.drawing.FontProvider
import de.atennert.lcarswm.keys.KeyManager
import de.atennert.lcarswm.lifecycle.closeWith
import de.atennert.lcarswm.system.*
import kotlinx.cinterop.*
import xlib.*

class PosixButton(
    private val display: CPointer<Display>?,
    val screen: Screen,
    colorFactory: ColorFactory,
    fontProvider: FontProvider,
    val keyManager: KeyManager,
    text: String,
    backgroundColor: Color,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    onClick: () -> Unit
) : Button(onClick) {
    private val backgroundColorXft = colorFactory.createXftColor(backgroundColor)
    private val textColorXft = colorFactory.createXftColor(BLACK)

    private val windowId: Window

    private val borderSpace = 4

    init {
        closeWith(PosixButton::cleanup)

        windowId = wrapXCreateSimpleWindow(
            display,
            screen.root,
            x,
            y,
            width.convert(),
            height.convert(),
            0.convert(),
            0.convert(),
            0.convert(),
        )

        keyManager.grabButton(
            Button1.convert(),
            AnyModifier,
            windowId,
            (ButtonPressMask or ButtonReleaseMask or ButtonMotionMask).convert(),
            GrabModeAsync,
            None.convert()
        )

        wrapXMapWindow(display, windowId)

        val pixmap = wrapXCreatePixmap(display, screen.root, width.convert(), height.convert(), screen.root_depth.convert())
        val xftDraw = wrapXftDrawCreate(display, pixmap, screen.root_visual!!, colorFactory.colorMapId)

        wrapXftDrawRect(xftDraw, backgroundColorXft.ptr, 0, 0,  width.convert(), height.convert())

        val textW = width - 2 * borderSpace
        val textH = 11
        val (ascent, descent) = fontProvider.getAscDsc(textH, PANGO_WEIGHT_NORMAL)
        val textY = 25 + (((textH * PANGO_SCALE)
                - (ascent + descent))
                / 2 + ascent) / PANGO_SCALE
        wrapPangoLayoutSetText(fontProvider.layout, text, text.length)
        wrapPangoLayoutSetWidth(fontProvider.layout, textW * PANGO_SCALE)
        val rect = nativeHeap.alloc<PangoRectangle>()
        wrapPangoLayoutGetPixelExtents(fontProvider.layout, null, rect.ptr)
        val textX = textW + borderSpace - rect.width
        val line = wrapPangoLayoutGetLineReadonly(fontProvider.layout, 0)
        wrapPangoXftRenderLayoutLine(xftDraw, textColorXft.ptr, line, textX * PANGO_SCALE, textY * PANGO_SCALE)

        wrapXSetWindowBackgroundPixmap(display, windowId, pixmap)
        wrapXClearWindow(display, windowId)
        wrapXftDrawDestroy(xftDraw)
        wrapXFreePixmap(display, pixmap)

        nativeHeap.free(rect.rawPtr)
    }

    override fun changePosition(x: Int, y: Int) {
        wrapXMoveWindow(display, windowId, x, y)
    }

    private fun cleanup() {
        wrapXUnmapWindow(display, windowId)
        wrapXDestroyWindow(display, windowId)
    }
}