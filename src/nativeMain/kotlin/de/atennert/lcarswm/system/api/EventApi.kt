package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import xlib.Display
import xlib.Window
import xlib.XEvent
import xlib.XWindowChanges

/**
 *
 */
interface EventApi {

    fun sync(discardQueuedEvents: Boolean): Int

    fun sendEvent(window: Window, propagate: Boolean, eventMask: Long, event: CPointer<XEvent>): Int

    fun nextEvent(event: CPointer<XEvent>): Int

    fun getQueuedEvents(mode: Int): Int

    fun configureWindow(window: Window, configurationMask: UInt, configuration: CPointer<XWindowChanges>): Int

    fun setWindowBorderWidth(window: Window, borderWidth: UInt): Int

    fun reparentWindow(window: Window, parent: Window, x: Int, y: Int): Int

    fun resizeWindow(window: Window, width: UInt, height: UInt): Int

    fun moveResizeWindow(window: Window, x: Int, y: Int, width: UInt, height: UInt): Int

    fun lowerWindow(window: Window): Int

    fun mapWindow(window: Window): Int

    fun unmapWindow(window: Window): Int

    fun destroyWindow(window: Window): Int

    fun flush()
}