package de.atennert.lcarswm.window

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.drawing.UIDrawingMock
import de.atennert.lcarswm.events.EventStore
import de.atennert.lcarswm.lifecycle.closeClosables
import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.monitor.MonitorManagerMock
import de.atennert.lcarswm.monitor.NewMonitor
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.SystemCallMocker
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.cinterop.*
import xlib.Display
import xlib.RROutput
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PosixWindowCoordinatorTest : SystemCallMocker() {
    private val eventStore = EventStore()
    private var display: CPointer<Display>? = null

    @BeforeTest
    override fun setup() {
        super.setup()
        display = nativeHeap.allocPointerTo<Display>().value
    }

    @AfterTest
    override fun teardown() {
        closeClosables()
        display?.let(nativeHeap::free)
        super.teardown()
    }

    @Test
    fun `add windows to a monitor`() {
        val monitorManager = MonitorManagerMock()
        val windowFactory = FakeWindowFactory()
        val windowList = WindowList()
        val rootWindowDrawer = UIDrawingMock()

        PosixWindowCoordinator(
            LoggerMock(),
            eventStore,
            monitorManager,
            windowFactory,
            windowList,
            rootWindowDrawer,
            display,
        )

        eventStore.mapSj.next(42.convert())

        val window = windowList.get(42.convert()) as FakeManagedWindow
        val windowMeasurements = WindowMeasurements.createNormal(
            monitorManager.primaryMonitor!!.x,
            monitorManager.primaryMonitor!!.y,
            monitorManager.primaryMonitor!!.width,
            monitorManager.primaryMonitor!!.height,
        )

        window.functionCalls.last().shouldBe(FunctionCall("open", windowMeasurements, ScreenMode.NORMAL))
    }

    @Test
    fun `remove window from coordinator`() {
        val monitorManager = MonitorManagerMock()
        val windowFactory = FakeWindowFactory()
        val windowList = WindowList()
        val rootWindowDrawer = UIDrawingMock()

        PosixWindowCoordinator(
            LoggerMock(),
            eventStore,
            monitorManager,
            windowFactory,
            windowList,
            rootWindowDrawer,
            display,
        )

        eventStore.mapSj.next(42.convert())
        val window = windowList.get(42.convert()) as FakeManagedWindow

        eventStore.unmapSj.next(42.convert())

        windowList.get(42.convert()).shouldBeNull()
        window.functionCalls.last().shouldBe(FunctionCall("close"))
    }

    @Test
    fun `handle monitor updates`() {
        val monitorBuilder = NewMonitor.Builder<RROutput>(42.convert())
            .setName("")
            .setScreenMode(ScreenMode.NORMAL)
            .setX(0)
            .setY(0)
            .setWidth(800)
            .setHeight(600)
            .setPrimary(true)

        val monitorManager = MonitorManagerMock()
        monitorManager.primaryMonitorSj.next(monitorBuilder.build())

        val windowFactory = FakeWindowFactory()
        val windowList = WindowList()
        val rootWindowDrawer = UIDrawingMock()

        PosixWindowCoordinator(
            LoggerMock(),
            eventStore,
            monitorManager,
            windowFactory,
            windowList,
            rootWindowDrawer,
            display,
        )
        eventStore.mapSj.next(42.convert())
        val window = windowList.get(42.convert()) as FakeManagedWindow

        val newMonitor = monitorBuilder.setWidth(1280).setHeight(1024).build()
        monitorManager.primaryMonitor = monitorBuilder.setWidth(1280).setHeight(1024).build()

        val measurments = WindowMeasurements.createNormal(
            newMonitor.x,
            newMonitor.y,
            newMonitor.width,
            newMonitor.height
        )
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.NORMAL))
    }

//    @Test
//    private fun `restack a window to the top`() {
//        memScoped {
//            val memScope = this
//            val configurations = object {
//                val calls = mutableListOf<FunctionCall>()
//                fun configure(display: CValuesRef<Display>?, window: Window, mode: UInt, changes: CValuesRef<XWindowChanges>?): Int {
//                    calls.add(FunctionCall("configure", display, window, mode, changes?.getPointer(memScope)?.pointed?.stack_mode))
//                    return 0
//                }
//            }
//            wrapXConfigureWindow = configurations::configure
//
//            val monitorManager = MonitorManagerMock()
//            val windowFactory = FakeWindowFactory()
//            val windowList = WindowList()
//            val rootWindowDrawer = UIDrawingMock()
//
//            val windowCoordinator = PosixWindowCoordinator(
//                LoggerMock(),
//                eventStore,
//                monitorManager,
//                windowFactory,
//                windowList,
//                rootWindowDrawer,
//                display,
//            )
//
//            eventStore.mapSj.next(42.convert())
//            val window = windowList.get(42.toULong())!!
//
//            windowCoordinator.stackWindowToTheTop(window.id)
//
//            configurations.calls.last().shouldBe(FunctionCall("configure", display, window.frame, CWStackMode.toUInt(), Above))
//        }
//    }

    @Test
    fun `move window to next monitor`() {
        val monitorBuilder = NewMonitor.Builder<RROutput>(42.convert())
            .setName("")
            .setScreenMode(ScreenMode.NORMAL)
            .setX(0)
            .setY(0)
            .setWidth(800)
            .setHeight(600)
            .setPrimary(false)
        val monitorManager = MonitorManagerMock()
        val secondaryMonitor = monitorBuilder.setX(800).build()
        val tertiaryMonitor = monitorBuilder.setY(600).build()
        monitorManager.otherMonitors = listOf(secondaryMonitor, tertiaryMonitor)

        val windowFactory = FakeWindowFactory()
        val windowList = WindowList()
        val rootWindowDrawer = UIDrawingMock()

        val windowCoordinator = PosixWindowCoordinator(
            LoggerMock(),
            eventStore,
            monitorManager,
            windowFactory,
            windowList,
            rootWindowDrawer,
            display,
        )
        eventStore.mapSj.next(42.convert())
        val window = windowList.get(42.convert()) as FakeManagedWindow

        windowCoordinator.moveWindowToNextMonitor(window.id)
        var measurments = WindowMeasurements.createMaximized(
            secondaryMonitor.x,
            secondaryMonitor.y,
            secondaryMonitor.width,
            secondaryMonitor.height
        )
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.MAXIMIZED))

        windowCoordinator.moveWindowToNextMonitor(window.id)
        measurments = WindowMeasurements.createMaximized(
            tertiaryMonitor.x,
            tertiaryMonitor.y,
            tertiaryMonitor.width,
            tertiaryMonitor.height
        )
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.MAXIMIZED))

        windowCoordinator.moveWindowToNextMonitor(window.id)
        measurments = WindowMeasurements.createNormal(
            monitorManager.primaryMonitor!!.x,
            monitorManager.primaryMonitor!!.y,
            monitorManager.primaryMonitor!!.width,
            monitorManager.primaryMonitor!!.height
        )
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.NORMAL))
    }

    @Test
    fun `move window to previous monitor`() {
        val monitorBuilder = NewMonitor.Builder<RROutput>(42.convert())
            .setName("")
            .setScreenMode(ScreenMode.NORMAL)
            .setX(0)
            .setY(0)
            .setWidth(800)
            .setHeight(600)
            .setPrimary(false)
        val monitorManager = MonitorManagerMock()
        val secondaryMonitor = monitorBuilder.setX(800).build()
        val tertiaryMonitor = monitorBuilder.setY(600).build()
        monitorManager.otherMonitors = listOf(secondaryMonitor, tertiaryMonitor)
        println("asd")

        val windowFactory = FakeWindowFactory()
        val windowList = WindowList()
        val rootWindowDrawer = UIDrawingMock()

        val windowCoordinator = PosixWindowCoordinator(
            LoggerMock(),
            eventStore,
            monitorManager,
            windowFactory,
            windowList,
            rootWindowDrawer,
            display,
        )
        eventStore.mapSj.next(42.convert())
        val window = windowList.get(42.convert()) as FakeManagedWindow

        windowCoordinator.moveWindowToPreviousMonitor(window.id)
        var measurments = WindowMeasurements.createMaximized(
            tertiaryMonitor.x,
            tertiaryMonitor.y,
            tertiaryMonitor.width,
            tertiaryMonitor.height
        )
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.MAXIMIZED))

        windowCoordinator.moveWindowToPreviousMonitor(window.id)
        measurments = WindowMeasurements.createMaximized(
            secondaryMonitor.x,
            secondaryMonitor.y,
            secondaryMonitor.width,
            secondaryMonitor.height
        )
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.MAXIMIZED))

        windowCoordinator.moveWindowToPreviousMonitor(window.id)
        measurments = WindowMeasurements.createNormal(
            monitorManager.primaryMonitor!!.x,
            monitorManager.primaryMonitor!!.y,
            monitorManager.primaryMonitor!!.width,
            monitorManager.primaryMonitor!!.height
        )
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.NORMAL))
    }

//    @Test
    fun `test realign windows`() {
        val monitorManager = MonitorManagerMock()
        val windowFactory = FakeWindowFactory()
        val windowList = WindowList()
        val rootWindowDrawer = UIDrawingMock()

        PosixWindowCoordinator(
            LoggerMock(),
            eventStore,
            monitorManager,
            windowFactory,
            windowList,
            rootWindowDrawer,
            display,
        )
        eventStore.mapSj.next(42.convert())
        val window = windowList.get(42.convert()) as FakeManagedWindow

        monitorManager.screenMode = ScreenMode.MAXIMIZED
        var measurments = WindowMeasurements(0, 48, 800, 504, 552)
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.MAXIMIZED))

        monitorManager.screenMode = ScreenMode.FULLSCREEN
        measurments = WindowMeasurements(0, 0, 800, 600, 600)
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.FULLSCREEN))

        monitorManager.screenMode = ScreenMode.NORMAL
        measurments = WindowMeasurements(232, 240, 568, 312, 360)
        window.functionCalls.last().shouldBe(FunctionCall("moveResize", measurments, ScreenMode.NORMAL))
    }
}
