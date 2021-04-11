package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.lifecycle.closeWith
import de.atennert.lcarswm.system.api.DrawApi
import kotlinx.cinterop.*
import xlib.*

class ColorFactory(private val drawApi: DrawApi, screen: Screen) {
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

    val colorMapId = drawApi.createColormap(screen.root, screen.root_visual!!, AllocNone)

    val colorPixels: List<ULong> by lazy(this::allocateCompleteColorMap)

    private val knownColors = mutableMapOf<Color, Pair<GC, ULong>>()

    init {
        closeWith(ColorFactory::cleanupColorMap)
    }

    private fun allocateCompleteColorMap(): List<ULong> {
        return colors
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
    }

    private fun cleanupColorMap() {
        drawApi.freeColors(colorMapId, colorPixels.toULongArray().toCValues(), colorPixels.size)
        drawApi.freeColormap(colorMapId)
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
        color.pixel = colorPixels[colorIndex]
        return color
    }

    fun createColorGC(drawable: Drawable, color: Color): GC? {
        knownColors[color]?.let {
            return it.first
        }

        val xColor = color.toXColor()
        drawApi.allocColor(colorMapId, xColor.ptr)

        val gcValues = createGcValues(xColor)
        val mask = GCForeground or GCGraphicsExposures or GCArcMode
        val gc = drawApi.createGC(drawable, mask.convert(), gcValues.ptr)!!

        knownColors[color] = Pair(gc, xColor.pixel)

        nativeHeap.free(gcValues)
        nativeHeap.free(xColor)

        return gc
    }

    private fun createGcValues(xColor: XColor): XGCValues {
        val gcValues = nativeHeap.alloc<XGCValues>()
        gcValues.foreground = xColor.pixel
        gcValues.graphics_exposures = 0
        gcValues.arc_mode = ArcPieSlice
        return gcValues
    }
}