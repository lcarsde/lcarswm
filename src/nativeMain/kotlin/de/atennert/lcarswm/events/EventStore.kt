package de.atennert.lcarswm.events

import de.atennert.rx.Subject
import xlib.Window
import xlib.XConfigureRequestEvent

data class ReparentEvent(val id: Window, val parentId: Window)

class EventStore {
    internal val destroySj = Subject<Window>()
    val destroyObs = destroySj.asObservable()

    internal val mapSj = Subject<Window>()
    val mapObs = mapSj.asObservable()

    internal val unmapSj = Subject<Window>()
    val unmapObs = unmapSj.asObservable()

    internal val reparentSj = Subject<ReparentEvent>()
    val reparentObs = reparentSj.asObservable()

    internal val propertyNotifyNameSj = Subject<Window>()
    val propertyNotifyNameObs = propertyNotifyNameSj.asObservable()

    internal val configureRequestSj = Subject<XConfigureRequestEvent>()
    val configureRequestObs = configureRequestSj.asObservable()

    internal val enterNotifySj = Subject<Window>()
    val enterNotifyObs = enterNotifySj.asObservable()

    internal val leaveNotifySj = Subject<Window>()
    val leaveNotifyObs = leaveNotifySj.asObservable()
}