package de.atennert.lcarswm.window

import de.atennert.lcarswm.FramedWindow
import xlib.Window

class WindowList {
    private val windows = mutableSetOf<FramedWindow>()

    fun add(window: FramedWindow) {
        windows.add(window)
    }

    fun remove(window: FramedWindow) {
        windows.remove(window)
    }

    fun remove(windowId: Window) {
        windows.removeAll { it.id == windowId }
    }

    fun get(windowId: Window): FramedWindow? {
        return windows.firstOrNull { it.id == windowId }
    }

    fun update(window: FramedWindow) {
        windows.removeAll { it.id == window.id }
    }

    fun isManaged(windowId: Window): Boolean {
        return windows.any { it.id == windowId }
    }
}