package de.atennert.lcarswm.events

import xlib.XEvent

fun logCreateNotify(xEvent: XEvent): Boolean {
    val createEvent = xEvent.xcreatewindow
    println("::logCreateNotify::window ${createEvent.window}, o r: ${createEvent.override_redirect}")
    return false
}

fun logConfigureNotify(xEvent: XEvent): Boolean {
    val configureEvent = xEvent.xconfigure
    println("::logConfigureNotify::window ${configureEvent.window}, o r: ${configureEvent.override_redirect}, above ${configureEvent.above}, event ${configureEvent.event}")
    return false
}

fun logButtonPress(xEvent: XEvent): Boolean {
    val pressEvent = xEvent.xbutton
    val button = pressEvent.button

    println("::logButtonPress::Button pressed: $button")
    return false
}

fun logButtonRelease(xEvent: XEvent): Boolean {
    val pressEvent = xEvent.xbutton
    val button = pressEvent.button

    println("::logButtonRelease::Button released: $button")
    return false
}

fun logMapNotify(xEvent: XEvent): Boolean {
    val mapEvent = xEvent.xmap
    val window = mapEvent.window
    println("::logMapNotify::map notify for window $window")

    return false
}

fun logReparentNotify(xEvent: XEvent): Boolean {
    val reparentEvent = xEvent.xreparent
    println("::logReparentNotify::reparented window ${reparentEvent.window} to ${reparentEvent.parent}")
    return false
}
