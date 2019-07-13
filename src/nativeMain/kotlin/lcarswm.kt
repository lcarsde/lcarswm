import cnames.structs.xcb_connection_t
import de.atennert.lcarswm.*
import kotlinx.cinterop.*
import xcb.*

fun main() {
    println("::main::start lcarswm initialization")

    memScoped {
        val screenNumber = alloc<IntVar>()

        val xcbConnection = xcb_connect(null, screenNumber.ptr)
        if (xcbConnection == null || (xcb_connection_has_error(xcbConnection) != 0)) {
            error("::main::no XCB connection from setup")
        }

        val screen = xcb_aux_get_screen(xcbConnection, screenNumber.value)?.pointed ?: error("::main::got no screen")
        println("::main::Screen size: ${screen.width_in_pixels}/${screen.height_in_pixels}, root: ${screen.root}")

        // register buttons
        registerButton(xcbConnection, screen.root, 1) // left mouse button
        registerButton(xcbConnection, screen.root, 2) // middle mouse button
        registerButton(xcbConnection, screen.root, 3) // right mouse button

        val values = UIntArray(2)
        values[0] =
            XCB_EVENT_MASK_SUBSTRUCTURE_REDIRECT or XCB_EVENT_MASK_STRUCTURE_NOTIFY or XCB_EVENT_MASK_SUBSTRUCTURE_NOTIFY

        val cookie =
            xcb_change_window_attributes_checked(xcbConnection, screen.root, XCB_CW_EVENT_MASK, values.toCValues())
        val error = xcb_request_check(xcbConnection, cookie)

        xcb_flush(xcbConnection)

        if (error != null) {
            xcb_disconnect(xcbConnection)
            error(
                "::main::Can't get SUBSTRUCTURE REDIRECT. Error code: ${error.pointed.error_code}\n" +
                        "Another window manager running? Exiting."
            )
        }

        // event loop
        eventLoop(xcbConnection)

        xcb_disconnect(xcbConnection)
    }

    println("::main::lcarswm stopped")
}

private fun registerButton(xcbConnection: CPointer<xcb_connection_t>, window: xcb_window_t, buttonId: Int) {
    xcb_grab_button(
        xcbConnection, 0.convert(), window,
        (XCB_EVENT_MASK_BUTTON_PRESS or XCB_EVENT_MASK_BUTTON_RELEASE).convert(),
        XCB_GRAB_MODE_ASYNC.convert(), XCB_GRAB_MODE_ASYNC.convert(), window,
        XCB_NONE.convert(), buttonId.convert(), XCB_NONE.convert()
    )
}

private fun eventLoop(xcbConnection: CPointer<xcb_connection_t>) {
    while (true) {
        val xEvent = xcb_wait_for_event(xcbConnection)
        val eventType = xEvent?.pointed?.response_type ?: continue // TODO check for connection error
        val eventId = eventType.toInt() and (0x08.inv())

        if (eventHandlers.containsKey(eventId)) {
            val stop = eventHandlers[eventId]!!.invoke(xcbConnection, xEvent)
            if (stop) {
                break
            }
        } else {
            println("::eventLoop::unhandled event: $eventType")
        }

        nativeHeap.free(xEvent)
    }
}
