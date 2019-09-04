package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

class SystemFacadeMock : SystemApi {
    override fun rQueryExtension(
        display: CValuesRef<Display>,
        eventBase: CValuesRef<IntVar>,
        errorBase: CValuesRef<IntVar>
    ): Int = 0

    override fun rSelectInput(display: CValuesRef<Display>, window: Window, mask: Int) {}

    override fun rGetScreenResources(display: CValuesRef<Display>, window: Window): CPointer<XRRScreenResources>? = null

    override fun rGetOutputPrimary(display: CValuesRef<Display>, window: Window): RROutput = 0.convert()

    override fun rGetOutputInfo(
        display: CValuesRef<Display>,
        resources: CPointer<XRRScreenResources>,
        output: RROutput
    ): CPointer<XRROutputInfo>? = null

    override fun rGetCrtcInfo(
        display: CValuesRef<Display>,
        resources: CPointer<XRRScreenResources>,
        crtc: RRCrtc
    ): CPointer<XRRCrtcInfo>? = null

    override fun fillArcs(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        arcs: CValuesRef<XArc>,
        arcCount: Int
    ): Int = 0

    override fun fillRectangle(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun fillRectangles(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        rects: CValuesRef<XRectangle>,
        rectCount: Int
    ): Int = 0

    override fun putImage(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC?,
        image: CValuesRef<XImage>,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun createGC(
        display: CValuesRef<Display>,
        drawable: Drawable,
        mask: ULong,
        gcValues: CValuesRef<XGCValues>?
    ): GC? = null

    override fun freeGC(display: CValuesRef<Display>, graphicsContext: GC?): Int = 0

    override fun createColormap(
        display: CValuesRef<Display>,
        window: Window,
        visual: CValuesRef<Visual>?,
        alloc: Int
    ): Colormap = 0.convert()

    override fun allocColor(display: CValuesRef<Display>, colorMap: Colormap, color: CValuesRef<XColor>): Int = 0

    override fun freeColors(
        display: CValuesRef<Display>,
        colorMap: Colormap,
        pixels: CValuesRef<ULongVar>,
        pixelCount: Int
    ): Int = 0

    override fun freeColormap(display: CValuesRef<Display>, colorMap: Colormap): Int = 0
}