@file:Suppress("UNUSED_PARAMETER")

package de.atennert.lcarswm.system

import kotlinx.cinterop.*
import platform.linux.mq_attr
import platform.linux.mqd_t
import platform.posix.size_t
import xlib.*

fun mockXGrabServer(display: CValuesRef<Display>?) = 0

fun mockXUngrabServer(display: CValuesRef<Display>?) = 0

fun mockXGetWindowAttributes(
    display: CValuesRef<Display>?,
    window: Window,
    attributes: CValuesRef<XWindowAttributes>?
) = 0

fun mockXChangeWindowAttributes(
    display: CValuesRef<Display>?,
    window: Window,
    mask: ULong,
    attributes: CValuesRef<XSetWindowAttributes>?
) = 0

fun mockXGetTextProperty(
    display: CValuesRef<Display>?,
    window: Window,
    textProperty: CValuesRef<XTextProperty>?,
    atom: Atom
) = 0

fun mockXGetWindowProperty(
    display: CValuesRef<Display>?,
    window: Window,
    property: Atom,
    offset: Long,
    length: Long,
    delete: Int,
    type: Atom,
    returnType: CValuesRef<ULongVar>?,
    returnFormat: CValuesRef<IntVar>?,
    returnItemCount: CValuesRef<ULongVar>?,
    returnBytesAfter: CValuesRef<ULongVar>?,
    returnProperty: CValuesRef<CPointerVar<UByteVar>>?,
) = 0

fun mockXChangeProperty(
    display: CValuesRef<Display>?,
    window: Window,
    property: Atom,
    type: Atom,
    format: Int,
    mode: Int,
    data: CValuesRef<UByteVar>?,
    dataCount: Int,
) = 0

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

fun mockXDestroyWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXReparentWindow(display: CValuesRef<Display>?, window: Window, newParent: Window, x: Int, y: Int): Int = 0

fun mockXRestackWindows(display: CValuesRef<Display>?, windows: CValuesRef<WindowVar>?, windowCount: Int): Int = 0

fun mockXMapWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXUnmapWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXClearWindow(display: CValuesRef<Display>?, window: Window): Int = 0

fun mockXMoveWindow(display: CValuesRef<Display>?, window: Window, x: Int, y: Int): Int = 0

fun mockXResizeWindow(display: CValuesRef<Display>?, window: Window, width: UInt, height: UInt) = 0

fun mockXMoveResizeWindow(display: CValuesRef<Display>?, window: Window, x: Int, y: Int, width: UInt, height: UInt) = 0

fun mockXSetWindowBorderWidth(display: CValuesRef<Display>?, window: Window, borderWidth: UInt) = 0

fun mockXFlush(display: CValuesRef<Display>?) = 0

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

fun mockXSendEvent(
    display: CValuesRef<Display>?,
    window: Window,
    propagate: Int,
    eventMask: Long,
    event: CValuesRef<XEvent>?,
) = 0

fun mockXConfigureWindow(
    display: CValuesRef<Display>?,
    window: Window,
    valueMask: UInt,
    changes: CValuesRef<XWindowChanges>?,
) = 0

fun mockXSelectInput(
    display: CValuesRef<Display>?,
    window: Window,
    eventMask: Long,
) = 0

fun mockXAddToSaveSet(display: CValuesRef<Display>?, window: Window) = 0

fun mockXRemoveFromSaveSet(display: CValuesRef<Display>?, window: Window) = 0

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

fun mockMqOpen(name: String?, oflag: Int, permissions: mode_t, attributes: mq_attr): mqd_t = 1

fun mockMqClose(mqd: mqd_t) = 0

fun mockMqSend(mqd: mqd_t, message: String?, messageSize: size_t, prio: UInt) = 0

fun mockMqReceive(mqd: mqd_t, message: CValuesRef<ByteVar>?, length: size_t, prio: CValuesRef<UIntVar>?): ssize_t = 0

fun mockMqUnlink(name: String?) = 0
