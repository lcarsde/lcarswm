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

    private val knownColors = mutableMapOf<Color, ULong>()
    private val knownGCs = mutableMapOf<Pair<Color, Drawable>, GC>()
    private val knownXftColors = mutableMapOf<Color, XftColor>()

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
        val pixels = knownColors.values.toULongArray().toCValues()
        drawApi.freeColors(colorMapId, pixels, pixels.size)
        knownColors.clear()

        knownGCs.values.forEach { drawApi.freeGC(it) }
        knownGCs.clear()

        knownXftColors.values.forEach(nativeHeap::free)
        knownXftColors.clear()

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

        val pixel = knownColors[color]
            ?: let {
                val xColor = color.toXColor()
                drawApi.allocColor(colorMapId, xColor.ptr)
                val px = xColor.pixel
                nativeHeap.free(xColor)
                knownColors[color] = px
                px
            }

        return knownGCs[Pair(color, drawable)]
            ?: let {
                val gcValues = createGcValues(pixel)
                val mask = GCForeground or GCGraphicsExposures or GCArcMode
                val gc = drawApi.createGC(drawable, mask.convert(), gcValues.ptr)!!
                nativeHeap.free(gcValues)
                knownGCs[Pair(color, drawable)] = gc
                gc
            }
    }

    private fun createGcValues(pixel: ULong): XGCValues {
        val gcValues = nativeHeap.alloc<XGCValues>()
        gcValues.foreground = pixel
        gcValues.graphics_exposures = 0
        gcValues.arc_mode = ArcPieSlice
        return gcValues
    }

    fun createXftColor(color: Color): XftColor {
        val pixel = knownColors[color]
            ?: let {
                val xColor = color.toXColor()
                drawApi.allocColor(colorMapId, xColor.ptr)
                val px = xColor.pixel
                nativeHeap.free(xColor)
                knownColors[color] = px
                px
            }

        return knownXftColors[color]
            ?: let {
                val xftColor = nativeHeap.alloc<XftColor>()
                xftColor.color.red = color.red.convert()
                xftColor.color.green = color.green.convert()
                xftColor.color.blue = color.blue.convert()
                xftColor.color.alpha = 0xffff.convert()
                xftColor.pixel = pixel
                knownXftColors[color] = xftColor

                xftColor
            }
    }
}