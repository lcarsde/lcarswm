package de.atennert.lcarswm

import xlib.Window

open class WindowManagerStateMock: WindowManagerStateHandler {
    override fun removeWindow(windowId: Window) {}
}