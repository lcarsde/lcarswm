package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.*
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.system.api.DrawApi
import de.atennert.lcarswm.system.api.FontApi
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
    private val colors: Colors,
    screenId: Int,
    private val screen: Screen
) : IFrameDrawer {
    private val pango = fontApi.xftGetContext(screenId)
    private val layout = fontApi.newLayout(pango)
    private val font = fontApi.getFontDescription()

    private var ascent: Int
    private var descent: Int

    private val activeTextColor = colors.getXftColor(1)
    private val inactiveTextColor = colors.getXftColor(4)
    private val primaryBarColor = colors.getXftColor(2)
    private val secondaryBarColor = colors.getXftColor(6)
    private val backgroundColor = colors.getXftColor(0)
    override val colorMap: Colormap
            get() = colors.colorMap.first

    init {
        val ascDesc = initializeFontObjects()
        ascent = ascDesc.first
        descent = ascDesc.second
    }

    private fun initializeFontObjects(): Pair<Int, Int> {
        val lang = fontApi.getDefaultLanguage()
        fontApi.setFontDescriptionFamily(font, WM_FONT)
        fontApi.setFontDescriptionWeight(font, PANGO_WEIGHT_BOLD)
        fontApi.setFontDescriptionStyle(font, PangoStyle.PANGO_STYLE_NORMAL)
        fontApi.setFontDescriptionSize(font, WINDOW_TITLE_FONT_SIZE * PANGO_SCALE)

        fontApi.setLayoutFontDescription(layout, font)
        fontApi.setLayoutWrapMode(layout, PangoWrapMode.PANGO_WRAP_WORD_CHAR)
        fontApi.setLayoutEllipsizeMode(layout, PangoEllipsizeMode.PANGO_ELLIPSIZE_END)
        fontApi.setLayoutSingleParagraphMode(layout, true)

        val metrics = fontApi.getFontMetrics(pango, font, lang)
        val ascDesc = fontApi.getFontAscentDescent(metrics)
        fontApi.freeFontMetrics(metrics)

        return ascDesc
    }

    override fun drawFrame(window: FramedWindow, monitor: Monitor) {
        val windowMeasurements = monitor.getWindowMeasurements()
        val textW = if (monitor.getScreenMode() == ScreenMode.NORMAL) {
            monitor.width - LOWER_CORNER_WIDTH - BAR_GAP_SIZE - WINDOW_TITLE_OFFSET - BAR_GAP_SIZE /* <text> */ - BAR_GAP_SIZE - BAR_END_WIDTH
        } else {
            windowMeasurements.width - WINDOW_TITLE_OFFSET - BAR_GAP_SIZE
        }
        val textH = BAR_HEIGHT_WITH_OFFSET
        val rect = nativeHeap.alloc<PangoRectangle>()

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

        fontApi.setLayoutText(layout, window.title)
        fontApi.setLayoutWidth(layout, textW * PANGO_SCALE)

        fontApi.getLayoutPixelExtents(layout, rect.ptr)
        val textX = windowMeasurements.width - rect.width

        drawApi.xftDrawRect(xftDraw, backgroundColor.ptr, 0, 0,  windowMeasurements.width.convert(), textH.convert())

        val line = fontApi.getLayoutLineReadonly(layout, 0)
        fontApi.xftRenderLayoutLine(xftDraw, textColor.ptr, line, textX * PANGO_SCALE, textY * PANGO_SCALE)

        if (monitor.getScreenMode() == ScreenMode.NORMAL) {
            val primBarWidth = LOWER_CORNER_WIDTH - NORMAL_WINDOW_LEFT_OFFSET
            val secBarWidth = textX - primBarWidth - 2 * BAR_GAP_SIZE + FIRST_LETTER_OFFSET
            drawApi.xftDrawRect(xftDraw, primaryBarColor.ptr, 0, TITLE_BAR_OFFSET,  primBarWidth.convert(), BAR_HEIGHT.convert())
            drawApi.xftDrawRect(xftDraw, secondaryBarColor.ptr, primBarWidth + BAR_GAP_SIZE, TITLE_BAR_OFFSET,  secBarWidth.convert(), BAR_HEIGHT.convert())
        } else {
            val barWidth = textX - BAR_GAP_SIZE + FIRST_LETTER_OFFSET
            drawApi.xftDrawRect(xftDraw, primaryBarColor.ptr, 0, TITLE_BAR_OFFSET,  barWidth.convert(), BAR_HEIGHT.convert())
        }

        drawApi.setWindowBackgroundPixmap(window.titleBar, pixmap)
        drawApi.clearWindow(window.titleBar)
        drawApi.freePixmap(pixmap)
        nativeHeap.free(rect.rawPtr)
    }

    fun close() {
        layout?.let { nativeHeap.free(it.rawValue) }
        fontApi.freeFontDescription(font)
        pango?.let { nativeHeap.free(it.rawValue) }

        nativeHeap.free(activeTextColor.rawPtr)
        nativeHeap.free(inactiveTextColor.rawPtr)
        nativeHeap.free(primaryBarColor.rawPtr)
        nativeHeap.free(secondaryBarColor.rawPtr)
        nativeHeap.free(backgroundColor.rawPtr)
    }

    companion object {
        private const val FIRST_LETTER_OFFSET = 2
    }
}