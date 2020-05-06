package de.atennert.lcarswm.window

import de.atennert.lcarswm.FramedWindow
import xlib.Window

class WindowList {
    private val windows = mutableSetOf<FramedWindow>()

    private val observers = mutableSetOf<Observer>()

    fun registerObserver(observer: Observer) {
        observers.add(observer)
    }

    fun add(window: FramedWindow) {
        windows.add(window)
        notifyObservers()
    }

    fun remove(window: FramedWindow) {
        windows.remove(window)
        notifyObservers()
    }

    fun remove(windowId: Window): FramedWindow? {
        val window = get(windowId) ?: return null
        remove(window)
        return window
    }

    fun get(windowId: Window): FramedWindow? {
        return windows.firstOrNull { it.id == windowId }
    }

    fun getAll(): Set<FramedWindow> {
        return windows
    }

    fun update(window: FramedWindow) {
        windows.add(window)
    }

    fun isManaged(windowId: Window): Boolean {
        return windows.any { it.id == windowId }
    }

    private fun notifyObservers() {
        observers.forEach { observer -> observer.listChanged() }
    }

    interface Observer {
        fun listChanged()
    }
}