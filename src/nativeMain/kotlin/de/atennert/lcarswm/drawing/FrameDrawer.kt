package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.*
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.system.api.DrawApi
import de.atennert.lcarswm.system.api.FontApi
import de.atennert.lcarswm.windowactions.WindowFocusHandler
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.*

class FrameDrawer(
    private val fontApi: FontApi,
    private val drawApi: DrawApi,
    private val focusHandler: WindowFocusHandler,
    colors: Colors,
    screenId: Int,
    private val screen: Screen
) : IFrameDrawer {
    private val pango = fontApi.xftGetContext(screenId)
    private val layout = fontApi.newLayout(pango)
    private val font = fontApi.getFontDescription()

    private var ascent: Int
    private var descent: Int

    private val activeTextColor = nativeHeap.alloc<XftColor>()
    private val inactiveTextColor = nativeHeap.alloc<XftColor>()
    private val primaryBarColor = nativeHeap.alloc<XftColor>()
    private val secondaryBarColor = nativeHeap.alloc<XftColor>()
    private val backgroundColor = nativeHeap.alloc<XftColor>()
    override val colorMap: Colormap

    init {
        val ascDesc = loadFontGC()
        ascent = ascDesc.first
        descent = ascDesc.second

        colorMap = colors.colorMap.first

        activeTextColor.color.red = 0xffff.convert()
        activeTextColor.color.green = 0x9999.convert()
        activeTextColor.color.blue = 0x0000.convert()
        activeTextColor.color.alpha = 0xffff.convert()
        activeTextColor.pixel = colors.colorMap.second[1]

        inactiveTextColor.color.red = 0xcccc.convert()
        inactiveTextColor.color.green = 0x6666.convert()
        inactiveTextColor.color.blue = 0x6666.convert()
        inactiveTextColor.color.alpha = 0xffff.convert()
        inactiveTextColor.pixel = colors.colorMap.second[4]

        primaryBarColor.color.red = 0xcccc.convert()
        primaryBarColor.color.green = 0x9999.convert()
        primaryBarColor.color.blue = 0xcccc.convert()
        primaryBarColor.color.alpha = 0xffff.convert()
        primaryBarColor.pixel = colors.colorMap.second[6]

        secondaryBarColor.color.red = 0x9999.convert()
        secondaryBarColor.color.green = 0x9999.convert()
        secondaryBarColor.color.blue = 0xffff.convert()
        secondaryBarColor.color.alpha = 0xffff.convert()
        secondaryBarColor.pixel = colors.colorMap.second[2]

        backgroundColor.color.red = 0x0000.convert()
        backgroundColor.color.green = 0x0000.convert()
        backgroundColor.color.blue = 0x0000.convert()
        backgroundColor.color.alpha = 0xffff.convert()
        backgroundColor.pixel = colors.colorMap.second[0]
    }

    private fun loadFontGC(): Pair<Int, Int> {
        val lang = fontApi.getDefaultLanguage()
        fontApi.setFontDescriptionFamily(font, "Ubuntu Condensed")
        fontApi.setFontDescriptionWeight(font, PANGO_WEIGHT_BOLD)
        fontApi.setFontDescriptionStyle(font, PangoStyle.PANGO_STYLE_NORMAL)
        fontApi.setFontDescriptionSize(font, WINDOW_TITLE_FONT_SIZE * PANGO_SCALE)

        pango_layout_set_font_description(layout, font)
        pango_layout_set_wrap(layout, PangoWrapMode.PANGO_WRAP_WORD_CHAR)

        val metrics = pango_context_get_metrics(pango, font, lang)
        ascent = pango_font_metrics_get_ascent(metrics)
        descent = pango_font_metrics_get_descent(metrics)
        pango_font_metrics_unref(metrics)

        return Pair(ascent, descent)
    }

    override fun drawFrame(window: FramedWindow, monitor: Monitor) {
        val screenMeasurements = monitor.getWindowMeasurements()
        val textW = monitor.width - 390 // a little wider the normal layout lower corner
        val textH = BAR_HEIGHT_WITH_OFFSET
        val rect = nativeHeap.alloc<PangoRectangle>()

        val textY = (((textH * PANGO_SCALE)
                - (ascent + descent))
                / 2 + ascent) / PANGO_SCALE

        val pixmap = drawApi.createPixmap(screen.root, screenMeasurements[2].convert(), textH.convert(), screen.root_depth.convert())
        val xftDraw = drawApi.xftDrawCreate(pixmap, screen.root_visual!!, colorMap)

        val textColor = if (focusHandler.getFocusedWindow() == window.id) {
            activeTextColor
        } else {
            inactiveTextColor
        }

        pango_layout_set_text(layout, window.name, window.name.length)
        pango_layout_set_width(layout, textW * PANGO_SCALE)
        pango_layout_set_ellipsize(layout, PangoEllipsizeMode.PANGO_ELLIPSIZE_END)
        pango_layout_set_single_paragraph_mode(layout, X_TRUE)

        pango_layout_get_pixel_extents(layout, null, rect.ptr)
        val textX = screenMeasurements[2] - rect.width

        XftDrawRect(xftDraw, backgroundColor.ptr, 0, 0,  screenMeasurements[2].convert(), textH.convert())

        val line = pango_layout_get_line_readonly(layout, 0)
        pango_xft_render_layout_line(xftDraw, textColor.ptr, line, textX * PANGO_SCALE, textY * PANGO_SCALE)

        if (monitor.getScreenMode() == ScreenMode.NORMAL) {
            val primBarWidth = 104
            val secBarWidth = textX - primBarWidth - 14 // 8 + 8 - 1 because of first letter offset
            XftDrawRect(xftDraw, primaryBarColor.ptr, 0, TITLE_BAR_OFFSET,  primBarWidth.convert(), BAR_HEIGHT.convert())
            XftDrawRect(xftDraw, secondaryBarColor.ptr, primBarWidth + 8, TITLE_BAR_OFFSET,  secBarWidth.convert(), BAR_HEIGHT.convert())
        } else {
            val barWidth = textX - 7 // 8 - 1 because of first letter offset
            XftDrawRect(xftDraw, primaryBarColor.ptr, 0, TITLE_BAR_OFFSET,  barWidth.convert(), BAR_HEIGHT.convert())
        }

        XSetWindowBackgroundPixmap(screen.display, window.titleBar, pixmap)
        XClearWindow(screen.display, window.titleBar)
        XFreePixmap(screen.display, pixmap)
        nativeHeap.free(rect.rawPtr)
    }

    fun close() {
        layout?.let { nativeHeap.free(it.rawValue) }
        pango_font_description_free(font)
        pango?.let { nativeHeap.free(it.rawValue) }

        nativeHeap.free(activeTextColor.rawPtr)
        nativeHeap.free(inactiveTextColor.rawPtr)
        nativeHeap.free(primaryBarColor.rawPtr)
        nativeHeap.free(secondaryBarColor.rawPtr)
        nativeHeap.free(backgroundColor.rawPtr)
    }
}