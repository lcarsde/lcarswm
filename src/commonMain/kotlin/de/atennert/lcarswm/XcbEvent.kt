package de.atennert.lcarswm

/**
 * Enum for XCB events that can be received via xcb_wait_for_event
 */
enum class XcbEvent(val code: Int) {
    XCB_KEY_PRESS(2),
    XCB_KEY_RELEASE(3),
    XCB_BUTTON_PRESS(4),
    XCB_BUTTON_RELEASE(5),
    XCB_MOTION_NOTIFY(6),
    XCB_ENTER_NOTIFY(7),
    XCB_CREATE_NOTIFY(16),
    XCB_DESTROY_NOTIFY(17),
    XCB_UNMAP_NOTIFY(18),
    XCB_MAP_NOTIFY(19),
    XCB_MAP_REQUEST(20),
    XCB_REPARENT_NOTIFY(21),
    XCB_CONFIGURE_NOTIFY(22),
    XCB_CONFIGURE_REQUEST(23),
    XCB_CIRCULATE_REQUEST(27),
    XCB_CLIENT_MESSAGE(33),
    XCB_MAPPING_NOTIFY(34);

    companion object {
        fun getEventTypeForCode(code: Int): XcbEvent {
            return values().find { it.code == code }
                ?: throw IllegalArgumentException("XcbEvent::getEventTypeForCode::unknown code $code")
        }
    }
}
