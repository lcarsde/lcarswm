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

    val primaryMonitorSj = BehaviorSubject<NewMonitor<RROutput>?>(
        NewMonitor.Builder<RROutput>(42.convert())
            .setPrimary(true)
            .setName("Prim")
            .setX(0)
            .setY(0)
            .setWidth(800)
            .setHeight(600)
            .setScreenMode(screenMode)
            .build()
    )
    var primaryMonitor by primaryMonitorSj

    val otherMonitorsSj = BehaviorSubject(emptyList<NewMonitor<RROutput>>())
    var otherMonitors by otherMonitorsSj

    override val monitorsObs: Observable<List<NewMonitor<RROutput>>> = primaryMonitorSj
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