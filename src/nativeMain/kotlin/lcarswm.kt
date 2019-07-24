import cnames.structs.xcb_connection_t
import de.atennert.lcarswm.*
import kotlinx.cinterop.*
import xcb.*

private const val NO_RANDR_BASE = -1

private val WM_MODIFIER_KEY = XCB_MOD_MASK_4 // should be windows key

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

        val windowManagerConfig = WindowManagerState(screen.root) { getAtom(xcbConnection, it) }

        val randrBase = setupRandr(xcbConnection, windowManagerConfig)

        if (!setupKeys(xcbConnection, screen.root)) {
            xcb_disconnect(xcbConnection)
            error("::main::unable to setup the wm control keys")
        }

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

        // FIXME this needs be removed once RANDR development is finished. In the meantime it's here for convenience
        runProgram("VBoxClient-all")

        // event loop
        eventLoop(xcbConnection, windowManagerConfig, randrBase)

        xcb_disconnect(xcbConnection)
    }

    println("::main::lcarswm stopped")
}

/**
 * @return <code>true</code> if setting up the keys was successful, <code>false</code> otherwise.
 */
fun setupKeys(xcbConnection: CPointer<xcb_connection_t>, window: xcb_window_t): Boolean {
    // TODO create a key management
    val keySyms = xcb_key_symbols_alloc(xcbConnection)
    val keyCode = getKeyCodeFromKeySym(XK_Tab, keySyms)

    if (keyCode.toInt() == 0) {
        // if one key can't be set up nothing can
        xcb_key_symbols_free(keySyms)
        return false
    }

    xcb_grab_key(
        xcbConnection, 1.convert(), window, WM_MODIFIER_KEY.convert(), keyCode,
        XCB_GRAB_MODE_ASYNC.convert(), XCB_GRAB_MODE_ASYNC.convert()
    )

    xcb_flush(xcbConnection)
    xcb_key_symbols_free(keySyms)
    return true
}

/**
 * Get the key code for a key symbol for registration.
 */
fun getKeyCodeFromKeySym(keySym: Int, keySyms: CPointer<xcb_key_symbols_t>?): xcb_keycode_t {
    val keyPointer = xcb_key_symbols_get_keycode(keySyms, keySym.convert())

    if (keyPointer == null) {
        println("::getKeyCodeFromKeySym::unable to get key code for $keySym")
        return 0.convert()
    }

    val key = keyPointer.pointed.value
    nativeHeap.free(keyPointer)
    return key
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
private fun setupRandr(xcbConnection: CPointer<xcb_connection_t>, windowManagerState: WindowManagerState): Int {
    val extension = xcb_get_extension_data(xcbConnection, xcb_randr_id.ptr)!!.pointed

    if (extension.present.toInt() == 0) {
        println("::setupRandr::no RANDR extension")
        return NO_RANDR_BASE
    }

    handleRandrEvent(xcbConnection, windowManagerState)

    xcb_randr_select_input(
        xcbConnection, windowManagerState.screenRoot.convert(),
        (XCB_RANDR_NOTIFY_MASK_SCREEN_CHANGE or
                XCB_RANDR_NOTIFY_MASK_OUTPUT_CHANGE or
                XCB_RANDR_NOTIFY_CRTC_CHANGE or
                XCB_RANDR_NOTIFY_MASK_OUTPUT_PROPERTY).convert()
    )

    xcb_flush(xcbConnection)

    println("::setupRandr::RANDR base: ${extension.first_event}")

    return extension.first_event.toInt()
}

private fun eventLoop(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState,
    randrBase: Int
) {
    val randrEventValue = randrBase + XCB_RANDR_SCREEN_CHANGE_NOTIFY

    while (true) {
        val xEvent = xcb_wait_for_event(xcbConnection) ?: continue // TODO check for connection error
        val eventValue = xEvent.pointed.response_type.toInt()
        val eventId = eventValue and (0x08.inv())

        if (eventValue == randrEventValue) {
            println("::eventLoop::received randr event")
            handleRandrEvent(xcbConnection, windowManagerState)
            nativeHeap.free(xEvent)
            continue
        }

        try {
            val eventType = XcbEvent.getEventTypeForCode(eventId) // throws IllegalArgumentException

            if (EVENT_HANDLERS.containsKey(eventType)) {
                val stop = EVENT_HANDLERS[eventType]!!.invoke(xcbConnection, windowManagerState, xEvent)
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
