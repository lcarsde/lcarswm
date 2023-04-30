package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.rx.BehaviorSubject
import de.atennert.rx.Observable
import de.atennert.rx.operators.combineLatestWith
import de.atennert.rx.operators.map
import kotlinx.cinterop.convert
import xlib.RROutput

open class MonitorManagerMock : MonitorManager<RROutput> {
    private val screenModeSj = BehaviorSubject(ScreenMode.NORMAL)
    override val screenModeObs = screenModeSj.asObservable()
    var screenMode by screenModeSj

    val primaryMonitorSj = BehaviorSubject<Monitor<RROutput>?>(Monitor(this, 42.convert(), "", true))
    var primaryMonitor by primaryMonitorSj

    val otherMonitorsSj = BehaviorSubject(emptyList<Monitor<RROutput>>())
    var otherMonitors by otherMonitorsSj

    override val monitorsObs: Observable<List<Monitor<RROutput>>> = primaryMonitorSj
        .apply(combineLatestWith(otherMonitorsSj.asObservable()))
        .apply(map { (primaryMonitor, otherMonitors) ->
            if (primaryMonitor != null) {
                listOf(primaryMonitor).plus(otherMonitors)
            } else {
                otherMonitors
            }
        })

    val functionCalls = mutableListOf<FunctionCall>()
    override val primaryMonitorObs = monitorsObs.apply(map { monitors -> monitors.find { it.isPrimary } })
    override val combinedScreenSizeObs: Observable<Pair<Int, Int>>
        get() = BehaviorSubject(Pair(800, 600)).asObservable()

    override fun updateMonitorList() {
        functionCalls.add(FunctionCall("updateMonitorList"))
    }

    override fun toggleScreenMode() {
        functionCalls.add(FunctionCall("toggleScreenMode"))
    }

    override fun toggleFramedScreenMode() {
        functionCalls.add(FunctionCall("toggleFramedScreenMode"))
    }
}