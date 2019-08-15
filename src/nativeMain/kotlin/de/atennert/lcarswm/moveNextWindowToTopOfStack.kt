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
    }
}