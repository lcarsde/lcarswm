package de.atennert.lcarswm.window

import xlib.Window

class WindowList {
    private val windows = mutableSetOf<FramedWindow>()

    private val observers = mutableSetOf<Observer>()

    fun registerObserver(observer: Observer) {
        observers.add(observer)
    }

    fun add(window: FramedWindow) {
        windows.add(window)
        observers.forEach { observer -> observer.windowAdded(window) }
    }

    fun remove(window: FramedWindow) {
        windows.remove(window)
        observers.forEach { observer -> observer.windowRemoved(window) }
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
        observers.forEach { observer -> observer.windowUpdated(window) }
    }

    fun isManaged(windowId: Window): Boolean {
        return windows.any { it.id == windowId }
    }

    interface Observer {
        fun windowAdded(window: FramedWindow)

        fun windowRemoved(window: FramedWindow)

        fun windowUpdated(window: FramedWindow)
    }
}