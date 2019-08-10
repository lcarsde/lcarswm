package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.toCValues
import xcb.*

/**
 *
 */
fun moveNextWindowToTopOfStack(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState) {
    val activeWindow = windowManagerState.toggleActiveWindow()
    println("::moveNextWindowToTopOfStack::activate window $activeWindow")
    if (activeWindow != null) {
        val params = UIntArray(1) { XCB_STACK_MODE_ABOVE }

        xcb_configure_window(
            xcbConnection, activeWindow.id,
            XCB_CONFIG_WINDOW_STACK_MODE.convert(), params.toCValues()
        )

        xcb_set_input_focus(xcbConnection, XCB_INPUT_FOCUS_POINTER_ROOT.convert(), activeWindow.id, XCB_CURRENT_TIME.convert())

        xcb_flush(xcbConnection)
    } else {
        // nothing to focus fall back to normal pointer based focus
        xcb_set_input_focus(xcbConnection, 0.convert(), XCB_INPUT_FOCUS_POINTER_ROOT, XCB_CURRENT_TIME.convert())
        xcb_flush(xcbConnection)
    }
}