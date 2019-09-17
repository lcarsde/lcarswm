package de.atennert.lcarswm

import xlib.Window

interface WindowManagerStateHandler {
    fun removeWindow(windowId: Window)
}