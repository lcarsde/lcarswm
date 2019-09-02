package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.SystemDrawApi
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.convert
import xlib.*

/**
 * This is the facade for accessing system functions.
 */
class SystemFacade : SystemDrawApi {
    override fun fillArcs(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        arcs: CValuesRef<XArc>,
        arcCount: Int
    ): Int {
        return XFillArcs(display, drawable, graphicsContext, arcs, arcCount)
    }

    override fun fillRectangle(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int {
        return XFillRectangle(display, drawable, graphicsContext, x, y, width, height)
    }

    override fun fillRectangles(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        rects: CValuesRef<XRectangle>,
        rectCount: Int
    ): Int {
        return XFillRectangles(display, drawable, graphicsContext, rects, rectCount)
    }

    override fun putImage(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC?,
        image: CValuesRef<XImage>,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int {
        return XPutImage(display, drawable, graphicsContext, image, 0, 0, x, y, width, height)
    }

    override fun createGC(
        display: CValuesRef<Display>,
        drawable: Drawable,
        mask: ULong,
        gcValues: CValuesRef<XGCValues>?
    ): GC? {
        return XCreateGC(display, drawable, mask, gcValues)
    }

    override fun freeGC(display: CValuesRef<Display>, graphicsContext: GC?): Int {
        return XFreeGC(display, graphicsContext)
    }

    override fun createColormap(
        display: CValuesRef<Display>,
        window: Window,
        visual: CValuesRef<Visual>?,
        alloc: Int
    ): Colormap {
        return XCreateColormap(display, window, visual, alloc)
    }

    override fun allocColor(display: CValuesRef<Display>, colorMap: Colormap, color: CValuesRef<XColor>): Int {
        return XAllocColor(display, colorMap, color)
    }

    override fun freeColors(
        display: CValuesRef<Display>,
        colorMap: Colormap,
        pixels: CValuesRef<ULongVar>,
        pixelCount: Int
    ): Int {
        return XFreeColors(display, colorMap, pixels, pixelCount, 0.convert())
    }

    override fun freeColormap(display: CValuesRef<Display>, colorMap: Colormap): Int {
        return XFreeColormap(display, colorMap)
    }
}