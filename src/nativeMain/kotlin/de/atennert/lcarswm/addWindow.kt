package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun addWindow(display: CPointer<Display>, windowManagerState: WindowManagerState, lcarsWindow: ULong, window: ULong, isSetup: Boolean) {
    val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
    XGetWindowAttributes(display, window, windowAttributes.ptr)

    if (windowAttributes.override_redirect != 0 || (isSetup &&
            windowAttributes.map_state != IsViewable)) {
        println("::addWindow::skipping window $window")
        nativeHeap.free(windowAttributes)
        return
    }
    val windowMonitor = windowManagerState.addWindow(Window(window))

    val measurements = windowMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowMonitor))

    println(
        "::addWindow::monitor: ${windowMonitor.name}, setup: $isSetup, position: $measurements, class: ${windowAttributes.`class`}, b-grav: ${windowAttributes.bit_gravity}, propagate mask: ${windowAttributes.do_not_propagate_mask}, map state: ${windowAttributes.map_state}, redirect: ${windowAttributes.override_redirect}, map installed: ${windowAttributes.map_installed}, visual: ${windowAttributes.visual}, w-grav: ${windowAttributes.win_gravity}"
    )

    XAddToSaveSet(display, window)

    XReparentWindow(display, window, lcarsWindow, measurements[0], measurements[1])

    adjustWindowPositionAndSize(
        display,
        measurements,
        window
    )

    nativeHeap.free(windowAttributes)
}