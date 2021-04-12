package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.*
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.settings.GeneralSetting
import de.atennert.lcarswm.system.api.DrawApi
import de.atennert.lcarswm.system.api.FontApi
import kotlinx.cinterop.*
import xlib.*

/**
 * Class for drawing the root window decorations.
 */
class RootWindowDrawer(
    private val drawApi: DrawApi,
    private val fontApi: FontApi,
    private val monitorManager: MonitorManager,
    private val screen: Screen,
    private val colorFactory: ColorFactory,
    settings: Map<GeneralSetting, String>,
    private val fontProvider: FontProvider
) : UIDrawing {
    private val barEndOpacities = getFilledArcOpacities(BAR_HEIGHT / 2)
    private val bigOuterCornerOpacities = getFilledArcOpacities(OUTER_CORNER_RADIUS_BIG)
    private val smallOuterCornerOpacities = getFilledArcOpacities(OUTER_CORNER_RADIUS_SMALL)
    private val innerCornerOpacities = invertOpacities(getFilledArcOpacities(INNER_CORNER_RADIUS))

    private val barEndLeftColors = getArcs(COLOR_BAR_ENDS, barEndOpacities, BAR_HEIGHT / 2, 2, 3)
    private val barEndRightColors = getArcs(COLOR_BAR_ENDS, barEndOpacities, BAR_HEIGHT / 2, 1, 4)
    private val corner1OuterColors = getArcs(COLOR_NORMAL_CORNER_1, bigOuterCornerOpacities, OUTER_CORNER_RADIUS_BIG, 2)
    private val corner4OuterColors = getArcs(COLOR_NORMAL_CORNER_4, bigOuterCornerOpacities, OUTER_CORNER_RADIUS_BIG, 3)
    private val corner2OuterColors = getArcs(COLOR_NORMAL_CORNER_2, smallOuterCornerOpacities, OUTER_CORNER_RADIUS_SMALL, 3)
    private val corner3OuterColors = getArcs(COLOR_NORMAL_CORNER_3, smallOuterCornerOpacities, OUTER_CORNER_RADIUS_SMALL, 2)
    private val corner1InnerColors = getArcs(COLOR_NORMAL_CORNER_1, innerCornerOpacities, INNER_CORNER_RADIUS, 2)
    private val corner4InnerColors = getArcs(COLOR_NORMAL_CORNER_4, innerCornerOpacities, INNER_CORNER_RADIUS, 3)
    private val corner2InnerColors = getArcs(COLOR_NORMAL_CORNER_2, innerCornerOpacities, INNER_CORNER_RADIUS, 3)
    private val corner3InnerColors = getArcs(COLOR_NORMAL_CORNER_3, innerCornerOpacities, INNER_CORNER_RADIUS, 2)

    private fun getArcs(
        baseColor: Color,
        opacities: List<Triple<Int, Int, Double>>,
        radius: Int,
        vararg quadrants: Int
    ): List<Triple<Int, Int, Color>> {
        val colors = mutableListOf<Triple<Int, Int, Color>>()
        val adjust = { opacity: Double ->
            baseColor.run { Color((red * opacity).toInt(), (green * opacity).toInt(), (blue * opacity).toInt()) }
        }

        for ((x, y, opacity) in opacities) {
            for (quadrant in quadrants) {
                when (quadrant) {
                    1 -> colors.add(Triple(x + radius, -y + radius - 1, adjust(opacity)))
                    2 -> colors.add(Triple(-x + radius - 1, -y + radius - 1, adjust(opacity)))
                    3 -> colors.add(Triple(-x + radius - 1, y + radius, adjust(opacity)))
                    4 -> colors.add(Triple(x + radius, y + radius, adjust(opacity)))
                    else -> throw IllegalArgumentException("There are only quadrants 1 to 4, not $quadrant")
                }
            }
        }
        return colors
    }

    private fun invertOpacities(opacities: List<Triple<Int, Int, Double>>): List<Triple<Int, Int, Double>> =
        opacities.map { (x , y, opacity) -> Triple(x, y, 1 - opacity) }

    private val logoImage: CPointer<XImage>?
    private val logoText: String

    private val logoColor = colorFactory.createXftColor(COLOR_LOGO)

    private val colorMap: Colormap
        get() = colorFactory.colorMapId

    init {
        val imageArray = nativeHeap.allocArrayOfPointersTo(nativeHeap.alloc<XImage>())
        val wmLogoPath = settings[GeneralSetting.TITLE_IMAGE]
        logoImage = if (wmLogoPath != null) {
            drawApi.readXpmFileToImage(wmLogoPath, imageArray)
            imageArray[0]!!
        } else {
            null
        }
        logoText = settings[GeneralSetting.TITLE] ?: "LCARS"
    }

    override fun drawWindowManagerFrame() {
        val screenSize = monitorManager.getCombinedScreenSize()
        val pixmap = drawApi.createPixmap(
            screen.root,
            screenSize.first.convert(),
            screenSize.second.convert(),
            screen.root_depth.convert()
        )
        monitorManager.getMonitors().forEach {
            when (it.getScreenMode()) {
                ScreenMode.NORMAL -> drawNormalFrame(it, pixmap)
                ScreenMode.MAXIMIZED -> drawMaximizedFrame(it, pixmap)
                ScreenMode.FULLSCREEN -> clearScreen(it, pixmap)
            }
        }
        drawApi.setWindowBackgroundPixmap(screen.root, pixmap)
        drawApi.clearWindow(screen.root)
        drawApi.freePixmap(pixmap)
    }

    private fun drawLogoTextFront(pixmap: Pixmap, x: Int, y: Int, barWidth: Int) {
        val rect = nativeHeap.alloc<PangoRectangle>()
        val maxTextWidth = barWidth - 16

        val textY = y + (((BAR_HEIGHT_WITH_OFFSET * PANGO_SCALE)
                - (fontProvider.ascent + fontProvider.descent))
                / 2 + fontProvider.ascent) / PANGO_SCALE

        fontApi.setLayoutText(fontProvider.layout, logoText)
        fontApi.setLayoutWidth(fontProvider.layout, maxTextWidth * PANGO_SCALE)
        fontApi.getLayoutPixelExtents(fontProvider.layout, rect.ptr)

        val backgroundGC = getGC(COLOR_BACKGROUND)
        drawApi.fillRectangle(
            pixmap, backgroundGC,
            x, y,
            (rect.width + 16 - 1).convert(), BAR_HEIGHT.convert()
        )

        val line = fontApi.getLayoutLineReadonly(fontProvider.layout, 0)

        val xftDraw = drawApi.xftDrawCreate(pixmap, screen.root_visual!!, colorMap)
        fontApi.xftRenderLayoutLine(xftDraw, logoColor.ptr, line, (x + 8 - 1) * PANGO_SCALE, textY * PANGO_SCALE)

        nativeHeap.free(rect.rawPtr)
    }

    private fun drawLogoTextBack(pixmap: Pixmap, barX: Int, y: Int, barWidth: Int) {
        val rect = nativeHeap.alloc<PangoRectangle>()
        val maxTextWidth = barWidth - 16

        val textY = y + (((BAR_HEIGHT_WITH_OFFSET * PANGO_SCALE)
                - (fontProvider.ascent + fontProvider.descent))
                / 2 + fontProvider.ascent) / PANGO_SCALE

        fontApi.setLayoutText(fontProvider.layout, logoText)
        fontApi.setLayoutWidth(fontProvider.layout, maxTextWidth * PANGO_SCALE)
        fontApi.getLayoutPixelExtents(fontProvider.layout, rect.ptr)
        val logoX = barX + barWidth - rect.width - 8

        val backgroundGC = getGC(COLOR_BACKGROUND)
        drawApi.fillRectangle(
            pixmap, backgroundGC,
            logoX + 1, y,
            (rect.width + 16 - 1).convert(), BAR_HEIGHT.convert()
        )

        val line = fontApi.getLayoutLineReadonly(fontProvider.layout, 0)

        val xftDraw = drawApi.xftDrawCreate(pixmap, screen.root_visual!!, colorMap)
        fontApi.xftRenderLayoutLine(xftDraw, logoColor.ptr, line, (logoX + 8) * PANGO_SCALE, textY * PANGO_SCALE)

        nativeHeap.free(rect.rawPtr)
    }

    private fun drawLogo(pixmap: Pixmap, x: Int, y: Int) {
        if (logoImage == null) return

        val gcCopyImage = drawApi.createGC(screen.root, 0.convert(), null)!!

        val backgroundGC = getGC(COLOR_BACKGROUND)
        drawApi.fillRectangle(
            pixmap, backgroundGC,
            x, y,
            (logoImage.pointed.width + 16).convert(), BAR_HEIGHT.convert()
        )

        drawApi.putImage(
            pixmap, gcCopyImage,
            logoImage, x + 8, y,
            logoImage.pointed.width.convert(), BAR_HEIGHT.convert()
        )

        drawApi.freeGC(gcCopyImage)
    }

    private fun drawMaximizedFrame(monitor: Monitor, pixmap: Pixmap) {
        clearScreen(monitor, pixmap)

        val barEndsGC = getGC(COLOR_BAR_ENDS)
        val maxBarUpGC = getGC(COLOR_MAX_BAR_UP)
        val maxBarDownGC = getGC(COLOR_MAX_BAR_DOWN)

        val rects = nativeHeap.allocArray<XRectangle>(4)
        // extensions for round pieces
        for (i in 0 until 4) {
            rects[i].width = 12.convert()
            rects[i].height = 40.convert()
        }
        rects[0].x = (monitor.x + 20).convert()
        rects[0].y = monitor.y.convert()

        rects[1].x = (monitor.x + 20).convert()
        rects[1].y = (monitor.y + monitor.height - 40).convert()

        rects[2].x = (monitor.x + monitor.width - 32).convert()
        rects[2].y = monitor.y.convert()

        rects[3].x = (monitor.x + monitor.width - 32).convert()
        rects[3].y = (monitor.y + monitor.height - 40).convert()

        val bars = nativeHeap.allocArray<XRectangle>(2)
        // top bar
        bars[0].x = (monitor.x + 40).convert()
        bars[0].y = monitor.y.toShort()
        bars[0].width = (monitor.width - 80).convert()
        bars[0].height = 40.convert()

        // bottom bar
        bars[1].x = (monitor.x + 40).convert()
        bars[1].y = (monitor.y + monitor.height - 40).convert()
        bars[1].width = (monitor.width - 80).convert()
        bars[1].height = 40.convert()

        drawApi.fillRectangles(pixmap, barEndsGC, rects, 4)
        drawApi.fillRectangle(
            pixmap,
            maxBarUpGC,
            bars[0].x.toInt(),
            bars[0].y.toInt(),
            bars[0].width.convert(),
            bars[0].height.convert()
        )
        drawApi.fillRectangle(
            pixmap,
            maxBarDownGC,
            bars[1].x.toInt(),
            bars[1].y.toInt(),
            bars[1].width.convert(),
            bars[1].height.convert()
        )

        // left bar end
        for ((x, y, color) in barEndLeftColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + x, monitor.y + y)
            drawApi.drawPoint(pixmap, gc, monitor.x + x, monitor.y + monitor.height - BAR_HEIGHT + y)
        }

        // right bar end
        for ((x, y, color) in barEndRightColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + monitor.width - 40 + x, monitor.y + y)
            drawApi.drawPoint(pixmap, gc, monitor.x + monitor.width - 40 + x, monitor.y + monitor.height - BAR_HEIGHT + y)
        }

        if (logoImage != null) {
            drawLogo(pixmap, monitor.x + 32, monitor.y)
        } else {
            drawLogoTextFront(pixmap, monitor.x + 32, monitor.y, monitor.width - 80)
        }

        nativeHeap.free(rects)
    }

    private fun drawNormalFrame(monitor: Monitor, pixmap: Pixmap) {
        clearScreen(monitor, pixmap)

        val barEndGC = getGC(COLOR_BAR_ENDS)
        val barUpGC = getGC(COLOR_NORMAL_BAR_UP)
        val barDownGC = getGC(COLOR_NORMAL_BAR_DOWN)
        val sideBarUpGC = getGC(COLOR_NORMAL_SIDEBAR_UP)
        val sideBarDownGC = getGC(COLOR_NORMAL_SIDEBAR_DOWN)
        val middleBar1GC = getGC(COLOR_NORMAL_BAR_MIDDLE_1)
        val middleBar2GC = getGC(COLOR_NORMAL_BAR_MIDDLE_2)
        val middleBar3GC = getGC(COLOR_NORMAL_BAR_MIDDLE_3)
        val middleBar4GC = getGC(COLOR_NORMAL_BAR_MIDDLE_4)
        val corner1GC = getGC(COLOR_NORMAL_CORNER_1)
        val corner2GC = getGC(COLOR_NORMAL_CORNER_2)
        val corner3GC = getGC(COLOR_NORMAL_CORNER_3)
        val corner4GC = getGC(COLOR_NORMAL_CORNER_4)

        val rects = nativeHeap.allocArray<XRectangle>(3)
        // extensions for round pieces
        for (i in 0 until 3) {
            rects[i].width = 12.convert()
            rects[i].height = 40.convert()
        }
        rects[0].x = (monitor.x + monitor.width - 32).convert()
        rects[0].y = monitor.y.convert()

        rects[1].x = (monitor.x + monitor.width - 32).convert()
        rects[1].y = (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()

        rects[2].x = (monitor.x + monitor.width - 32).convert()
        rects[2].y = (monitor.y + monitor.height - 40).convert()

        val bigBars = nativeHeap.allocArray<XRectangle>(2)
        bigBars[0].x = (monitor.x + 290).convert()
        bigBars[0].y = monitor.y.convert()
        bigBars[0].width = (monitor.width - 330).convert()
        bigBars[0].height = 40.convert()

        // bottom bar
        bigBars[1].x = (monitor.x + 320).convert()
        bigBars[1].y = (monitor.y + monitor.height - 40).convert()
        bigBars[1].width = (monitor.width - 360).convert()
        bigBars[1].height = 40.convert()

        val middleBars = nativeHeap.allocArray<XRectangle>(4)
        val middleSegmentWidth = (monitor.width - 280) / 8

        // upper middle bars
        middleBars[0].x = (monitor.x + 232 + 32).convert()
        middleBars[0].y =
            (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        middleBars[0].width = (middleSegmentWidth * 6 - 32).convert()
        middleBars[0].height = 16.convert()

        middleBars[1].x = (monitor.x + 240 + middleSegmentWidth * 6).convert()
        middleBars[1].y =
            (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        middleBars[1].width = (middleSegmentWidth * 2).convert()
        middleBars[1].height = 16.convert()

        // lower middle bars
        middleBars[2].x = (monitor.x + 232 + 32).convert()
        middleBars[2].y =
            (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL).convert()
        middleBars[2].width = (middleSegmentWidth * 3 - 32).convert()
        middleBars[2].height = 16.convert()

        middleBars[3].x = (monitor.x + 240 + middleSegmentWidth * 3).convert()
        middleBars[3].y =
            (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL).convert()
        middleBars[3].width = (middleSegmentWidth * 5).convert()
        middleBars[3].height = 16.convert()

        val sideBars = nativeHeap.allocArray<XRectangle>(2)
        sideBars[0].x = monitor.x.convert()
        sideBars[0].y = (monitor.y + BAR_HEIGHT + INNER_CORNER_RADIUS + BAR_GAP_SIZE).convert()
        sideBars[0].width = SIDE_BAR_WIDTH.convert()
        sideBars[0].height = DATA_BAR_HEIGHT.convert()

        sideBars[1].x = monitor.x.convert()
        sideBars[1].y = (monitor.y + NORMAL_WINDOW_UPPER_OFFSET + INNER_CORNER_RADIUS).convert()
        sideBars[1].width = SIDE_BAR_WIDTH.convert()
        sideBars[1].height = (monitor.height - NORMAL_WINDOW_NON_APP_HEIGHT).convert()

        val cornerRects = nativeHeap.allocArray<XRectangle>(8)
        for (i in 0 until 4) {
            cornerRects[i].x = monitor.x.convert()
            cornerRects[i].width = 200.convert()
            cornerRects[i].height = 16.convert()
        }
        cornerRects[0].y = (monitor.y + BAR_HEIGHT).convert()
        cornerRects[1].y = (monitor.y + BAR_HEIGHT + INNER_CORNER_RADIUS + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        cornerRects[2].y =
            (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + 2 * BAR_HEIGHT_SMALL).convert()
        cornerRects[3].y = (monitor.y + monitor.height - BAR_HEIGHT - INNER_CORNER_RADIUS).convert()

        for (i in 4 until 6) {
            cornerRects[i].x = (monitor.x + OUTER_CORNER_RADIUS_BIG).convert()
            cornerRects[i].height = BAR_HEIGHT.convert()
        }
        cornerRects[4].y = monitor.y.convert()
        cornerRects[4].width = 242.convert()
        cornerRects[5].y = (monitor.y + monitor.height - BAR_HEIGHT).convert()
        cornerRects[5].width = 272.convert()

        for (i in 6 until 8) {
            cornerRects[i].x = (monitor.x + OUTER_CORNER_RADIUS_SMALL).convert()
            cornerRects[i].width = 240.convert()
            cornerRects[i].height = BAR_HEIGHT_SMALL.convert()
        }
        cornerRects[6].y =
            (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        cornerRects[7].y =
            (monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL).convert()

        drawApi.fillRectangles(pixmap, barEndGC, rects, 3)
        drawApi.fillRectangles(pixmap, barUpGC, bigBars[0].ptr, 1)
        drawApi.fillRectangles(pixmap, barDownGC, bigBars[1].ptr, 1)

        // middle bars
        drawApi.fillRectangles(pixmap, middleBar1GC, middleBars[0].ptr, 1)
        drawApi.fillRectangles(pixmap, middleBar2GC, middleBars[1].ptr, 1)
        drawApi.fillRectangles(pixmap, middleBar3GC, middleBars[2].ptr, 1)
        drawApi.fillRectangles(pixmap, middleBar4GC, middleBars[3].ptr, 1)

        // side bars
        drawApi.fillRectangles(pixmap, sideBarUpGC, sideBars[0].ptr, 1)
        drawApi.fillRectangles(pixmap, sideBarDownGC, sideBars[1].ptr, 1)

        // corner pieces
        drawApi.fillRectangles(pixmap, corner1GC, cornerRects[0].ptr, 1)
        drawApi.fillRectangles(pixmap, corner2GC, cornerRects[1].ptr, 1)
        drawApi.fillRectangles(pixmap, corner3GC, cornerRects[2].ptr, 1)
        drawApi.fillRectangles(pixmap, corner4GC, cornerRects[3].ptr, 1)
        drawApi.fillRectangles(pixmap, corner1GC, cornerRects[4].ptr, 1)
        drawApi.fillRectangles(pixmap, corner4GC, cornerRects[5].ptr, 1)
        drawApi.fillRectangles(pixmap, corner2GC, cornerRects[6].ptr, 1)
        drawApi.fillRectangles(pixmap, corner3GC, cornerRects[7].ptr, 1)

        // right bar end
        for ((x, y, color) in barEndRightColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + monitor.width - 40 + x, monitor.y + y)
            drawApi.drawPoint(pixmap, gc, monitor.x + monitor.width - 40 + x, monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + y)
            drawApi.drawPoint(pixmap, gc, monitor.x + monitor.width - 40 + x, monitor.y + monitor.height - BAR_HEIGHT + y)
        }

        // corner 1 outer
        for ((x, y, color) in corner1OuterColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + x, monitor.y + y)
        }

        // corner 2 outer
        for ((x, y, color) in corner2OuterColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + x, monitor.y + BAR_HEIGHT + INNER_CORNER_RADIUS + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + y)
        }

        // corner 3 outer
        for ((x, y, color) in corner3OuterColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + x, monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL + y)
        }

        // corner 4 outer
        for ((x, y, color) in corner4OuterColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + x, monitor.y + monitor.height - OUTER_CORNER_RADIUS_BIG * 2 + y)
        }

        // corner 1 inner
        for ((x, y, color) in corner1InnerColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + SIDE_BAR_WIDTH + x, monitor.y + BAR_HEIGHT + y)
        }

        // corner 2 inner
        for ((x, y, color) in corner2InnerColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + SIDE_BAR_WIDTH + x, monitor.y + BAR_HEIGHT + 2 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + y)
        }

        // corner 3 inner
        for ((x, y, color) in corner3InnerColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + SIDE_BAR_WIDTH + x, monitor.y + BAR_HEIGHT + 2 * INNER_CORNER_RADIUS + 3 * BAR_GAP_SIZE + DATA_BAR_HEIGHT + 2 * BAR_HEIGHT_SMALL + y)
        }

        // corner 4 inner
        for ((x, y, color) in corner4InnerColors) {
            val gc = getGC(color)
            drawApi.drawPoint(pixmap, gc, monitor.x + SIDE_BAR_WIDTH + x, monitor.y + monitor.height - BAR_HEIGHT - 2 * INNER_CORNER_RADIUS + y)
        }

        if (logoImage != null) {
            drawLogo(pixmap, monitor.x + monitor.width - 48 - logoImage.pointed.width, monitor.y)
        } else {
            drawLogoTextBack(pixmap, monitor.x + 290, monitor.y, monitor.width - 330)
        }

        nativeHeap.free(rects)
        nativeHeap.free(bigBars)
        nativeHeap.free(middleBars)
        nativeHeap.free(sideBars)
        nativeHeap.free(cornerRects)
    }

    private fun clearScreen(monitor: Monitor, pixmap: Pixmap) {
        val backgroundGC = getGC(COLOR_BACKGROUND)
        drawApi.fillRectangle(
            pixmap,
            backgroundGC,
            monitor.x,
            monitor.y,
            monitor.width.convert(),
            monitor.height.convert()
        )
    }

    private inline fun getGC(color: Color): GC = colorFactory.createColorGC(screen.root, color)
}
