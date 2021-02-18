package de.atennert.lcarswm.window

import de.atennert.lcarswm.keys.KeySessionManager
import xlib.Window

typealias FocusObserver = (Window?, Window?, Boolean) -> Unit

class WindowFocusHandler {
    private var activeWindow: Window? = null

    private val windowIdList = mutableListOf<Window>()

    private val observers = mutableListOf<FocusObserver>()

    private var toggleSessionActive = false

    val keySessionListener = object : KeySessionManager.KeySessionListener {
        override fun stopSession() {
            if (!toggleSessionActive) {
                return
            }
            toggleSessionActive = false
            activeWindow?.let {
                if (windowIdList[0] != it) {
                    putActiveWindowToFront()
                }
            }
            observers.forEach { it(activeWindow, null, false) }
        }
    }

    fun getFocusedWindow(): Window? {
        return activeWindow
    }

    fun registerObserver(observer: FocusObserver) {
        this.observers.add(observer)
        observer(activeWindow, null, false)
    }

    fun setFocusedWindow(activeWindow: Window) {
        setFocusedWindowInternal(activeWindow)
        putActiveWindowToFront()
    }

    private fun setFocusedWindowInternal(activeWindow: Window) {
        if (activeWindow == this.activeWindow) {
            return
        }

        val oldActiveWindow = this.activeWindow
        this.activeWindow = activeWindow
        this.observers.forEach { it(activeWindow, oldActiveWindow, toggleSessionActive) }
        if (!windowIdList.contains(activeWindow)) {
            windowIdList.add(activeWindow)
        }
    }

    fun removeWindow(window: Window) {
        windowIdList.remove(window)
        if (window == activeWindow) {
            this.activeWindow = windowIdList.getOrNull(0)
            this.observers.forEach { it(this.activeWindow, window, false) }
        }
    }

    fun toggleWindowFocusForward() {
        if (activeWindow == null) {
            return
        }
        toggleSessionActive = true
        val focusIndex = getIndexOfFocusedWindow()
        val nextWindow = getNextWindowToFocus(focusIndex)
        setFocusedWindowInternal(nextWindow)
    }

    fun toggleWindowFocusBackward() {
        if (activeWindow == null) {
            return
        }
        toggleSessionActive = true
        val focusIndex = getIndexOfFocusedWindow()
        val previousWindow = getPreviousWindowToFocus(focusIndex)
        setFocusedWindowInternal(previousWindow)
    }

    /**
     * When starting a new toggle session, the list should be ordered to that the active window is at the front.
     */
    private fun putActiveWindowToFront() {
        activeWindow?.let {
            windowIdList.remove(it)
            windowIdList.add(0, it)
        }
    }

    private fun getIndexOfFocusedWindow(): Int {
        return windowIdList.indexOf(activeWindow)
    }

    private fun getNextWindowToFocus(currentFocusIndex: Int): Window {
        val nextIndex = (currentFocusIndex + 1).rem(windowIdList.size)
        return windowIdList.elementAt(nextIndex)
    }

    private fun getPreviousWindowToFocus(currentFocusIndex: Int): Window {
        val previousIndex = if (currentFocusIndex - 1 == -1) {
            windowIdList.size - 1
        } else {
            currentFocusIndex - 1
        }
        return windowIdList.elementAt(previousIndex)
    }
}
