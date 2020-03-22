package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.system.api.DrawApi
import kotlinx.cinterop.*
import xlib.*

class Colors(private val drawApi: DrawApi) {
    private val colors = listOf(
        Triple(0, 0, 0), // black
        Triple(0xFFFF, 0x9999, 0), // yellow
        Triple(0xCCCC, 0x9999, 0xCCCC), // orchid
        Triple(0x9999, 0x9999, 0xCCCC), // dampened purple
        Triple(0xCCCC, 0x6666, 0x6666), // dark red
        Triple(0xFFFF, 0xCCCC, 0x9999), // sand
        Triple(0x9999, 0x9999, 0xFFFF), // bright purple
        Triple(0xFFFF, 0x9999, 0x6666), // orange
        Triple(0xCCCC, 0x6666, 0x9999)  // dark pink
    )

    fun allocateCompleteColorMap(
        visual: CPointer<Visual>,
        windowId: Window
    ): Pair<Colormap, List<ULong>> {
        val colorMapId = drawApi.createColormap(windowId, visual, AllocNone)

        val colorReplies = colors
            .asSequence()
            .map { (red, green, blue) ->
                val color = nativeHeap.alloc<XColor>()
                color.red = red.convert()
                color.green = green.convert()
                color.blue = blue.convert()
                drawApi.allocColor(colorMapId, color.ptr)
                color.pixel
            }
            .filterNotNull()
            .toList()

        return Pair(colorMapId, colorReplies)
    }

    fun loadForegroundGraphicContexts(
        window: Window,
        colors: List<ULong>
    ): List<GC> = colors
        .map { color ->
            val gcValues = nativeHeap.alloc<XGCValues>()
            gcValues.foreground = color
            gcValues.graphics_exposures = 0
            gcValues.arc_mode = ArcPieSlice

            val mask = GCForeground or GCGraphicsExposures or GCArcMode
            drawApi.createGC(window, mask.convert(), gcValues.ptr)!!
        }
}