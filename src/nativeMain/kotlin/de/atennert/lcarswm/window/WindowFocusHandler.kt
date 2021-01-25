package de.atennert.lcarswm.window

import de.atennert.lcarswm.keys.KeySessionManager
import xlib.Window

typealias FocusObserver = (Window?, Window?) -> Unit

class WindowFocusHandler {
    private var activeWindow: Window? = null

    private val windowIdList = mutableListOf<Window>()

    private val observers = mutableListOf<FocusObserver>()

    val keySessionListener = object : KeySessionManager.KeySessionListener {
        override fun stopSession() {
            activeWindow?.let {
                if (windowIdList[0] != it) {
                    putActiveWindowToFront()
                }
            }
        }
    }

    fun getFocusedWindow(): Window? {
        return activeWindow
    }

    fun registerObserver(observer: FocusObserver) {
        this.observers.add(observer)
        observer(activeWindow, null)
    }

    fun setFocusedWindow(activeWindow: Window) {
        putActiveWindowToFront()
        setFocusedWindowInternal(activeWindow)
    }

    private fun setFocusedWindowInternal(activeWindow: Window) {
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
        windowIdList.remove(window)
        if (window == activeWindow) {
            this.activeWindow = windowIdList.getOrNull(0)
            this.observers.forEach { it(this.activeWindow, window) }
        }
    }

    fun toggleWindowFocus() {
        if (activeWindow == null) {
            return
        }
        val focusIndex = getIndexOfFocusedWindow()
        val nextWindow = getNextWindowToFocus(focusIndex)
        setFocusedWindowInternal(nextWindow)
    }

    /**
     * When starting a new toggle session, the list should be ordered to that the active window is at the front.
     */
    private fun putActiveWindowToFront() {
        if (windowIdList.isEmpty()) {
            return
        }
        windowIdList.remove(activeWindow)
        windowIdList.add(0, activeWindow!!)
    }

    private fun getIndexOfFocusedWindow(): Int {
        return windowIdList.indexOf(activeWindow)
    }

    private fun getNextWindowToFocus(currentFocusIndex: Int): Window {
        val nextIndex = (currentFocusIndex + 1).rem(windowIdList.size)
        return windowIdList.elementAt(nextIndex)
    }
}
