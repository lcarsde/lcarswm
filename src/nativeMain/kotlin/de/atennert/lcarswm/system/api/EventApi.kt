package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CValuesRef
import xlib.Display
import xlib.Window
import xlib.XEvent
import xlib.XWindowChanges

/**
 *
 */
interface EventApi {

    fun sync(display: CValuesRef<Display>, discardQueuedEvents: Boolean): Int

    fun sendEvent(display: CValuesRef<Display>, window: Window, propagate: Boolean, eventMask: Long, event: CValuesRef<XEvent>): Int

    fun nextEvent(display: CValuesRef<Display>, event: CValuesRef<XEvent>): Int

    fun configureWindow(display: CValuesRef<Display>, window: Window, configurationMask: UInt, configuration: CValuesRef<XWindowChanges>): Int

    fun reparentWindow(display: CValuesRef<Display>, window: Window, parent: Window, x: Int, y: Int): Int

    fun resizeWindow(display: CValuesRef<Display>, window: Window, width: UInt, height: UInt): Int

    fun moveResizeWindow(display: CValuesRef<Display>, window: Window, x: Int, y: Int, width: UInt, height: UInt): Int

    fun mapWindow(display: CValuesRef<Display>, window: Window): Int

    fun unmapWindow(display: CValuesRef<Display>, window: Window): Int

    fun destroyWindow(display: CValuesRef<Display>, window: Window): Int
}