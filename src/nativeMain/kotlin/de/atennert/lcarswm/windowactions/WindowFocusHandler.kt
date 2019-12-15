package de.atennert.lcarswm.windowactions

import xlib.Window

class WindowFocusHandler {
    private var activeWindow: Window? = null

    private val observers = mutableListOf<(Window?) -> Unit>()

    fun getFocusedWindow(): Window? {
        return activeWindow
    }

    fun registerObserver(observer: (Window?) -> Unit) {
        this.observers.add(observer)
        observer(activeWindow)
    }

    fun setFocusedWindow(activeWindow: Window) {
        this.activeWindow = activeWindow
        this.observers.forEach { it(activeWindow) }
    }

    fun removeWindow(window: Window) {
        if (this.activeWindow == window) {
            this.activeWindow = null
            this.observers.forEach { it(null) }
        }
    }
}
