package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.*
import xcb.*

/**
 *
 */
val colors = listOf(
    Triple(0x99, 0x99, 0xff)
)

fun allocateColorMap(
    xcbConnection: CPointer<xcb_connection_t>,
    rootVisual: UInt,
    windowId: UInt
): Pair<UInt, List<CPointer<xcb_alloc_color_reply_t>>> {

    val colorMapId = xcb_generate_id(xcbConnection)

    xcb_create_colormap(xcbConnection, XCB_COLORMAP_ALLOC_NONE.convert(), colorMapId, windowId, rootVisual)

    val colorReplies = colors
        .map { (red, green, blue) ->
            xcb_alloc_color(xcbConnection, colorMapId, red.convert(), green.convert(), blue.convert())
        }
        .map { colorCookie -> xcb_alloc_color_reply(xcbConnection, colorCookie, null)!! }

    return Pair(colorMapId, colorReplies)
}

fun cleanupColorMap(
    xcbConnection: CPointer<xcb_connection_t>,
    colorMap: Pair<UInt, List<CPointer<xcb_alloc_color_reply_t>>>
) {
    xcb_free_colormap(xcbConnection, colorMap.first)

    colorMap.second.forEach { colorReply -> nativeHeap.free(colorReply) }
}