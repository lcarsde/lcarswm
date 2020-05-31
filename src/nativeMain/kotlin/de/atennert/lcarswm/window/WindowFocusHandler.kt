package de.atennert.lcarswm.window

import xlib.Window

typealias FocusObserver = (Window?, Window?) -> Unit

class WindowFocusHandler {
    private var activeWindow: Window? = null

    private val windowIdList = mutableListOf<Window>()

    private val observers = mutableListOf<FocusObserver>()

    fun getFocusedWindow(): Window? {
        return activeWindow
    }

    fun registerObserver(observer: FocusObserver) {
        this.observers.add(observer)
        observer(activeWindow, null)
    }

    fun setFocusedWindow(activeWindow: Window) {
        if (activeWindow == this.activeWindow) {
            return
        }

        val oldActiveWindow = this.activeWindow
        this.activeWindow = activeWindow
        this.observers.forEach { it(activeWindow, oldActiveWindow) }
        if (!windowIdList.contains(activeWindow)) {
            windowIdList.add(activeWindow)
        }
    }

    fun removeWindow(window: Window) {
        val focusIndex = getIndexOfFocusedWindow()
        windowIdList.remove(window)
        if (window == activeWindow) {
            this.activeWindow = getPreviousWindowToFocus(focusIndex)
            this.observers.forEach { it(this.activeWindow, window) }
        }
    }

    fun toggleWindowFocus() {
        if (activeWindow == null) {
            return
        }
        val focusIndex = getIndexOfFocusedWindow()
        val nextWindow = getNextWindowToFocus(focusIndex)
        setFocusedWindow(nextWindow)
    }

    private fun getIndexOfFocusedWindow(): Int {
        return windowIdList.indexOf(activeWindow)
    }

    private fun getNextWindowToFocus(currentFocusIndex: Int): Window {
        val nextIndex = (currentFocusIndex + 1).rem(windowIdList.size)
        return windowIdList.elementAt(nextIndex)
    }

    private fun getPreviousWindowToFocus(currentFocusIndex: Int): Window? {
        if (windowIdList.isEmpty()) {
            return null
        }

        val nextIndex = if (currentFocusIndex > 0) currentFocusIndex - 1 else windowIdList.size - 1
        return windowIdList.elementAt(nextIndex)
    }
}
