package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ULongVar
import xlib.*

/**
 *
 */
interface DrawApi {
    fun fillArcs(display: CValuesRef<Display>, drawable: Drawable, graphicsContext: GC, arcs: CValuesRef<XArc>, arcCount: Int): Int

    fun fillRectangle(display: CValuesRef<Display>, drawable: Drawable, graphicsContext: GC, x: Int, y: Int, width: UInt, height: UInt): Int

    fun fillRectangles(display: CValuesRef<Display>, drawable: Drawable, graphicsContext: GC, rects: CValuesRef<XRectangle>, rectCount: Int): Int

    fun putImage(display: CValuesRef<Display>, drawable: Drawable, graphicsContext: GC?, image: CValuesRef<XImage>,
                  x: Int, y: Int, width: UInt, height: UInt): Int

    fun createGC(display: CValuesRef<Display>, drawable: Drawable, mask: ULong, gcValues: CValuesRef<XGCValues>?): GC?

    fun freeGC(display: CValuesRef<Display>, graphicsContext: GC?): Int

    fun createColormap(display: CValuesRef<Display>, window: Window, visual: CValuesRef<Visual>?, alloc: Int): Colormap

    fun allocColor(display: CValuesRef<Display>, colorMap: Colormap, color: CValuesRef<XColor>): Int

    fun freeColors(display: CValuesRef<Display>, colorMap: Colormap, pixels: CValuesRef<ULongVar>, pixelCount: Int): Int

    fun freeColormap(display: CValuesRef<Display>, colorMap: Colormap): Int

    fun readXpmFileToImage(display: CValuesRef<Display>, imagePath: String, imageBuffer: CValuesRef<CPointerVar<XImage>>): Int
}