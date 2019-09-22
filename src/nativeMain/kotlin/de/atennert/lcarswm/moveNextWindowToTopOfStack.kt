package de.atennert.lcarswm

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.EventApi
import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun moveNextWindowToTopOfStack(eventApi: EventApi, logger: Logger, windowManagerState: WindowManagerState) {
    val activeWindow = windowManagerState.toggleActiveWindow()
    logger.logDebug("::moveNextWindowToTopOfStack::activate window $activeWindow")
    if (activeWindow != null) {
        val windowChanges = nativeHeap.alloc<XWindowChanges>()
        windowChanges.stack_mode = Above

        eventApi.configureWindow(activeWindow.frame, CWStackMode.convert(), windowChanges.ptr)
    }
}