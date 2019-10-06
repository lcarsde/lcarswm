package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ULongVar
import xlib.*

/**
 *
 */
interface DrawApi {
    fun fillArcs(drawable: Drawable, graphicsContext: GC, arcs: CValuesRef<XArc>, arcCount: Int): Int

    fun fillRectangle(drawable: Drawable, graphicsContext: GC, x: Int, y: Int, width: UInt, height: UInt): Int

    fun fillRectangles(drawable: Drawable, graphicsContext: GC, rects: CValuesRef<XRectangle>, rectCount: Int): Int

    fun putImage(drawable: Drawable, graphicsContext: GC, image: CValuesRef<XImage>,
                  x: Int, y: Int, width: UInt, height: UInt): Int

    fun createGC(drawable: Drawable, mask: ULong, gcValues: CValuesRef<XGCValues>?): GC?

    fun freeGC(graphicsContext: GC): Int

    fun createColormap(window: Window, visual: CValuesRef<Visual>, alloc: Int): Colormap

    fun allocColor(colorMap: Colormap, color: CValuesRef<XColor>): Int

    fun freeColors(colorMap: Colormap, pixels: CValuesRef<ULongVar>, pixelCount: Int): Int

    fun freeColormap(colorMap: Colormap): Int

    fun readXpmFileToImage(imagePath: String, imageBuffer: CValuesRef<CPointerVar<XImage>>): Int
}