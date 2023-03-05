package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.*
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.system.api.DrawApi
import de.atennert.lcarswm.system.api.FontApi
import de.atennert.lcarswm.window.FramedWindow
import de.atennert.lcarswm.window.WindowFocusHandler
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.*

class FrameDrawer(
    private val fontApi: FontApi,
    private val drawApi: DrawApi,
    private val focusHandler: WindowFocusHandler,
    private val fontProvider: FontProvider,
    private val colorFactory: ColorFactory,
    private val screen: Screen
) : IFrameDrawer {

    private val activeTextColor = colorFactory.createXftColor(COLOR_ACTIVE_TITLE)
    private val inactiveTextColor = colorFactory.createXftColor(COLOR_INACTIVE_TITLE)
    private val maxBarColor = colorFactory.createXftColor(COLOR_MAX_BAR_DOWN)
    private val normalBarColor = colorFactory.createXftColor(COLOR_NORMAL_BAR_DOWN)
    private val normalCornerDownColor = colorFactory.createXftColor(COLOR_NORMAL_CORNER_3)
    private val backgroundColor = colorFactory.createXftColor(COLOR_BACKGROUND)
    override val colorMap: Colormap
            get() = colorFactory.colorMapId

    override fun drawFrame(window: FramedWindow, monitor: Monitor) {
        val windowMeasurements = monitor.getWindowMeasurements()
        val textW = if (monitor.getScreenMode() == ScreenMode.NORMAL) {
            monitor.width - LOWER_CORNER_WIDTH - BAR_GAP_SIZE - WINDOW_TITLE_OFFSET - BAR_GAP_SIZE /* <text> */ - BAR_GAP_SIZE - BAR_END_WIDTH
        } else {
            windowMeasurements.width - WINDOW_TITLE_OFFSET - BAR_GAP_SIZE
        }
        val textH = BAR_HEIGHT_WITH_OFFSET
        val rect = nativeHeap.alloc<PangoRectangle>()
        val (ascent, descent) = fontProvider.getAscDsc(WINDOW_TITLE_FONT_SIZE, PANGO_WEIGHT_BOLD)

        val textY = (((textH * PANGO_SCALE)
                - (ascent + descent))
                / 2 + ascent) / PANGO_SCALE

        val pixmap = drawApi.createPixmap(screen.root, windowMeasurements.width.convert(), textH.convert(), screen.root_depth.convert())
        val xftDraw = drawApi.xftDrawCreate(pixmap, screen.root_visual!!, colorMap)

        val textColor = if (focusHandler.getFocusedWindow() == window.id) {
            activeTextColor
        } else {
            inactiveTextColor
        }

        fontApi.setLayoutText(fontProvider.layout, window.title)
        fontApi.setLayoutWidth(fontProvider.layout, textW * PANGO_SCALE)

        fontApi.getLayoutPixelExtents(fontProvider.layout, rect.ptr)
        val textX = windowMeasurements.width - rect.width

        drawApi.xftDrawRect(xftDraw, backgroundColor.ptr, 0, 0,  windowMeasurements.width.convert(), textH.convert())

        val line = fontApi.getLayoutLineReadonly(fontProvider.layout, 0)
        fontApi.xftRenderLayoutLine(xftDraw, textColor.ptr, line, textX * PANGO_SCALE, textY * PANGO_SCALE)

        if (monitor.getScreenMode() == ScreenMode.NORMAL) {
            val primBarWidth = LOWER_CORNER_WIDTH - NORMAL_WINDOW_LEFT_OFFSET
            val secBarWidth = textX - primBarWidth - 2 * BAR_GAP_SIZE + FIRST_LETTER_OFFSET
            drawApi.xftDrawRect(xftDraw, normalCornerDownColor.ptr, 0, TITLE_BAR_OFFSET,  primBarWidth.convert(), BAR_HEIGHT.convert())
            drawApi.xftDrawRect(xftDraw, normalBarColor.ptr, primBarWidth + BAR_GAP_SIZE, TITLE_BAR_OFFSET,  secBarWidth.convert(), BAR_HEIGHT.convert())
        } else {
            val barWidth = textX - BAR_GAP_SIZE + FIRST_LETTER_OFFSET
            drawApi.xftDrawRect(xftDraw, maxBarColor.ptr, 0, TITLE_BAR_OFFSET,  barWidth.convert(), BAR_HEIGHT.convert())
        }

        drawApi.setWindowBackgroundPixmap(window.titleBar, pixmap)
        drawApi.clearWindow(window.titleBar)
        drawApi.freePixmap(pixmap)
        nativeHeap.free(rect.rawPtr)
    }

    companion object {
        private const val FIRST_LETTER_OFFSET = 2
    }
}