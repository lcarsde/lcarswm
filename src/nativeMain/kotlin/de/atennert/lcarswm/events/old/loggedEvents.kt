package de.atennert.lcarswm.events.old

import de.atennert.lcarswm.log.Logger
import kotlinx.cinterop.get
import xlib.XEvent

fun logCreateNotify(logger: Logger, xEvent: XEvent): Boolean {
    val createEvent = xEvent.xcreatewindow
    logger.logDebug("::logCreateNotify::window ${createEvent.window}, o-r: ${createEvent.override_redirect}, parent: ${createEvent.parent}")
    return false
}

fun logConfigureNotify(logger: Logger, xEvent: XEvent): Boolean {
    val configureEvent = xEvent.xconfigure
    logger.logDebug("::logConfigureNotify::window ${configureEvent.window}, o r: ${configureEvent.override_redirect}, above ${configureEvent.above}, event ${configureEvent.event}")
    return false
}

fun logButtonPress(logger: Logger, xEvent: XEvent): Boolean {
    val pressEvent = xEvent.xbutton
    val button = pressEvent.button

    logger.logDebug("::logButtonPress::Button pressed: $button")
    return false
}

fun logButtonRelease(logger: Logger, xEvent: XEvent): Boolean {
    val pressEvent = xEvent.xbutton
    val button = pressEvent.button

    logger.logDebug("::logButtonRelease::Button released: $button")
    return false
}

fun logMapNotify(logger: Logger, xEvent: XEvent): Boolean {
    val mapEvent = xEvent.xmap
    val window = mapEvent.window
    logger.logDebug("::logMapNotify::map notify for window $window")

    return false
}

fun logReparentNotify(logger: Logger, xEvent: XEvent): Boolean {
    val reparentEvent = xEvent.xreparent
    logger.logDebug("::logReparentNotify::reparented window ${reparentEvent.window} to ${reparentEvent.parent}")
    return false
}

fun logClientMessage(logger: Logger, xEvent: XEvent): Boolean {
    val clientMessage = xEvent.xclient
    logger.logDebug("::logClientMessage::type: ${clientMessage.message_type}, format: ${clientMessage.format}, data: ${clientMessage.data.l[0]}")
    return false
}
