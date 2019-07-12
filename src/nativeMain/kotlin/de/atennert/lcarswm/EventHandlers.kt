package de.atennert.lcarswm

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toCValues
import xcb.*

/**
 * TODO description
 */
fun handleButtonRelease(xEvent: CPointer<xcb_generic_event_t>): Boolean {
    @Suppress("UNCHECKED_CAST")
    val button = (xEvent as CPointer<xcb_button_release_event_t>).pointed.detail.toInt()
    println("::handleButtonRelease::Button released: $button")
    return button != 2
}

/**
 * TODO description
 */
fun handleMapRequest(xcbConnection: CPointer<xcb_connection_t>, xEvent: CPointer<xcb_generic_event_t>): Boolean {
    println("::handleMapRequest::map request")
    @Suppress("UNCHECKED_CAST")
    val mapEvent = xEvent as CPointer<xcb_map_request_event_t>
    xcb_map_window(xcbConnection, mapEvent.pointed.window)
    xcb_flush(xcbConnection)
    return false
}

/**
 * TODO description
 */
fun handleConfigureRequest(xcbConnection: CPointer<xcb_connection_t>, xEvent: CPointer<xcb_generic_event_t>): Boolean {
    println("::handleConfigureRequest::configure request")
    @Suppress("UNCHECKED_CAST")
    val configureEvent = (xEvent as CPointer<xcb_configure_request_event_t>).pointed

    val conf = WindowConfig(
        configureEvent.x.toLong(),
        configureEvent.y.toLong(),
        configureEvent.width.toLong(),
        configureEvent.height.toLong(),
        configureEvent.stack_mode.toLong(),
        configureEvent.sibling.toLong()
    )

    val (mask, valueList) = configureWindow(configureEvent.value_mask.toInt(), conf)

    if (!valueList.isEmpty()) {
        val uintList = valueList.map { it.toUInt() }
        val uintArray = UIntArray(7) { uintList.getOrElse(it) { 0.toUInt() } }

        xcb_configure_window(
            xcbConnection, configureEvent.window, mask.convert(), uintArray.toCValues()
        )
        xcb_flush(xcbConnection)
    }
    return false
}
