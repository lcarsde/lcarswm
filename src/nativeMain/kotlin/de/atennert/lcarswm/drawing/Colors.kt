package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.closeWith
import de.atennert.lcarswm.system.api.DrawApi
import kotlinx.cinterop.*
import xlib.*

class Colors(private val drawApi: DrawApi, private val screen: Screen) {
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

    val colorMap = allocateCompleteColorMap()

    init {
        closeWith(Colors::cleanupColorMap)
    }

    private fun allocateCompleteColorMap(): Pair<Colormap, List<ULong>> {
        val colorMapId = drawApi.createColormap(screen.root, screen.root_visual!!, AllocNone)

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

    private fun cleanupColorMap() {
        val colorPixels = ULongArray(colorMap.second.size) { colorMap.second[it] }
        drawApi.freeColors(colorMap.first, colorPixels.toCValues(), colorPixels.size)
        drawApi.freeColormap(colorMap.first)
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

    fun getXftColor(colorIndex: Int): XftColor {
        val color = nativeHeap.alloc<XftColor>()
        val colorCode = colors[colorIndex]
        color.color.red = colorCode.first.convert()
        color.color.green = colorCode.second.convert()
        color.color.blue = colorCode.third.convert()
        color.color.alpha = 0xffff.convert()
        color.pixel = colorMap.second[colorIndex]
        return color
    }
}