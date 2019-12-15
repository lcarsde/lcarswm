package de.atennert.lcarswm.windowactions

import xlib.Window

class WindowFocusHandler {
    private var activeWindow: Window? = null

    private val windowIdList = mutableSetOf<Window>()

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
        if (!windowIdList.contains(activeWindow)) {
            windowIdList.add(activeWindow)
        }
    }

    fun removeWindow(window: Window) {
        windowIdList.remove(window)
        this.activeWindow = if (windowIdList.isEmpty()) {
            null
        } else {
            windowIdList.first()
        }
        this.observers.forEach { it(this.activeWindow) }
    }
}
