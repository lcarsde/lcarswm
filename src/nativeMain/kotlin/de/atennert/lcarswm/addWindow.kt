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

    println(
        "::addWindow::monitor: ${windowMonitor.name}, setup: $isSetup, position: $measurements, class: ${windowAttributes.`class`}, b-grav: ${windowAttributes.bit_gravity}, propagate mask: ${windowAttributes.do_not_propagate_mask}, map state: ${windowAttributes.map_state}, redirect: ${windowAttributes.override_redirect}, map installed: ${windowAttributes.map_installed}, visual: ${windowAttributes.visual}, w-grav: ${windowAttributes.win_gravity}"
    )

    window.frame = XCreateSimpleWindow(display, lcarsWindow, measurements[0], measurements[1],
        measurements[2].convert(), measurements[3].convert(), 0.convert(), 0.convert(), 0.convert())

    XSelectInput(display, window.frame, SubstructureRedirectMask or SubstructureNotifyMask)

    XAddToSaveSet(display, windowId)

    XReparentWindow(display, windowId, window.frame, 0, 0)

    XMapWindow(display, window.frame)

    adjustWindowPositionAndSize(
        display,
        measurements,
        window
    )

    nativeHeap.free(windowAttributes)
}