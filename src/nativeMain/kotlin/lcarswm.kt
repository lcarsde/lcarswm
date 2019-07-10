import cnames.structs.xcb_connection_t
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
        values[0] = XCB_EVENT_MASK_SUBSTRUCTURE_REDIRECT or XCB_EVENT_MASK_STRUCTURE_NOTIFY or XCB_EVENT_MASK_SUBSTRUCTURE_NOTIFY

        val cookie = xcb_change_window_attributes_checked(xcbConnection, screen.root, XCB_CW_EVENT_MASK, values.toCValues())
        val error = xcb_request_check(xcbConnection, cookie)

        xcb_flush(xcbConnection)

        if (error != null) {
            xcb_disconnect(xcbConnection)
            error("::main::Can't get SUBSTRUCTURE REDIRECT. Error code: ${error.pointed.error_code}\n" +
                    "Another window manager running? Exiting.")
        }

        // event loop
        eventLoop(xcbConnection)

        xcb_disconnect(xcbConnection)
    }

    println("::main::lcarswm stopped")
}

fun registerButton(xcbConnection: CPointer<xcb_connection_t>, window: xcb_window_t, buttonId: Int) {
    xcb_grab_button(xcbConnection, 0, window,
        (XCB_EVENT_MASK_BUTTON_PRESS or XCB_EVENT_MASK_BUTTON_RELEASE).convert(),
        XCB_GRAB_MODE_ASYNC.convert(), XCB_GRAB_MODE_ASYNC.convert(), window,
        XCB_NONE.convert(), buttonId.convert(), XCB_NONE.convert())
}


class WinConf(val x: UInt, val y: UInt, val width: UInt, val height: UInt, val stackMode: UInt, val sibling: xcb_window_t)

fun eventLoop(xcbConnection: CPointer<xcb_connection_t>) {
    var keepRunning = true

    while (keepRunning) {
        val xEvent = xcb_wait_for_event(xcbConnection)
        val eventType = xEvent?.pointed?.response_type ?: continue
        val maskedEvent = eventType.toInt() and (0x08.inv())

        when (maskedEvent) {
            XCB_BUTTON_RELEASE -> {
                @Suppress("UNCHECKED_CAST")
                val button = (xEvent as CPointer<xcb_button_release_event_t>).pointed.detail.toInt()
                println("Button pressed: $button")
                if (button != 2) {
                    keepRunning = false
                }
            }
            XCB_CONFIGURE_REQUEST -> {
                println("configure request")
                @Suppress("UNCHECKED_CAST")
                val configureEvent = (xEvent as CPointer<xcb_configure_request_event_t>).pointed

                val conf = WinConf(
                    configureEvent.x.toUInt(),
                    configureEvent.y.toUInt(),
                    configureEvent.width.toUInt(),
                    configureEvent.height.toUInt(),
                    configureEvent.stack_mode.toUInt(),
                    configureEvent.sibling
                )
                configureWindow(xcbConnection, configureEvent.window, configureEvent.value_mask, conf)
            }
            XCB_MAP_REQUEST -> {
                println("map request")
                @Suppress("UNCHECKED_CAST")
                val mapEvent = xEvent as CPointer<xcb_map_request_event_t>
                xcb_map_window(xcbConnection, mapEvent.pointed.window)
                xcb_flush(xcbConnection)
            }
            else -> println("unhandled event: $eventType")
        }
    }
}

fun configureWindow(xcbConnection: CPointer<xcb_connection_t>, window: xcb_window_t, mask: uint16_t, wc: WinConf) {
    val values = UIntArray(7)
    var i = -1
    var newMask = mask

    if (mask.toUInt() and XCB_CONFIG_WINDOW_X == XCB_CONFIG_WINDOW_X)
    {
        newMask = newMask or XCB_CONFIG_WINDOW_X.convert()
        i ++
        values[i] = wc.x
    }

    if (mask.toUInt() and XCB_CONFIG_WINDOW_Y == XCB_CONFIG_WINDOW_Y)
    {
        newMask = newMask or XCB_CONFIG_WINDOW_Y.convert()
        i ++
        values[i] = wc.y
    }

    if (mask.toUInt() and XCB_CONFIG_WINDOW_WIDTH == XCB_CONFIG_WINDOW_WIDTH)
    {
        newMask = newMask or XCB_CONFIG_WINDOW_WIDTH.convert()
        i ++
        values[i] = wc.width
    }

    if (mask.toUInt() and XCB_CONFIG_WINDOW_HEIGHT == XCB_CONFIG_WINDOW_HEIGHT)
    {
        newMask = newMask or XCB_CONFIG_WINDOW_HEIGHT.convert()
        i ++
        values[i] = wc.height
    }

    if (mask.toUInt() and XCB_CONFIG_WINDOW_SIBLING == XCB_CONFIG_WINDOW_SIBLING)
    {
        newMask = newMask or XCB_CONFIG_WINDOW_SIBLING.convert()
        i ++
        values[i] = wc.sibling
    }

    if (mask.toUInt() and XCB_CONFIG_WINDOW_STACK_MODE == XCB_CONFIG_WINDOW_STACK_MODE)
    {
        newMask = newMask or XCB_CONFIG_WINDOW_STACK_MODE.convert()
        i ++
        values[i] = wc.stackMode
    }

    if (-1 != i)
    {
        xcb_configure_window(xcbConnection, window, newMask.convert(), values.toCValues())
        xcb_flush(xcbConnection)
    }
}
