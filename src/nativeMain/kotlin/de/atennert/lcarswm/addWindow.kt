package de.atennert.lcarswm

import de.atennert.lcarswm.system.xEventApi
import de.atennert.lcarswm.system.xInputApi
import de.atennert.lcarswm.system.xWindowUtilApi
import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun addWindow(display: CPointer<Display>, windowManagerState: WindowManagerState, rootWindow: Window, windowId: Window, isSetup: Boolean) {
    val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
    xWindowUtilApi().getWindowAttributes(display, windowId, windowAttributes.ptr)

    if (windowAttributes.override_redirect != 0 || (isSetup &&
            windowAttributes.map_state != IsViewable)) {
        println("::addWindow::skipping window $windowId")
        nativeHeap.free(windowAttributes)
        return
    }

    val window = WindowContainer(windowId)
    val windowMonitor = windowManagerState.addWindow(window)

    val measurements = windowMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowMonitor))

    window.frame = XCreateSimpleWindow(display, rootWindow, measurements[0], measurements[1],
        measurements[2].convert(), measurements[3].convert(), 0.convert(), 0.convert(), 0.convert())

    xInputApi().selectInput(display, window.frame, SubstructureRedirectMask or SubstructureNotifyMask)

    xWindowUtilApi().addToSaveSet(display, windowId)

    xEventApi().reparentWindow(display, windowId, window.frame, 0, 0)

    xEventApi().resizeWindow(display, window.id, measurements[2].convert(), measurements[3].convert())

    xEventApi().mapWindow(display, window.frame)

    xEventApi().mapWindow(display, window.id)

    nativeHeap.free(windowAttributes)
}