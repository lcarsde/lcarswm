package de.atennert.lcarswm.window

import de.atennert.rx.BehaviorSubject
import de.atennert.rx.Subject
import xlib.Window

sealed class WindowEvent(val window: ManagedWmWindow<Window>)
class WindowAddedEvent(window: ManagedWmWindow<Window>) : WindowEvent(window)
class WindowUpdatedEvent(window: ManagedWmWindow<Window>) : WindowEvent(window)
class WindowRemovedEvent(window: ManagedWmWindow<Window>) : WindowEvent(window)

class WindowList {
    private val windowsSj = BehaviorSubject<Set<ManagedWmWindow<Window>>>(emptySet())
    val windowsObs = windowsSj.asObservable()
    private var windows by windowsSj

    private val windowEventSj = Subject<WindowEvent>()
    val windowEventObs = windowEventSj.asObservable()

    fun add(window: ManagedWmWindow<Window>) {
        windows += window
        windowEventSj.next(WindowAddedEvent(window))
    }

    fun remove(window: ManagedWmWindow<Window>) {
        windows -= window
        windowEventSj.next(WindowRemovedEvent(window))
    }

    fun remove(windowId: Window): ManagedWmWindow<Window>? {
        val window = get(windowId) ?: return null
        remove(window)
        return window
    }

    fun get(windowId: Window): ManagedWmWindow<Window>? {
        return windows.firstOrNull { it.id == windowId }
    }

    fun getByAny(windowId: Window): ManagedWmWindow<Window>? {
        return windows.firstOrNull { it.hasId(windowId) }
    }

    fun isManaged(windowId: Window): Boolean {
        return windows.any { it.id == windowId }
    }
}