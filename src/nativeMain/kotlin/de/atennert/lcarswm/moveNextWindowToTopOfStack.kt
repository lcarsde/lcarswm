package de.atennert.lcarswm

import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun moveNextWindowToTopOfStack(display: CPointer<Display>, windowManagerState: WindowManagerState) {
    val activeWindow = windowManagerState.toggleActiveWindow()
    println("::moveNextWindowToTopOfStack::activate window $activeWindow")
    if (activeWindow != null) {
        val windowChanges = nativeHeap.alloc<XWindowChanges>()
        windowChanges.stack_mode = Above

        XConfigureWindow(display, activeWindow.id, CWStackMode, windowChanges.ptr)

//        xcb_set_input_focus(xcbConnection, XCB_INPUT_FOCUS_POINTER_ROOT.convert(), activeWindow.id, XCB_CURRENT_TIME.convert())
    } else {
        // nothing to focus fall back to normal pointer based focus
//        xcb_set_input_focus(xcbConnection, 0.convert(), XCB_INPUT_FOCUS_POINTER_ROOT, XCB_CURRENT_TIME.convert())
    }
}