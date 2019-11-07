package de.atennert.lcarswm.windowactions

import xlib.Window

/**
 *
 */
interface WindowRegistrationApi {
    fun addWindow(windowId: Window, isSetup: Boolean)
}