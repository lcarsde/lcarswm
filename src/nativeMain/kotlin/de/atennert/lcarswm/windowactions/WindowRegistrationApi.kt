package de.atennert.lcarswm.windowactions

import xlib.Window

/**
 *
 */
interface WindowRegistrationApi {
    fun addWindow(windowId: Window, isSetup: Boolean)

    fun isWindowManaged(windowId: Window): Boolean

    fun removeWindow(windowId: Window)
}