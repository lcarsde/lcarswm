package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.*
import xcb.*

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
    xcbConnection: CPointer<xcb_connection_t>,
    rootVisual: UInt,
    windowId: UInt
): Pair<UInt, List<CPointer<xcb_alloc_color_reply_t>>> {

    val colorMapId = xcb_generate_id(xcbConnection)

    xcb_create_colormap(xcbConnection, XCB_COLORMAP_ALLOC_NONE.convert(), colorMapId, windowId, rootVisual)

    val colorReplies = COLORS
        .asSequence()
        .map { (red, green, blue) ->
            xcb_alloc_color(xcbConnection, colorMapId, red.convert(), green.convert(), blue.convert())
        }
        .map { colorCookie -> xcb_alloc_color_reply(xcbConnection, colorCookie, null) }
        .filterNotNull()
        .toList()

    return Pair(colorMapId, colorReplies)
}

fun getGraphicContexts(
    xcbConnection: CPointer<xcb_connection_t>,
    rootId: UInt,
    colors: List<CPointer<xcb_alloc_color_reply_t>>
): List<UInt> = colors
    .map { colorReply ->
        val gcId = xcb_generate_id(xcbConnection)
        val parameterArray = arrayOf(colorReply.pointed.pixel, 0.convert(), XCB_ARC_MODE_PIE_SLICE.convert())
        val parameters = UIntArray(parameterArray.size) { parameterArray[it] }
        xcb_create_gc(
            xcbConnection,
            gcId,
            rootId,
            XCB_GC_FOREGROUND or XCB_GC_GRAPHICS_EXPOSURES or XCB_GC_ARC_MODE,
            parameters.toCValues()
        )
        gcId
    }

fun cleanupColorMap(
    xcbConnection: CPointer<xcb_connection_t>,
    colorMap: Pair<UInt, List<CPointer<xcb_alloc_color_reply_t>>>
) {
    xcb_free_colormap(xcbConnection, colorMap.first)

    colorMap.second.forEach { colorReply -> nativeHeap.free(colorReply) }
}

private fun drawMaximizedFrame(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    display: CPointer<Display>,
    image: CPointer<XImage>
) {
    clearScreen(xcbConnection, windowManagerState, display, image)

    val gcPurple2 = windowManagerState.graphicsContexts[6]
    val gcOrchid = windowManagerState.graphicsContexts[2]
    val gcCopyImage = XCreateGC(display, windowManagerState.lcarsWindowId.convert(), 0.convert(), null)


    windowManagerState.monitors.forEach { monitor ->
        // TODO create bar ends as pixmaps
        val arcs = nativeHeap.allocArray<xcb_arc_t>(4)
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

        val rects = nativeHeap.allocArray<xcb_rectangle_t>(4)
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

        val bars = nativeHeap.allocArray<xcb_rectangle_t>(2)
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

        xcb_poly_fill_arc(xcbConnection, windowManagerState.lcarsWindowId, gcPurple2, 4.convert(), arcs)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcPurple2, 4.convert(), rects)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcOrchid, 2.convert(), bars)

        XPutImage(display, windowManagerState.lcarsWindowId.convert(), gcCopyImage, image, 0, 0, monitor.x + 40, 0, image.pointed.width.convert(), image.pointed.height.convert())

        nativeHeap.free(arcs)
        nativeHeap.free(rects)
    }

    XFreeGC(display, gcCopyImage)
}

private fun drawNormalFrame(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    display: CPointer<Display>,
    image: CPointer<XImage>
) {
    clearScreen(xcbConnection, windowManagerState, display, image)

    val gcBlack = windowManagerState.graphicsContexts[0]
    val gcPurple2 = windowManagerState.graphicsContexts[6]
    val gcOrchid = windowManagerState.graphicsContexts[2]
    val gcPurple1 = windowManagerState.graphicsContexts[3]
    val gcBrick = windowManagerState.graphicsContexts[4]
    val gcCopyImage = XCreateGC(display, windowManagerState.lcarsWindowId.convert(), 0.convert(), null)

    windowManagerState.monitors.forEach { monitor ->
        // TODO create bar ends as pixmaps
        val arcs = nativeHeap.allocArray<xcb_arc_t>(3)
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

        val rects = nativeHeap.allocArray<xcb_rectangle_t>(3)
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

        val bigBars = nativeHeap.allocArray<xcb_rectangle_t>(2)
        bigBars[0].x = (monitor.x + 270).toShort()
        bigBars[0].y = monitor.y.toShort()
        bigBars[0].width = (monitor.width - 318 - image.pointed.width).toUShort()
        bigBars[0].height = 40.toUShort()

        // bottom bar
        bigBars[1].x = (monitor.x + 320).toShort()
        bigBars[1].y = (monitor.y + monitor.height - 40).toShort()
        bigBars[1].width = (monitor.width - 360).toUShort()
        bigBars[1].height = 40.toUShort()

        val middleBars = nativeHeap.allocArray<xcb_rectangle_t>(4)
        val middleSegmentWidth = (monitor.width - 280) / 5

        // upper middle bars
        middleBars[0].x = (monitor.x + 240).toShort()
        middleBars[0].y = (monitor.y + 176).toShort()
        middleBars[0].width = (middleSegmentWidth * 4 - 8).toUShort()
        middleBars[0].height = 16.toUShort()

        middleBars[1].x = (monitor.x + 240 + middleSegmentWidth * 4).toShort()
        middleBars[1].y = (monitor.y + 176).toShort()
        middleBars[1].width = (middleSegmentWidth).toUShort()
        middleBars[1].height = 16.toUShort()

        // lower middle bars
        middleBars[2].x = (monitor.x + 240).toShort()
        middleBars[2].y = (monitor.y + 200).toShort()
        middleBars[2].width = (middleSegmentWidth * 2).toUShort()
        middleBars[2].height = 16.toUShort()

        middleBars[3].x = (monitor.x + 248 + middleSegmentWidth * 2).toShort()
        middleBars[3].y = (monitor.y + 200).toShort()
        middleBars[3].width = (middleSegmentWidth * 3 - 8).toUShort()
        middleBars[3].height = 16.toUShort()

        val sideBars = nativeHeap.allocArray<xcb_rectangle_t>(2)
        sideBars[0].x = monitor.x.toShort()
        sideBars[0].y = (monitor.y + 64).toShort()
        sideBars[0].width = 184.toUShort()
        sideBars[0].height = 88.toUShort()

        sideBars[1].x = monitor.x.toShort()
        sideBars[1].y = (monitor.y + 240).toShort()
        sideBars[1].width = 184.toUShort()
        sideBars[1].height = (monitor.height - 304).toUShort()

        // TODO create corners as pixmaps
        val cornerOuterArcs = nativeHeap.allocArray<xcb_arc_t>(4)
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

        val cornerRects = nativeHeap.allocArray<xcb_rectangle_t>(8)
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

        val cornerInnerArcs = nativeHeap.allocArray<xcb_arc_t>(4)
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

        xcb_poly_fill_arc(xcbConnection, windowManagerState.lcarsWindowId, gcPurple2, 3.convert(), arcs)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcPurple2, 3.convert(), rects)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcPurple2, 2.convert(), bigBars)

        // middle bars
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcPurple1, 1.convert(), middleBars[0].ptr)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcBrick, 1.convert(), middleBars[1].ptr)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcPurple2, 1.convert(), middleBars[2].ptr)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcOrchid, 1.convert(), middleBars[3].ptr)

        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcPurple1, 2.convert(), sideBars)

        // corner pieces
        xcb_poly_fill_arc(xcbConnection, windowManagerState.lcarsWindowId, gcOrchid, 4.convert(), cornerOuterArcs)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, gcOrchid, 8.convert(), cornerRects)
        xcb_poly_fill_arc(xcbConnection, windowManagerState.lcarsWindowId, gcBlack, 4.convert(), cornerInnerArcs)

        XPutImage(display, windowManagerState.lcarsWindowId.convert(), gcCopyImage, image, 0, 0, monitor.x + monitor.width - 40 - image.pointed.width, 0, image.pointed.width.convert(), image.pointed.height.convert())

        nativeHeap.free(arcs)
        nativeHeap.free(rects)
        nativeHeap.free(bigBars)
        nativeHeap.free(middleBars)
        nativeHeap.free(sideBars)
        nativeHeap.free(cornerOuterArcs)
        nativeHeap.free(cornerRects)
        nativeHeap.free(cornerInnerArcs)
    }

    XFreeGC(display, gcCopyImage)
}

private fun clearScreen(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    display: CPointer<Display>,
    image: CPointer<XImage>
) {
    val rect = nativeHeap.alloc<xcb_rectangle_t>()

    rect.x = 0
    rect.y = 0
    rect.width = windowManagerState.screenSize.first.convert()
    rect.height = windowManagerState.screenSize.second.convert()

    val graphicsContext = windowManagerState.graphicsContexts[0]

    xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, graphicsContext, 1.convert(), rect.ptr)

    nativeHeap.free(rect)
}
