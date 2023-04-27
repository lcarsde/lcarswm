package de.atennert.lcarswm.system

import kotlinx.cinterop.ptr
import platform.linux.*
import xlib.*

var wrapXGrabServer = ::XGrabServer

var wrapXUngrabServer = ::XUngrabServer

var wrapXGetWindowAttributes = ::XGetWindowAttributes

var wrapXChangeWindowAttributes = ::XChangeWindowAttributes

var wrapXGetTextProperty = ::XGetTextProperty

var wrapXGetWindowProperty = ::XGetWindowProperty

var wrapXChangeProperty = ::XChangeProperty

var wrapXCreateSimpleWindow = ::XCreateSimpleWindow

var wrapXDestroyWindow = ::XDestroyWindow

var wrapXReparentWindow = ::XReparentWindow

var wrapXMapWindow = ::XMapWindow

var wrapXUnmapWindow = ::XUnmapWindow

var wrapXClearWindow = ::XClearWindow

var wrapXMoveWindow = ::XMoveWindow

var wrapXResizeWindow = ::XResizeWindow

var wrapXMoveResizeWindow = ::XMoveResizeWindow

var wrapXSetWindowBorderWidth = ::XSetWindowBorderWidth

var wrapXFlush = ::XFlush

var wrapXCreatePixmap = ::XCreatePixmap

var wrapXFreePixmap = ::XFreePixmap

var wrapXFree = ::XFree

var wrapXSetWindowBackgroundPixmap = ::XSetWindowBackgroundPixmap

var wrapXGetTransientForHint = ::XGetTransientForHint

var wrapXSendEvent = ::XSendEvent

var wrapXConfigureWindow = ::XConfigureWindow

var wrapXSelectInput = ::XSelectInput

var wrapXAddToSaveSet = ::XAddToSaveSet

var wrapXRemoveFromSaveSet = ::XRemoveFromSaveSet

var wrapXftDrawCreate = ::XftDrawCreate

var wrapXftDrawDestroy = ::XftDrawDestroy

var wrapXftDrawRect = ::XftDrawRect

var wrapPangoLayoutSetText = ::pango_layout_set_text

var wrapPangoLayoutSetWidth = ::pango_layout_set_width

var wrapPangoLayoutGetPixelExtents = ::pango_layout_get_pixel_extents

var wrapPangoLayoutGetLineReadonly = ::pango_layout_get_line_readonly

var wrapPangoXftRenderLayoutLine = ::pango_xft_render_layout_line

var wrapMqOpen = ::mqWrapper
fun mqWrapper(name: String?, oflag: Int, permissions: mode_t, attributes: mq_attr): mqd_t {
    // this is required because Kotlin/Native doesn't like references to functions with varargs
    return mq_open(name, oflag, permissions, attributes.ptr)
}

var wrapMqClose = ::mq_close

var wrapMqSend = ::mq_send

var wrapMqReceive = ::mq_receive

var wrapMqUnlink = ::mq_unlink
