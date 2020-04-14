package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ULongVar
import xlib.*

/**
 * API interface for X drawing functions
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

    fun allocColor(colorMap: Colormap, color: CPointer<XColor>): Int

    fun freeColors(colorMap: Colormap, pixels: CValuesRef<ULongVar>, pixelCount: Int): Int

    fun freeColormap(colorMap: Colormap): Int

    fun readXpmFileToImage(imagePath: String, imageBuffer: CPointer<CPointerVar<XImage>>): Int

    fun createPixmap(drawable: Drawable, width: UInt, height: UInt, depth: UInt): Pixmap

    fun setWindowBackgroundPixmap(window: Window, pixmap: Pixmap)

    fun clearWindow(window: Window)

    fun freePixmap(pixmap: Pixmap)

    fun xftDrawCreate(drawable: Drawable, visual: CValuesRef<Visual>, colorMap: Colormap): CPointer<XftDraw>?
}