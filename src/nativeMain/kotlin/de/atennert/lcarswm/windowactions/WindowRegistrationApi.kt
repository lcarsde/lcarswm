package de.atennert.lcarswm.windowactions

import xlib.Window

/**
 * Interface to the window registration, which handles windows, not monitors and not their combination.
 */
interface WindowRegistrationApi {
    fun addWindow(windowId: Window, isSetup: Boolean)

    fun isWindowManaged(windowId: Window): Boolean

    fun removeWindow(windowId: Window)
}