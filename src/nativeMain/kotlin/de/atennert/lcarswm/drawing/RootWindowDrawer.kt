package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.*
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.DrawApi
import kotlinx.cinterop.*
import xlib.*

/**
 * Class for drawing the root window decorations.
 */
class RootWindowDrawer(
    private val drawApi: DrawApi,
    private val monitorManager: MonitorManager,
    private val screen: Screen,
    colors: Colors
) : UIDrawing {
    private val graphicsContexts = colors.loadForegroundGraphicContexts(screen.root, colors.colorMap.second)

    private val logoImage: CPointer<XImage>

    init {
        val imageArray = nativeHeap.allocArrayOfPointersTo(nativeHeap.alloc<XImage>())
        drawApi.readXpmFileToImage("/usr/share/pixmaps/lcarswm.xpm", imageArray)
        logoImage = imageArray[0]!!
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

    fun cleanupGraphicsContexts() {
        graphicsContexts.forEach { drawApi.freeGC(it) }
    }

    private fun drawMaximizedFrame(monitor: Monitor, pixmap: Pixmap) {
        clearScreen(monitor, pixmap)

        val gcPurple2 = graphicsContexts[6]
        val gcOrchid = graphicsContexts[2]
        val gcCopyImage = drawApi.createGC(screen.root, 0.convert(), null)!!

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
        bars[0].x = (monitor.x + 48 + logoImage.pointed.width).convert()
        bars[0].y = monitor.y.toShort()
        bars[0].width = (monitor.width - 88 - logoImage.pointed.width).convert()
        bars[0].height = 40.convert()

        // bottom bar
        bars[1].x = (monitor.x + 40).convert()
        bars[1].y = (monitor.y + monitor.height - 40).convert()
        bars[1].width = (monitor.width - 80).convert()
        bars[1].height = 40.convert()

        drawApi.fillArcs(pixmap, gcPurple2, arcs, 4)
        drawApi.fillRectangles(pixmap, gcPurple2, rects, 4)
        drawApi.fillRectangles(pixmap, gcOrchid, bars, 2)

        drawApi.putImage(pixmap, gcCopyImage,
            logoImage, monitor.x + 40, monitor.y,
            logoImage.pointed.width.convert(), logoImage.pointed.height.convert())

        nativeHeap.free(arcs)
        nativeHeap.free(rects)

        drawApi.freeGC(gcCopyImage)
    }

    private fun drawNormalFrame(monitor: Monitor, pixmap: Pixmap) {
        clearScreen(monitor, pixmap)

        val gcBlack = graphicsContexts[0]
        val gcPurple2 = graphicsContexts[6]
        val gcOrchid = graphicsContexts[2]
        val gcPurple1 = graphicsContexts[3]
        val gcBrick = graphicsContexts[4]
        val gcCopyImage = drawApi.createGC(screen.root, 0.convert(), null)!!

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
        arcs[1].y = (monitor.y + 176).convert()

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
        rects[1].y = (monitor.y + 176).convert()

        rects[2].x = (monitor.x + monitor.width - 32).convert()
        rects[2].y = (monitor.y + monitor.height - 40).convert()

        val bigBars = nativeHeap.allocArray<XRectangle>(2)
        bigBars[0].x = (monitor.x + 290).convert()
        bigBars[0].y = monitor.y.convert()
        bigBars[0].width = (monitor.width - 338 - logoImage.pointed.width).convert()
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
        middleBars[0].y = (monitor.y + 176).convert()
        middleBars[0].width = (middleSegmentWidth * 6 - 32).convert()
        middleBars[0].height = 16.convert()

        middleBars[1].x = (monitor.x + 240 + middleSegmentWidth * 6).convert()
        middleBars[1].y = (monitor.y + 176).convert()
        middleBars[1].width = (middleSegmentWidth * 2).convert()
        middleBars[1].height = 16.convert()

        // lower middle bars
        middleBars[2].x = (monitor.x + 232 + 32).convert()
        middleBars[2].y = (monitor.y + 200).convert()
        middleBars[2].width = (middleSegmentWidth * 3 - 32).convert()
        middleBars[2].height = 16.convert()

        middleBars[3].x = (monitor.x + 240 + middleSegmentWidth * 3).convert()
        middleBars[3].y = (monitor.y + 200).convert()
        middleBars[3].width = (middleSegmentWidth * 5).convert()
        middleBars[3].height = 16.convert()

        val sideBars = nativeHeap.allocArray<XRectangle>(2)
        sideBars[0].x = monitor.x.convert()
        sideBars[0].y = (monitor.y + 64).convert()
        sideBars[0].width = SIDE_BAR_WIDTH.convert()
        sideBars[0].height = DATA_AREA_HEIGHT.convert()

        sideBars[1].x = monitor.x.convert()
        sideBars[1].y = (monitor.y + 240).convert()
        sideBars[1].width = SIDE_BAR_WIDTH.convert()
        sideBars[1].height = (monitor.height - 304).convert()

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

        cornerOuterArcs[1].y = (monitor.y + 160).convert()
        cornerOuterArcs[1].width = 32.convert()
        cornerOuterArcs[1].height = 32.convert()
        cornerOuterArcs[1].angle1 = 180.shl(6)

        cornerOuterArcs[2].y = (monitor.y + 200).convert()
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
        cornerRects[0].y = (monitor.y + 40).convert()
        cornerRects[1].y = (monitor.y + 160).convert()
        cornerRects[2].y = (monitor.y + 216).convert()
        cornerRects[3].y = (monitor.y + monitor.height - 56).convert()

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
        cornerRects[6].y = (monitor.y + 176).convert()
        cornerRects[7].y = (monitor.y + 200).convert()

        val cornerInnerArcs = nativeHeap.allocArray<XArc>(4)
        for (i in 0 until 4) {
            cornerInnerArcs[i].x = (monitor.x + 184).convert()
            cornerInnerArcs[i].width = 32.convert()
            cornerInnerArcs[i].height = 32.convert()
            cornerInnerArcs[i].angle2 = 90.shl(6)
        }
        cornerInnerArcs[0].y = (monitor.y + 40).convert()
        cornerInnerArcs[0].angle1 = 90.shl(6)

        cornerInnerArcs[1].y = (monitor.y + 144).convert()
        cornerInnerArcs[1].angle1 = 180.shl(6)

        cornerInnerArcs[2].y = (monitor.y + 216).convert()
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

        drawApi.putImage(pixmap, gcCopyImage,
            logoImage, monitor.x + monitor.width - 40 - logoImage.pointed.width, monitor.y,
            logoImage.pointed.width.convert(), logoImage.pointed.height.convert())

        nativeHeap.free(arcs)
        nativeHeap.free(rects)
        nativeHeap.free(bigBars)
        nativeHeap.free(middleBars)
        nativeHeap.free(sideBars)
        nativeHeap.free(cornerOuterArcs)
        nativeHeap.free(cornerRects)
        nativeHeap.free(cornerInnerArcs)

        drawApi.freeGC(gcCopyImage)
    }

    private fun clearScreen(monitor: Monitor, pixmap: Pixmap) {
        drawApi.fillRectangle(pixmap, graphicsContexts[0], monitor.x, monitor.y, monitor.width.convert(), monitor.height.convert())
    }
}
