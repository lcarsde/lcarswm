package de.atennert.lcarswm.system

import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.X_TRUE
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

/**
 * This is the facade for accessing system functions.
 */
class SystemFacade : SystemApi {
    override fun selectInput(display: CValuesRef<Display>, window: Window, mask: Long): Int {
        return XSelectInput(display, window, mask)
    }

    override fun setInputFocus(display: CValuesRef<Display>, window: Window, revertTo: Int, time: Time): Int {
        return XSetInputFocus(display, window, revertTo, time)
    }

    override fun grabKey(
        display: CValuesRef<Display>,
        keyCode: Int,
        modifiers: UInt,
        window: Window,
        ownerEvents: Boolean,
        pointerMode: Int,
        keyboardMode: Int
    ): Int {
        return XGrabKey(display, keyCode, modifiers, window, convertOwnerEvents(ownerEvents), pointerMode, keyboardMode)
    }

    override fun grabButton(
        display: CValuesRef<Display>,
        button: UInt,
        modifiers: UInt,
        window: Window,
        ownerEvents: Boolean,
        mask: UInt,
        pointerMode: Int,
        keyboardMode: Int,
        windowToConfineTo: Window,
        cursor: Cursor
    ): Int {
        return XGrabButton(display, button, modifiers, window, convertOwnerEvents(ownerEvents), mask, pointerMode, keyboardMode, windowToConfineTo, cursor)
    }

    override fun getModifierMapping(display: CValuesRef<Display>): CPointer<XModifierKeymap>? {
        return XGetModifierMapping(display)
    }

    override fun keysymToKeycode(display: CValuesRef<Display>, keySym: KeySym): KeyCode {
        return XKeysymToKeycode(display, keySym)
    }

    override fun rQueryExtension(
        display: CValuesRef<Display>,
        eventBase: CValuesRef<IntVar>,
        errorBase: CValuesRef<IntVar>
    ): Int {
        return XRRQueryExtension(display, eventBase, errorBase)
    }

    override fun rSelectInput(display: CValuesRef<Display>, window: Window, mask: Int) {
        XRRSelectInput(display, window, mask)
    }

    override fun rGetScreenResources(display: CValuesRef<Display>, window: Window): CPointer<XRRScreenResources>? {
        return XRRGetScreenResources(display, window)
    }

    override fun rGetOutputPrimary(display: CValuesRef<Display>, window: Window): RROutput {
        return XRRGetOutputPrimary(display, window)
    }

    override fun rGetOutputInfo(
        display: CValuesRef<Display>,
        resources: CPointer<XRRScreenResources>,
        output: RROutput
    ): CPointer<XRROutputInfo>? {
        return XRRGetOutputInfo(display, resources, output)
    }

    override fun rGetCrtcInfo(
        display: CValuesRef<Display>,
        resources: CPointer<XRRScreenResources>,
        crtc: RRCrtc
    ): CPointer<XRRCrtcInfo>? {
        return XRRGetCrtcInfo(display, resources, crtc)
    }

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

    override fun readXpmFileToImage(
        display: CValuesRef<Display>,
        imagePath: String,
        imageBuffer: CValuesRef<CPointerVar<XImage>>
    ): Int {
        return XpmReadFileToImage(display, imagePath, imageBuffer, null, null)
    }

    private fun convertOwnerEvents(ownerEvents: Boolean): Int = when (ownerEvents) {
        true  -> X_TRUE
        false -> X_FALSE
    }
}