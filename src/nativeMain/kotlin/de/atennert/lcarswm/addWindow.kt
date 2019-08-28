package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun addWindow(display: CPointer<Display>, windowManagerState: WindowManagerState, lcarsWindow: ULong, windowId: ULong, isSetup: Boolean) {
    val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
    XGetWindowAttributes(display, windowId, windowAttributes.ptr)

    if (windowAttributes.override_redirect != 0 || (isSetup &&
            windowAttributes.map_state != IsViewable)) {
        println("::addWindow::skipping window $windowId")
        nativeHeap.free(windowAttributes)
        return
    }

    val window = Window(windowId)
    val windowMonitor = windowManagerState.addWindow(window)

    val measurements = windowMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowMonitor))

    window.frame = XCreateSimpleWindow(display, lcarsWindow, measurements[0], measurements[1],
        measurements[2].convert(), measurements[3].convert(), 0.convert(), 0.convert(), 0.convert())

    XSelectInput(display, window.frame, SubstructureRedirectMask or SubstructureNotifyMask)

    XAddToSaveSet(display, windowId)

    XReparentWindow(display, windowId, window.frame, 0, 0)

    XResizeWindow(
        display,
        window.id,
        measurements[2].convert(),
        measurements[3].convert()
    )

    XMapWindow(display, window.frame)

    XMapWindow(display, window.id)

    nativeHeap.free(windowAttributes)
}