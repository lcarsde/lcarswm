package de.atennert.lcarswm

import de.atennert.lcarswm.system.xEventApi
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

        xEventApi().configureWindow(display, activeWindow.frame, CWStackMode.convert(), windowChanges.ptr)
    }
}