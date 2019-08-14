package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.*
import xlib.*

val COLORS = listOf(
    Triple(0, 0, 0),
    Triple(0xFFFF, 0x9999, 0),
    Triple(0xCCCC, 0x9999, 0xCCCC),
    Triple(0x9999, 0x9999, 0xCCCC),
    Triple(0xCCCC, 0x6666, 0x6666),
    Triple(0xFFFF, 0xCCCC, 0x9999),
    Triple(0x9999, 0x9999, 0xFFFF),
    Triple(0xFFFF, 0x9999, 0x6666),
    Triple(0xCCCC, 0x6666, 0x9999)
)

val DRAW_FUNCTIONS = hashMapOf(
    Pair(ScreenMode.NORMAL, ::drawNormalFrame),
    Pair(ScreenMode.MAXIMIZED, ::drawMaximizedFrame),
    Pair(ScreenMode.FULLSCREEN, ::clearScreen)
)

fun allocateColorMap(
    display: CPointer<Display>,
    visual: CPointer<Visual>?,
    windowId: ULong
): Pair<Colormap, List<ULong>> {
    val colorMapId = XCreateColormap(display, windowId, visual, AllocNone)

    val colorReplies = COLORS
        .asSequence()
        .map { (red, green, blue) ->
            val color = nativeHeap.alloc<XColor>()
            color.red = red.convert()
            color.green = green.convert()
            color.blue = blue.convert()
            XAllocColor(display, colorMapId, color.ptr)
            color.pixel
        }
        .filterNotNull()
        .toList()

    return Pair(colorMapId, colorReplies)
}

fun getGraphicContexts(
    display: CPointer<Display>,
    window: ULong,
    colors: List<ULong>
): List<GC> = colors
    .map { color ->
        val gcValues = nativeHeap.alloc<XGCValues>()
        gcValues.foreground = color
        gcValues.graphics_exposures = 0
        gcValues.arc_mode = ArcPieSlice

        XCreateGC(display, window, (GCForeground or GCGraphicsExposures or GCArcMode).convert(), gcValues.ptr)!!
    }

fun cleanupColorMap(
    display: CPointer<Display>,
    colorMap: Pair<Colormap, List<ULong>>
) {
    val colorPixels = ULongArray(colorMap.second.size) {colorMap.second[it]}
    XFreeColors(display, colorMap.first, colorPixels.toCValues(), colorPixels.size, 0.convert())
    XFreeColormap(display, colorMap.first)
}

private fun drawMaximizedFrame(
    graphicsContexts: List<GC>,
    lcarsWindow: ULong,
    display: CPointer<Display>,
    monitor: Monitor,
    image: CPointer<XImage>
) {
    clearScreen(graphicsContexts, lcarsWindow, display, monitor, image)

    val gcPurple2 = graphicsContexts[6]
    val gcOrchid = graphicsContexts[2]
    val gcCopyImage = XCreateGC(display, lcarsWindow, 0.convert(), null)

    // TODO create bar ends as pixmaps
    val arcs = nativeHeap.allocArray<XArc>(4)
    for (i in 0 until 4) {
        arcs[i].width = 40.toUShort()
        arcs[i].height = 40.toUShort()
        arcs[i].angle2 = 180.shl(6)
    }
    arcs[0].x = monitor.x.toShort()
    arcs[0].y = monitor.y.toShort()
    arcs[0].angle1 = 90.shl(6)

    arcs[1].x = monitor.x.toShort()
    arcs[1].y = (monitor.y + monitor.height - 40).toShort()
    arcs[1].angle1 = 90.shl(6)

    arcs[2].x = (monitor.x + monitor.width - 40).toShort()
    arcs[2].y = monitor.y.toShort()
    arcs[2].angle1 = 270.shl(6)

    arcs[3].x = (monitor.x + monitor.width - 40).toShort()
    arcs[3].y = (monitor.y + monitor.height - 40).toShort()
    arcs[3].angle1 = 270.shl(6)

    val rects = nativeHeap.allocArray<XRectangle>(4)
    // extensions for round pieces
    for (i in 0 until 4) {
        rects[i].width = 12.toUShort()
        rects[i].height = 40.toUShort()
    }
    rects[0].x = (monitor.x + 20).toShort()
    rects[0].y = monitor.y.toShort()

    rects[1].x = (monitor.x + 20).toShort()
    rects[1].y = (monitor.y + monitor.height - 40).toShort()

    rects[2].x = (monitor.x + monitor.width - 32).toShort()
    rects[2].y = monitor.y.toShort()

    rects[3].x = (monitor.x + monitor.width - 32).toShort()
    rects[3].y = (monitor.y + monitor.height - 40).toShort()

    val bars = nativeHeap.allocArray<XRectangle>(2)
    // top bar
    bars[0].x = (monitor.x + 48 + image.pointed.width).toShort()
    bars[0].y = monitor.y.toShort()
    bars[0].width = (monitor.width - 88 - image.pointed.width).toUShort()
    bars[0].height = 40.toUShort()

    // bottom bar
    bars[1].x = (monitor.x + 40).toShort()
    bars[1].y = (monitor.y + monitor.height - 40).toShort()
    bars[1].width = (monitor.width - 80).toUShort()
    bars[1].height = 40.toUShort()

    XFillArcs(display, lcarsWindow, gcPurple2, arcs, 4)
    XFillRectangles(display, lcarsWindow, gcPurple2, rects, 4)
    XFillRectangles(display, lcarsWindow, gcOrchid, bars, 2)

    XPutImage(display, lcarsWindow, gcCopyImage,
        image, 0, 0, monitor.x + 40, 0,
        image.pointed.width.convert(), image.pointed.height.convert())

    nativeHeap.free(arcs)
    nativeHeap.free(rects)

    XFreeGC(display, gcCopyImage)
}

private fun drawNormalFrame(
    graphicsContexts: List<GC>,
    lcarsWindow: ULong,
    display: CPointer<Display>,
    monitor: Monitor,
    image: CPointer<XImage>
) {
    clearScreen(graphicsContexts, lcarsWindow, display, monitor, image)

    val gcBlack = graphicsContexts[0]
    val gcPurple2 = graphicsContexts[6]
    val gcOrchid = graphicsContexts[2]
    val gcPurple1 = graphicsContexts[3]
    val gcBrick = graphicsContexts[4]
    val gcCopyImage = XCreateGC(display, lcarsWindow, 0.convert(), null)

    // TODO create bar ends as pixmaps
    val arcs = nativeHeap.allocArray<XArc>(3)
    for (i in 0 until 3) {
        arcs[i].width = 40.toUShort()
        arcs[i].height = 40.toUShort()
        arcs[i].angle1 = 270.shl(6)
        arcs[i].angle2 = 180.shl(6)
    }
    arcs[0].x = (monitor.x + monitor.width - 40).toShort()
    arcs[0].y = monitor.y.toShort()

    arcs[1].x = (monitor.x + monitor.width - 40).toShort()
    arcs[1].y = (monitor.y + 176).toShort()

    arcs[2].x = (monitor.x + monitor.width - 40).toShort()
    arcs[2].y = (monitor.y + monitor.height - 40).toShort()

    val rects = nativeHeap.allocArray<XRectangle>(3)
    // extensions for round pieces
    for (i in 0 until 3) {
        rects[i].width = 12.toUShort()
        rects[i].height = 40.toUShort()
    }
    rects[0].x = (monitor.x + monitor.width - 32).toShort()
    rects[0].y = monitor.y.toShort()

    rects[1].x = (monitor.x + monitor.width - 32).toShort()
    rects[1].y = (monitor.y + 176).toShort()

    rects[2].x = (monitor.x + monitor.width - 32).toShort()
    rects[2].y = (monitor.y + monitor.height - 40).toShort()

    val bigBars = nativeHeap.allocArray<XRectangle>(2)
    bigBars[0].x = (monitor.x + 270).toShort()
    bigBars[0].y = monitor.y.toShort()
    bigBars[0].width = (monitor.width - 318 - image.pointed.width).toUShort()
    bigBars[0].height = 40.toUShort()

    // bottom bar
    bigBars[1].x = (monitor.x + 320).toShort()
    bigBars[1].y = (monitor.y + monitor.height - 40).toShort()
    bigBars[1].width = (monitor.width - 360).toUShort()
    bigBars[1].height = 40.toUShort()

    val middleBars = nativeHeap.allocArray<XRectangle>(4)
    val middleSegmentWidth = (monitor.width - 280) / 8

    // upper middle bars
    middleBars[0].x = (monitor.x + 240).toShort()
    middleBars[0].y = (monitor.y + 176).toShort()
    middleBars[0].width = (middleSegmentWidth * 6 - 8).toUShort()
    middleBars[0].height = 16.toUShort()

    middleBars[1].x = (monitor.x + 240 + middleSegmentWidth * 6).toShort()
    middleBars[1].y = (monitor.y + 176).toShort()
    middleBars[1].width = (middleSegmentWidth * 2).toUShort()
    middleBars[1].height = 16.toUShort()

    // lower middle bars
    middleBars[2].x = (monitor.x + 240).toShort()
    middleBars[2].y = (monitor.y + 200).toShort()
    middleBars[2].width = (middleSegmentWidth * 3 - 8).toUShort()
    middleBars[2].height = 16.toUShort()

    middleBars[3].x = (monitor.x + 240 + middleSegmentWidth * 3).toShort()
    middleBars[3].y = (monitor.y + 200).toShort()
    middleBars[3].width = (middleSegmentWidth * 5).toUShort()
    middleBars[3].height = 16.toUShort()

    val sideBars = nativeHeap.allocArray<XRectangle>(2)
    sideBars[0].x = monitor.x.toShort()
    sideBars[0].y = (monitor.y + 64).toShort()
    sideBars[0].width = 184.toUShort()
    sideBars[0].height = 88.toUShort()

    sideBars[1].x = monitor.x.toShort()
    sideBars[1].y = (monitor.y + 240).toShort()
    sideBars[1].width = 184.toUShort()
    sideBars[1].height = (monitor.height - 304).toUShort()

    // TODO create corners as pixmaps
    val cornerOuterArcs = nativeHeap.allocArray<XArc>(4)
    for (i in 0 until 4) {
        cornerOuterArcs[i].x = monitor.x.toShort()
        cornerOuterArcs[i].angle2 = 90.shl(6)
    }
    cornerOuterArcs[0].y = (monitor.y).toShort()
    cornerOuterArcs[0].width = 80.toUShort()
    cornerOuterArcs[0].height = 80.toUShort()
    cornerOuterArcs[0].angle1 = 90.shl(6)

    cornerOuterArcs[1].y = (monitor.y + 160).toShort()
    cornerOuterArcs[1].width = 32.toUShort()
    cornerOuterArcs[1].height = 32.toUShort()
    cornerOuterArcs[1].angle1 = 180.shl(6)

    cornerOuterArcs[2].y = (monitor.y + 200).toShort()
    cornerOuterArcs[2].width = 32.toUShort()
    cornerOuterArcs[2].height = 32.toUShort()
    cornerOuterArcs[2].angle1 = 90.shl(6)

    cornerOuterArcs[3].y = (monitor.y + monitor.height - 80).toShort()
    cornerOuterArcs[3].width = 80.toUShort()
    cornerOuterArcs[3].height = 80.toUShort()
    cornerOuterArcs[3].angle1 = 180.shl(6)

    val cornerRects = nativeHeap.allocArray<XRectangle>(8)
    for (i in 0 until 4) {
        cornerRects[i].x = monitor.x.toShort()
        cornerRects[i].width = 199.toUShort()
        cornerRects[i].height = 16.toUShort()
    }
    cornerRects[0].y = (monitor.y + 40).toShort()
    cornerRects[1].y = (monitor.y + 160).toShort()
    cornerRects[2].y = (monitor.y + 216).toShort()
    cornerRects[3].y = (monitor.y + monitor.height - 56).toShort()

    for (i in 4 until 6) {
        cornerRects[i].x = (monitor.x + 40).toShort()
        cornerRects[i].height = 40.toUShort()
    }
    cornerRects[4].y = monitor.y.toShort()
    cornerRects[4].width = 222.toUShort()
    cornerRects[5].y = (monitor.y + monitor.height - 40).toShort()
    cornerRects[5].width = 272.toUShort()

    for (i in 6 until 8) {
        cornerRects[i].x = (monitor.x + 16).toShort()
        cornerRects[i].width = 216.toUShort()
        cornerRects[i].height = 16.toUShort()
    }
    cornerRects[6].y = (monitor.y + 176).toShort()
    cornerRects[7].y = (monitor.y + 200).toShort()

    val cornerInnerArcs = nativeHeap.allocArray<XArc>(4)
    for (i in 0 until 4) {
        cornerInnerArcs[i].x = (monitor.x + 183).toShort()
        cornerInnerArcs[i].width = 32.toUShort()
        cornerInnerArcs[i].height = 32.toUShort()
        cornerInnerArcs[i].angle2 = 90.shl(6)
    }
    cornerInnerArcs[0].y = (monitor.y + 40).toShort()
    cornerInnerArcs[0].angle1 = 90.shl(6)

    cornerInnerArcs[1].y = (monitor.y + 144).toShort()
    cornerInnerArcs[1].angle1 = 180.shl(6)

    cornerInnerArcs[2].y = (monitor.y + 216).toShort()
    cornerInnerArcs[2].angle1 = 90.shl(6)

    cornerInnerArcs[3].y = (monitor.y + monitor.height - 72).toShort()
    cornerInnerArcs[3].angle1 = 180.shl(6)

    XFillArcs(display, lcarsWindow, gcPurple2, arcs, 3)
    XFillRectangles(display, lcarsWindow, gcPurple2, rects, 3)
    XFillRectangles(display, lcarsWindow, gcPurple2, bigBars, 2)

    // middle bars
    XFillRectangles(display, lcarsWindow, gcPurple1, middleBars[0].ptr, 1)
    XFillRectangles(display, lcarsWindow, gcBrick, middleBars[1].ptr, 1)
    XFillRectangles(display, lcarsWindow, gcPurple2, middleBars[2].ptr, 1)
    XFillRectangles(display, lcarsWindow, gcOrchid, middleBars[3].ptr, 1)

    // side bars
    XFillRectangles(display, lcarsWindow, gcPurple1, sideBars, 2)

    // corner pieces
    XFillArcs(display, lcarsWindow, gcOrchid, cornerOuterArcs, 4)
    XFillRectangles(display, lcarsWindow, gcOrchid, cornerRects, 8)
    XFillArcs(display, lcarsWindow, gcBlack, cornerInnerArcs, 4)

    XPutImage(display, lcarsWindow, gcCopyImage,
        image, 0, 0, monitor.x + monitor.width - 40 - image.pointed.width, 0,
        image.pointed.width.convert(), image.pointed.height.convert())

    nativeHeap.free(arcs)
    nativeHeap.free(rects)
    nativeHeap.free(bigBars)
    nativeHeap.free(middleBars)
    nativeHeap.free(sideBars)
    nativeHeap.free(cornerOuterArcs)
    nativeHeap.free(cornerRects)
    nativeHeap.free(cornerInnerArcs)

    XFreeGC(display, gcCopyImage)
}

private fun clearScreen(
    graphicsContexts: List<GC>,
    lcarsWindow: ULong,
    display: CPointer<Display>,
    monitor: Monitor,
    image: CPointer<XImage>
) {
    XFillRectangle(display, lcarsWindow, graphicsContexts[0], monitor.x, monitor.y, monitor.width.convert(), monitor.height.convert())
}
