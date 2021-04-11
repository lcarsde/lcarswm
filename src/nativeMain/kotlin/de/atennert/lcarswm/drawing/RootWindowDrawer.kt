package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.*
import de.atennert.lcarswm.lifecycle.closeWith
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
    private val graphicsContexts = colorFactory.loadForegroundGraphicContexts(screen.root, colorFactory.colorPixels)

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

        closeWith(RootWindowDrawer::close)
    }

    override fun drawWindowManagerFrame() {
        val screenSize = monitorManager.getCombinedScreenSize()
        val pixmap = drawApi.createPixmap(screen.root, screenSize.first.convert(), screenSize.second.convert(), screen.root_depth.convert())
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

    private fun close() {
        graphicsContexts.forEach { drawApi.freeGC(it) }
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

        drawApi.fillRectangle(pixmap, graphicsContexts[0],
            x, y,
            (rect.width + 16 - 1).convert(), BAR_HEIGHT.convert())

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

        drawApi.fillRectangle(pixmap, graphicsContexts[0],
            logoX + 1, y,
            (rect.width + 16 - 1).convert(), BAR_HEIGHT.convert())

        val line = fontApi.getLayoutLineReadonly(fontProvider.layout, 0)

        val xftDraw = drawApi.xftDrawCreate(pixmap, screen.root_visual!!, colorMap)
        fontApi.xftRenderLayoutLine(xftDraw, logoColor.ptr, line, (logoX + 8) * PANGO_SCALE, textY * PANGO_SCALE)

        nativeHeap.free(rect.rawPtr)
    }

    private fun drawLogo(pixmap: Pixmap, x: Int, y: Int) {
        if (logoImage == null) return

        val gcCopyImage = drawApi.createGC(screen.root, 0.convert(), null)!!

        drawApi.fillRectangle(pixmap, graphicsContexts[0],
            x, y,
            (logoImage.pointed.width + 16).convert(), BAR_HEIGHT.convert())

        drawApi.putImage(pixmap, gcCopyImage,
            logoImage, x + 8, y,
            logoImage.pointed.width.convert(), BAR_HEIGHT.convert())

        drawApi.freeGC(gcCopyImage)
    }

    private fun drawMaximizedFrame(monitor: Monitor, pixmap: Pixmap) {
        clearScreen(monitor, pixmap)

        val gcPurple2 = graphicsContexts[6]
        val gcOrchid = graphicsContexts[2]

        // TODO create bar ends as pixmaps
        val arcs = nativeHeap.allocArray<XArc>(4)
        for (i in 0 until 4) {
            arcs[i].width = 40.convert()
            arcs[i].height = 40.convert()
            arcs[i].angle2 = 180.shl(6)
        }
        arcs[0].x = monitor.x.convert()
        arcs[0].y = monitor.y.convert()
        arcs[0].angle1 = 90.shl(6)

        arcs[1].x = monitor.x.convert()
        arcs[1].y = (monitor.y + monitor.height - 40).convert()
        arcs[1].angle1 = 90.shl(6)

        arcs[2].x = (monitor.x + monitor.width - 40).convert()
        arcs[2].y = monitor.y.convert()
        arcs[2].angle1 = 270.shl(6)

        arcs[3].x = (monitor.x + monitor.width - 40).convert()
        arcs[3].y = (monitor.y + monitor.height - 40).convert()
        arcs[3].angle1 = 270.shl(6)

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

        drawApi.fillArcs(pixmap, gcPurple2, arcs, 4)
        drawApi.fillRectangles(pixmap, gcPurple2, rects, 4)
        drawApi.fillRectangles(pixmap, gcOrchid, bars, 2)

        if (logoImage != null) {
            drawLogo(pixmap, monitor.x + 32, monitor.y)
        } else {
            drawLogoTextFront(pixmap, monitor.x + 32, monitor.y, monitor.width - 80)
        }

        nativeHeap.free(arcs)
        nativeHeap.free(rects)
    }

    private fun drawNormalFrame(monitor: Monitor, pixmap: Pixmap) {
        clearScreen(monitor, pixmap)

        val gcBlack = graphicsContexts[0]
        val gcPurple2 = graphicsContexts[6]
        val gcOrchid = graphicsContexts[2]
        val gcPurple1 = graphicsContexts[3]
        val gcBrick = graphicsContexts[4]

        // TODO create bar ends as pixmaps
        val arcs = nativeHeap.allocArray<XArc>(3)
        for (i in 0 until 3) {
            arcs[i].width = 40.convert()
            arcs[i].height = 40.convert()
            arcs[i].angle1 = 270.shl(6)
            arcs[i].angle2 = 180.shl(6)
        }
        arcs[0].x = (monitor.x + monitor.width - 40).convert()
        arcs[0].y = monitor.y.convert()

        arcs[1].x = (monitor.x + monitor.width - 40).convert()
        arcs[1].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()

        arcs[2].x = (monitor.x + monitor.width - 40).convert()
        arcs[2].y = (monitor.y + monitor.height - 40).convert()

        val rects = nativeHeap.allocArray<XRectangle>(3)
        // extensions for round pieces
        for (i in 0 until 3) {
            rects[i].width = 12.convert()
            rects[i].height = 40.convert()
        }
        rects[0].x = (monitor.x + monitor.width - 32).convert()
        rects[0].y = monitor.y.convert()

        rects[1].x = (monitor.x + monitor.width - 32).convert()
        rects[1].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()

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
        middleBars[0].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        middleBars[0].width = (middleSegmentWidth * 6 - 32).convert()
        middleBars[0].height = 16.convert()

        middleBars[1].x = (monitor.x + 240 + middleSegmentWidth * 6).convert()
        middleBars[1].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        middleBars[1].width = (middleSegmentWidth * 2).convert()
        middleBars[1].height = 16.convert()

        // lower middle bars
        middleBars[2].x = (monitor.x + 232 + 32).convert()
        middleBars[2].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 3*BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL).convert()
        middleBars[2].width = (middleSegmentWidth * 3 - 32).convert()
        middleBars[2].height = 16.convert()

        middleBars[3].x = (monitor.x + 240 + middleSegmentWidth * 3).convert()
        middleBars[3].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 3*BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL).convert()
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

        // TODO create corners as pixmaps
        val cornerOuterArcs = nativeHeap.allocArray<XArc>(4)
        for (i in 0 until 4) {
            cornerOuterArcs[i].x = monitor.x.convert()
            cornerOuterArcs[i].angle2 = 90.shl(6)
        }
        cornerOuterArcs[0].y = (monitor.y).convert()
        cornerOuterArcs[0].width = 80.convert()
        cornerOuterArcs[0].height = 80.convert()
        cornerOuterArcs[0].angle1 = 90.shl(6)

        cornerOuterArcs[1].y = (monitor.y + BAR_HEIGHT + INNER_CORNER_RADIUS + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        cornerOuterArcs[1].width = 32.convert()
        cornerOuterArcs[1].height = 32.convert()
        cornerOuterArcs[1].angle1 = 180.shl(6)

        cornerOuterArcs[2].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 3*BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL).convert()
        cornerOuterArcs[2].width = 32.convert()
        cornerOuterArcs[2].height = 32.convert()
        cornerOuterArcs[2].angle1 = 90.shl(6)

        cornerOuterArcs[3].y = (monitor.y + monitor.height - 80).convert()
        cornerOuterArcs[3].width = 80.convert()
        cornerOuterArcs[3].height = 80.convert()
        cornerOuterArcs[3].angle1 = 180.shl(6)

        val cornerRects = nativeHeap.allocArray<XRectangle>(8)
        for (i in 0 until 4) {
            cornerRects[i].x = monitor.x.convert()
            cornerRects[i].width = 200.convert()
            cornerRects[i].height = 16.convert()
        }
        cornerRects[0].y = (monitor.y + BAR_HEIGHT).convert()
        cornerRects[1].y = (monitor.y + BAR_HEIGHT + INNER_CORNER_RADIUS + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        cornerRects[2].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 3*BAR_GAP_SIZE + DATA_BAR_HEIGHT + 2*BAR_HEIGHT_SMALL).convert()
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
        cornerRects[6].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        cornerRects[7].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 3*BAR_GAP_SIZE + DATA_BAR_HEIGHT + BAR_HEIGHT_SMALL).convert()

        val cornerInnerArcs = nativeHeap.allocArray<XArc>(4)
        for (i in 0 until 4) {
            cornerInnerArcs[i].x = (monitor.x + 184).convert()
            cornerInnerArcs[i].width = 32.convert()
            cornerInnerArcs[i].height = 32.convert()
            cornerInnerArcs[i].angle2 = 90.shl(6)
        }
        cornerInnerArcs[0].y = (monitor.y + 40).convert()
        cornerInnerArcs[0].angle1 = 90.shl(6)

        cornerInnerArcs[1].y = (monitor.y + BAR_HEIGHT + 2*BAR_GAP_SIZE + DATA_BAR_HEIGHT).convert()
        cornerInnerArcs[1].angle1 = 180.shl(6)

        cornerInnerArcs[2].y = (monitor.y + BAR_HEIGHT + 2*INNER_CORNER_RADIUS + 3*BAR_GAP_SIZE + DATA_BAR_HEIGHT + 2*BAR_HEIGHT_SMALL).convert()
        cornerInnerArcs[2].angle1 = 90.shl(6)

        cornerInnerArcs[3].y = (monitor.y + monitor.height - 72).convert()
        cornerInnerArcs[3].angle1 = 180.shl(6)

        drawApi.fillArcs(pixmap, gcPurple2, arcs, 3)
        drawApi.fillRectangles(pixmap, gcPurple2, rects, 3)
        drawApi.fillRectangles(pixmap, gcPurple2, bigBars, 2)

        // middle bars
        drawApi.fillRectangles(pixmap, gcPurple1, middleBars[0].ptr, 1)
        drawApi.fillRectangles(pixmap, gcBrick, middleBars[1].ptr, 1)
        drawApi.fillRectangles(pixmap, gcPurple2, middleBars[2].ptr, 1)
        drawApi.fillRectangles(pixmap, gcOrchid, middleBars[3].ptr, 1)

        // side bars
        drawApi.fillRectangles(pixmap, gcPurple1, sideBars, 2)

        // corner pieces
        drawApi.fillArcs(pixmap, gcOrchid, cornerOuterArcs, 4)
        drawApi.fillRectangles(pixmap, gcOrchid, cornerRects, 8)
        drawApi.fillArcs(pixmap, gcBlack, cornerInnerArcs, 4)

        if (logoImage != null) {
            drawLogo(pixmap, monitor.x + monitor.width - 48 - logoImage.pointed.width, monitor.y)
        } else {
            drawLogoTextBack(pixmap, monitor.x + 290, monitor.y, monitor.width - 330)
        }

        nativeHeap.free(arcs)
        nativeHeap.free(rects)
        nativeHeap.free(bigBars)
        nativeHeap.free(middleBars)
        nativeHeap.free(sideBars)
        nativeHeap.free(cornerOuterArcs)
        nativeHeap.free(cornerRects)
        nativeHeap.free(cornerInnerArcs)
    }

    private fun clearScreen(monitor: Monitor, pixmap: Pixmap) {
        drawApi.fillRectangle(pixmap, graphicsContexts[0], monitor.x, monitor.y, monitor.width.convert(), monitor.height.convert())
    }
}
