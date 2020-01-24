package de.atennert.lcarswm.windowactions

import xlib.Window

/**
 * Interface to the window registration, which handles windows, not monitors and not their combination.
 */
interface WindowRegistration {
    /**
     * Register a new window to the window manager.
     * @param windowId ID of the window to register
     * @param isSetup true if the adding is happening in setup stage, false otherwise
     */
    fun addWindow(windowId: Window, isSetup: Boolean)

    /**
     * @return true, if the window with the given window ID is managed by the window manager, false otherwise
     */
    fun isWindowManaged(windowId: Window): Boolean

    /**
     * Unregister a window from the window manager.
     * @param windowId ID of the window to unregister
     */
    fun removeWindow(windowId: Window)
}