package de.atennert.lcarswm.window

import de.atennert.lcarswm.BAR_END_WIDTH
import de.atennert.lcarswm.BAR_GAP_SIZE
import de.atennert.lcarswm.BLACK
import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.drawing.Color
import de.atennert.lcarswm.drawing.ColorFactory
import de.atennert.lcarswm.drawing.FontProvider
import de.atennert.lcarswm.keys.KeyManager
import de.atennert.lcarswm.lifecycle.closeWith
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.*
import de.atennert.rx.NextObserver
import de.atennert.rx.Subscription
import kotlinx.cinterop.*
import xlib.*

class PosixButton(
    private val display: CPointer<Display>?,
    private val screen: Screen,
    private val colorFactory: ColorFactory,
    private val fontProvider: FontProvider,
    monitorManager: MonitorManager,
    keyManager: KeyManager,
    private val text: String,
    backgroundColor: Color,
    private var x: Int,
    private var y: Int,
    private val width: Int,
    private val height: Int,
    private val onClick: () -> Unit
) : Button<Window> {
    private val backgroundColorXft = colorFactory.createXftColor(backgroundColor)
    private val textColorXft = colorFactory.createXftColor(BLACK)

    override val id: Window = wrapXCreateSimpleWindow(
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

    private val borderSpace = 4

    private var xOffset = 0

    private val subscription = Subscription()

    // TODO combine screen mode and primary monitor handling
    private val screenModeHandler = NextObserver.NextHandler<ScreenMode> {
        when (it) {
            ScreenMode.NORMAL -> {
                xOffset = 0
                changePosition(x, y)
                wrapXMapWindow(display, id)
            }

            ScreenMode.MAXIMIZED -> {
                xOffset = BAR_END_WIDTH + BAR_GAP_SIZE
                changePosition(x + xOffset, y)
                wrapXMapWindow(display, id)
            }

            ScreenMode.FULLSCREEN -> wrapXUnmapWindow(display, id)
        }
    }

    private val primaryMonitorHandler = NextObserver.NextHandler<Monitor?> { monitor ->
        monitor?.let {
            x = it.x
            y = it.y
            this.changePosition(it.x + xOffset, it.y)
        }
    }

    init {
        closeWith(PosixButton::cleanup)

        keyManager.grabButton(
            Button1.convert(),
            AnyModifier,
            id,
            (ButtonPressMask or ButtonReleaseMask or ButtonMotionMask).convert(),
            GrabModeAsync,
            None.convert()
        )

        wrapXMapWindow(display, id)

        draw()

        subscription.add(
            monitorManager.screenModeObs
                .subscribe(NextObserver(screenModeHandler))
        )

        subscription.add(
            monitorManager.primaryMonitorObs
                .subscribe(NextObserver(primaryMonitorHandler))
        )
    }

    private fun draw() {
        val pixmap =
            wrapXCreatePixmap(display, screen.root, width.convert(), height.convert(), screen.root_depth.convert())
        val xftDraw = wrapXftDrawCreate(display, pixmap, screen.root_visual!!, colorFactory.colorMapId)

        wrapXftDrawRect(xftDraw, backgroundColorXft.ptr, 0, 0, width.convert(), height.convert())

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

        wrapXSetWindowBackgroundPixmap(display, id, pixmap)
        wrapXClearWindow(display, id)
        wrapXftDrawDestroy(xftDraw)
        wrapXFreePixmap(display, pixmap)

        nativeHeap.free(rect.rawPtr)
    }

    override fun changePosition(x: Int, y: Int) {
        wrapXMoveWindow(display, id, x, y)
    }

    override fun press() {
        // TODO press coloring
    }

    override fun release() {
        // TODO normal coloring
        onClick()
    }

    private fun cleanup() {
        subscription.unsubscribe()
        wrapXUnmapWindow(display, id)
        wrapXDestroyWindow(display, id)
    }
}
