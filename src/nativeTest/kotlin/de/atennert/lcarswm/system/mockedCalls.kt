package de.atennert.lcarswm.system

import kotlinx.cinterop.*
import xlib.*

fun mockXCreateSimpleWindow(
    display: CValuesRef<Display>?,
    parent: Window,
    c: Int,
    d: Int,
    e: UInt,
    f: UInt,
    g: UInt,
    h: ULong,
    i: ULong
) = 1.toULong()

fun mockXMapWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXUnmapWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXDestroyWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXClearWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXMoveWindow(display: CValuesRef<Display>?, window: Window, x: Int, y: Int): Int = 0

fun mockXGetWindowProperty(
    display: CValuesRef<Display>?,
    window: Window,
    atom: Atom,
    longOffset: Long,
    longLength: Long,
    delete: Int,
    type: Atom,
    actualReturnType: CValuesRef<AtomVar>?,
    actualReturnFormat: CValuesRef<IntVar>?,
    returnItemCount: CValuesRef<ULongVar>?,
    bytesAfterReturn: CValuesRef<ULongVar>?,
    returnProperty: CValuesRef<CPointerVar<UByteVar>>?
): Int {
    return BadAccess
}

fun mockXCreatePixmap(
    display: CValuesRef<Display>?,
    drawable: Drawable,
    width: UInt,
    height: UInt,
    depth: UInt
): Pixmap = 0.toULong()

fun mockXFreePixmap(display: CValuesRef<Display>?, pixmap: Pixmap): Int = 0

fun mockXFree(arg: CValuesRef<*>?): Int = 0

fun mockXSetWindowBackgroundPixmap(display: CValuesRef<Display>?, window: Window, pixmap: Pixmap): Int = 0

fun mockXGetTransientForHint(
    display: CValuesRef<Display>?,
    window: Window,
    propWindowReturn: CValuesRef<WindowVar>?
): Int = 0

fun mockXftDrawCreate(
    display: CValuesRef<Display>?,
    drawable: Drawable,
    visual: CValuesRef<Visual>?,
    colormap: Colormap
): CPointer<XftDraw>? {
    return nativeHeap.allocPointerTo<XftDraw>().value
}

fun mockXftDrawDestroy(xftDraw: CValuesRef<XftDraw>?) {
    if (xftDraw != null) {
        nativeHeap.free(xftDraw.objcPtr())
    }
}

fun mockXftDrawRect(
    xftDraw: CValuesRef<XftDraw>?,
    color: CValuesRef<XftColor>?,
    x: Int,
    y: Int,
    width: UInt,
    height: UInt
) {
}

fun mockPangoLayoutSetText(layout: CValuesRef<PangoLayout>?, text: String?, length: Int) {}

fun mockPangoLayoutSetWidth(layout: CValuesRef<PangoLayout>?, width: Int) {}

fun mockPangoLayoutGetPixelExtents(
    layout: CValuesRef<PangoLayout>?,
    inkRect: CValuesRef<PangoRectangle>?,
    logicalRect: CValuesRef<PangoRectangle>?
) {
}

fun mockPangoLayoutGetLineReadonly(layout: CValuesRef<PangoLayout>?, line: Int): CPointer<PangoLayoutLine>? = null

fun mockPangoXftRenderLayoutLine(
    xftDraw: CValuesRef<XftDraw>?,
    color: CValuesRef<XftColor>?,
    layoutLine: CValuesRef<PangoLayoutLine>?,
    x: Int,
    y: Int
) {
}
