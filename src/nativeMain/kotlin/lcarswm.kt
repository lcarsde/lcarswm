import cnames.structs.xcb_connection_t
import de.atennert.lcarswm.EVENT_HANDLERS
import de.atennert.lcarswm.XcbEvent
import kotlinx.cinterop.*
import xcb.*

private const val NO_RANDR_BASE = -1

fun main() {
    println("::main::start lcarswm initialization")

    memScoped {
        val screenNumber = alloc<IntVar>()

        // TODO support multi screen
        val xcbConnection = xcb_connect(null, screenNumber.ptr)
        if (xcbConnection == null || (xcb_connection_has_error(xcbConnection) != 0)) {
            error("::main::no XCB connection from setup")
        }

        val screen = xcb_aux_get_screen(xcbConnection, screenNumber.value)?.pointed ?: error("::main::got no screen")
        println("::main::Screen size: ${screen.width_in_pixels}/${screen.height_in_pixels}, root: ${screen.root}")

        val randrBase = setupRandr(xcbConnection, screen)

        // register buttons
        registerButton(xcbConnection, screen.root, 1) // left mouse button
        registerButton(xcbConnection, screen.root, 2) // middle mouse button
        registerButton(xcbConnection, screen.root, 3) // right mouse button

        val values = UIntArray(2)
        values[0] = XCB_EVENT_MASK_SUBSTRUCTURE_REDIRECT or
                XCB_EVENT_MASK_STRUCTURE_NOTIFY or
                XCB_EVENT_MASK_SUBSTRUCTURE_NOTIFY

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
        eventLoop(xcbConnection, randrBase)

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

/**
 * @return RANDR base value
 */
private fun setupRandr(xcbConnection: CPointer<xcb_connection_t>, screen: xcb_screen_t): Int {
    val extension = xcb_get_extension_data(xcbConnection, xcb_randr_id.ptr)!!.pointed

    if (extension.present.toInt() == 0) {
        println("::setupRandr::no RANDR extension")
        return NO_RANDR_BASE
    }

    xcb_randr_select_input(
        xcbConnection, screen.root,
        (XCB_RANDR_NOTIFY_MASK_SCREEN_CHANGE or
                XCB_RANDR_NOTIFY_MASK_OUTPUT_CHANGE or
                XCB_RANDR_NOTIFY_CRTC_CHANGE or
                XCB_RANDR_NOTIFY_MASK_OUTPUT_PROPERTY).convert()
    )

    xcb_flush(xcbConnection)

    println("::setupRandr::RANDR base: ${extension.first_event}")

    return extension.first_event.toInt()
}

private fun eventLoop(xcbConnection: CPointer<xcb_connection_t>, randrBase: Int) {
    val randrEventValue = randrBase + XCB_RANDR_SCREEN_CHANGE_NOTIFY

    while (true) {
        val xEvent = xcb_wait_for_event(xcbConnection) ?: continue // TODO check for connection error
        val eventValue = xEvent.pointed.response_type.toInt()
        val eventId = eventValue and (0x08.inv())

        if (eventValue == randrEventValue) {
            println("::eventLoop::received randr event")
            nativeHeap.free(xEvent)
            continue
        }

        try {
            val eventType = XcbEvent.getEventTypeForCode(eventId) // throws IllegalArgumentException

            if (EVENT_HANDLERS.containsKey(eventType)) {
                val stop = EVENT_HANDLERS[eventType]!!.invoke(xcbConnection, xEvent)
                if (stop) {
                    break
                }
            } else {
                println("::eventLoop::unhandled event: $eventType > $eventId")
            }
        } catch (ex: IllegalArgumentException) {
            println("WARN: " + ex.message)
        }

        nativeHeap.free(xEvent)
    }
}
