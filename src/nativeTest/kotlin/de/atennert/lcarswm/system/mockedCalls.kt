@file:Suppress("UNUSED_PARAMETER")

package de.atennert.lcarswm.system

import kotlinx.cinterop.*
import platform.linux.mq_attr
import platform.linux.mqd_t
import platform.posix.size_t
import xlib.*

@ExperimentalForeignApi
fun mockXGrabServer(display: CValuesRef<Display>?) = 0

@ExperimentalForeignApi
fun mockXUngrabServer(display: CValuesRef<Display>?) = 0

@ExperimentalForeignApi
fun mockXGetWindowAttributes(
    display: CValuesRef<Display>?,
    window: Window,
    attributes: CValuesRef<XWindowAttributes>?
) = 0

@ExperimentalForeignApi
fun mockXChangeWindowAttributes(
    display: CValuesRef<Display>?,
    window: Window,
    mask: ULong,
    attributes: CValuesRef<XSetWindowAttributes>?
) = 0

@ExperimentalForeignApi
fun mockXGetTextProperty(
    display: CValuesRef<Display>?,
    window: Window,
    textProperty: CValuesRef<XTextProperty>?,
    atom: Atom
) = 0

@ExperimentalForeignApi
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

@ExperimentalForeignApi
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

@ExperimentalForeignApi
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

@ExperimentalForeignApi
fun mockXDestroyWindow(display: CValuesRef<Display>?, window: Window): Int = 0

@ExperimentalForeignApi
fun mockXReparentWindow(display: CValuesRef<Display>?, window: Window, newParent: Window, x: Int, y: Int): Int = 0

@ExperimentalForeignApi
fun mockXRestackWindows(display: CValuesRef<Display>?, windows: CValuesRef<WindowVar>?, windowCount: Int): Int = 0

@ExperimentalForeignApi
fun mockXMapWindow(display: CValuesRef<Display>?, window: Window): Int = 0

@ExperimentalForeignApi
fun mockXUnmapWindow(display: CValuesRef<Display>?, window: Window): Int = 0

@ExperimentalForeignApi
fun mockXClearWindow(display: CValuesRef<Display>?, window: Window): Int = 0

@ExperimentalForeignApi
fun mockXMoveWindow(display: CValuesRef<Display>?, window: Window, x: Int, y: Int): Int = 0

@ExperimentalForeignApi
fun mockXResizeWindow(display: CValuesRef<Display>?, window: Window, width: UInt, height: UInt) = 0

@ExperimentalForeignApi
fun mockXMoveResizeWindow(display: CValuesRef<Display>?, window: Window, x: Int, y: Int, width: UInt, height: UInt) = 0

@ExperimentalForeignApi
fun mockXSetWindowBorderWidth(display: CValuesRef<Display>?, window: Window, borderWidth: UInt) = 0

@ExperimentalForeignApi
fun mockXFlush(display: CValuesRef<Display>?) = 0

@ExperimentalForeignApi
fun mockXCreatePixmap(
    display: CValuesRef<Display>?,
    drawable: Drawable,
    width: UInt,
    height: UInt,
    depth: UInt
): Pixmap = 0.toULong()

@ExperimentalForeignApi
fun mockXFreePixmap(display: CValuesRef<Display>?, pixmap: Pixmap): Int = 0

@ExperimentalForeignApi
fun mockXFree(arg: CValuesRef<*>?): Int = 0

@ExperimentalForeignApi
fun mockXSetWindowBackgroundPixmap(display: CValuesRef<Display>?, window: Window, pixmap: Pixmap): Int = 0

@ExperimentalForeignApi
fun mockXGetTransientForHint(
    display: CValuesRef<Display>?,
    window: Window,
    propWindowReturn: CValuesRef<WindowVar>?
): Int = 0

@ExperimentalForeignApi
fun mockXSendEvent(
    display: CValuesRef<Display>?,
    window: Window,
    propagate: Int,
    eventMask: Long,
    event: CValuesRef<XEvent>?,
) = 0

@ExperimentalForeignApi
fun mockXConfigureWindow(
    display: CValuesRef<Display>?,
    window: Window,
    valueMask: UInt,
    changes: CValuesRef<XWindowChanges>?,
) = 0

@ExperimentalForeignApi
fun mockXSelectInput(
    display: CValuesRef<Display>?,
    window: Window,
    eventMask: Long,
) = 0

@ExperimentalForeignApi
fun mockXAddToSaveSet(display: CValuesRef<Display>?, window: Window) = 0

@ExperimentalForeignApi
fun mockXRemoveFromSaveSet(display: CValuesRef<Display>?, window: Window) = 0

@ExperimentalForeignApi
fun mockXftDrawCreate(
    display: CValuesRef<Display>?,
    drawable: Drawable,
    visual: CValuesRef<Visual>?,
    colormap: Colormap
): CPointer<XftDraw>? {
    return nativeHeap.allocPointerTo<XftDraw>().value
}

@ExperimentalForeignApi
fun mockXftDrawDestroy(xftDraw: CValuesRef<XftDraw>?) {
    if (xftDraw != null) {
        nativeHeap.free(xftDraw.objcPtr())
    }
}

@ExperimentalForeignApi
fun mockXftDrawRect(
    xftDraw: CValuesRef<XftDraw>?,
    color: CValuesRef<XftColor>?,
    x: Int,
    y: Int,
    width: UInt,
    height: UInt
) {
}

@ExperimentalForeignApi
fun mockPangoLayoutSetText(layout: CValuesRef<PangoLayout>?, text: String?, length: Int) {}

@ExperimentalForeignApi
fun mockPangoLayoutSetWidth(layout: CValuesRef<PangoLayout>?, width: Int) {}

@ExperimentalForeignApi
fun mockPangoLayoutGetPixelExtents(
    layout: CValuesRef<PangoLayout>?,
    inkRect: CValuesRef<PangoRectangle>?,
    logicalRect: CValuesRef<PangoRectangle>?
) {
}

@ExperimentalForeignApi
fun mockPangoLayoutGetLineReadonly(layout: CValuesRef<PangoLayout>?, line: Int): CPointer<PangoLayoutLine>? = null

@ExperimentalForeignApi
fun mockPangoXftRenderLayoutLine(
    xftDraw: CValuesRef<XftDraw>?,
    color: CValuesRef<XftColor>?,
    layoutLine: CValuesRef<PangoLayoutLine>?,
    x: Int,
    y: Int
) {
}

@ExperimentalForeignApi
fun mockMqOpen(name: String?, oflag: Int, permissions: mode_t, attributes: mq_attr): mqd_t = 1

fun mockMqClose(mqd: mqd_t) = 0

fun mockMqSend(mqd: mqd_t, message: String?, messageSize: size_t, prio: UInt) = 0

@ExperimentalForeignApi
fun mockMqReceive(mqd: mqd_t, message: CValuesRef<ByteVar>?, length: size_t, prio: CValuesRef<UIntVar>?): ssize_t = 0

fun mockMqUnlink(name: String?) = 0
