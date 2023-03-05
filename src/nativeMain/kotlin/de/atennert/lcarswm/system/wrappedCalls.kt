package de.atennert.lcarswm.system

import xlib.*

var wrapXCreateSimpleWindow = ::XCreateSimpleWindow

var wrapXDestroyWindow = ::XDestroyWindow

var wrapXMapWindow = ::XMapWindow

var wrapXUnmapWindow = ::XUnmapWindow

var wrapXClearWindow = ::XClearWindow

var wrapXMoveWindow = ::XMoveWindow

var wrapXCreatePixmap = ::XCreatePixmap

var wrapXFreePixmap = ::XFreePixmap

var wrapXSetWindowBackgroundPixmap = ::XSetWindowBackgroundPixmap

var wrapXftDrawCreate = ::XftDrawCreate

var wrapXftDrawDestroy = ::XftDrawDestroy

var wrapXftDrawRect = ::XftDrawRect

var wrapPangoLayoutSetText = ::pango_layout_set_text

var wrapPangoLayoutSetWidth = ::pango_layout_set_width

var wrapPangoLayoutGetPixelExtents = ::pango_layout_get_pixel_extents

var wrapPangoLayoutGetLineReadonly = ::pango_layout_get_line_readonly

var wrapPangoXftRenderLayoutLine = ::pango_xft_render_layout_line
