package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.BAR_END_WIDTH
import de.atennert.lcarswm.BAR_GAP_SIZE
import de.atennert.lcarswm.SIDE_BAR_WIDTH
import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.window.WindowMeasurements
import de.atennert.rx.BehaviorSubject
import de.atennert.rx.NextObserver
import de.atennert.rx.Observable
import kotlinx.cinterop.convert
import kotlin.test.*

/**
 * Tests for Monitor data container
 */
class MonitorTest {
    @Test
    fun `verify that monitors with the same ID are equal`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 123.convert(), "name1", false)
        val monitor2 = Monitor(monitorManager, 123.convert(), "name2", false)

        assertEquals(monitor1, monitor2)
    }

    @Test
    fun `verify that monitors with different IDs are not equal`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.convert(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.convert(), "name", false)

        assertNotEquals(monitor1, monitor2)
    }

    @Test
    fun `return false when monitors have the same measurements`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.convert(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.convert(), "name", false)

        monitor1.setMonitorMeasurements(0, 0, 3.convert(), 4.convert())
        monitor2.setMonitorMeasurements(0, 0, 3.convert(), 4.convert())

        assertFalse(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `return true when monitors have different measurements`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.convert(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.convert(), "name", false)

        monitor1.setMonitorMeasurements(0, 0, 3.convert(), 4.convert())
        monitor2.setMonitorMeasurements(1, 2, 3.convert(), 4.convert())

        assertTrue(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `monitors are clones when they have the same position`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.convert(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.convert(), "name", false)

        monitor1.setMonitorMeasurements(0, 0, 1.convert(), 2.convert())
        monitor2.setMonitorMeasurements(0, 0, 3.convert(), 4.convert())

        assertTrue(monitor1.isClone(monitor2))
    }

    @Test
    fun `monitors are not clones when they have the different positions`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.convert(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.convert(), "name", false)

        monitor1.setMonitorMeasurements(1, 2, 1.convert(), 2.convert())
        monitor2.setMonitorMeasurements(0, 0, 3.convert(), 4.convert())

        assertFalse(monitor1.isClone(monitor2))
    }

    @Test
    fun `setting measurements twice throws exception`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 9.convert(), "name", false)
        monitor.setMonitorMeasurements(0, 1, 2.convert(), 3.convert())

        assertFailsWith(IllegalStateException::class)
        { monitor.setMonitorMeasurements(1, 2, 3.convert(), 4.convert()) }
    }

    @Test
    fun `verify that primary monitors return the correct normal screen mode`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 1.convert(), "name", true)

        val screenModes = mutableListOf<ScreenMode>()
        val sub = monitor.screenModeObs.subscribe(NextObserver(screenModes::add))

        assertContentEquals(listOf(ScreenMode.NORMAL), screenModes, "The primary monitor should return the normal mode when the monitor manager defines normal mode")

        sub.unsubscribe()
    }

    @Test
    fun `verify that non-primary monitors return the maximized on normal screen mode`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)

        val screenModes = mutableListOf<ScreenMode>()
        val sub = monitor.screenModeObs.subscribe(NextObserver(screenModes::add))

        assertEquals(listOf(ScreenMode.MAXIMIZED), screenModes, "A non-primary monitor should return the maximized mode when the monitor manager defines normal mode")

        sub.unsubscribe()
    }

    @Test
    fun `verify calculation of default window measurements`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 1.convert(), "name", true)
        monitor.setMonitorMeasurements(0, 0, 800.convert(), 600.convert())

        val measurements = mutableListOf<WindowMeasurements>()
        val sub = monitor.windowMeasurementsObs.subscribe(NextObserver(measurements::add))

        val x = SIDE_BAR_WIDTH + BAR_GAP_SIZE + BAR_END_WIDTH + BAR_GAP_SIZE
        val width = monitor.width - x
        assertEquals(WindowMeasurements(x, 240, width, 312, 360), measurements.last())

        sub.unsubscribe()
    }

    @Test
    fun `verify calculation of maximized window measurements`() {
        val monitorManager = object : MonitorManagerMock() {
            override val screenModeObs: Observable<ScreenMode>
                get() = BehaviorSubject(ScreenMode.MAXIMIZED).asObservable()
        }
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)
        monitor.setMonitorMeasurements(0, 0, 800.convert(), 600.convert())

        val measurements = mutableListOf<WindowMeasurements>()
        val sub = monitor.windowMeasurementsObs.subscribe(NextObserver(measurements::add))

        assertEquals(WindowMeasurements(0, 48, 800, 504, 552), measurements.last())

        sub.unsubscribe()
    }

    @Test
    fun `verify calculation of full window measurements`() {
        val monitorManager = object : MonitorManagerMock() {
            override val screenModeObs: Observable<ScreenMode>
                get() = BehaviorSubject(ScreenMode.FULLSCREEN).asObservable()
        }
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)
        monitor.setMonitorMeasurements(0, 0, 800.convert(), 600.convert())

        val measurements = mutableListOf<WindowMeasurements>()
        val sub = monitor.windowMeasurementsObs.subscribe(NextObserver(measurements::add))

        assertEquals(WindowMeasurements(0, 0, 800, 600, 600), measurements.last())

        sub.unsubscribe()
    }

    @Test
    fun `verify on single monitor`() {
        val monitorManager = object : MonitorManagerMock() {
            override val screenModeObs: Observable<ScreenMode>
                get() = BehaviorSubject(ScreenMode.FULLSCREEN).asObservable()
        }
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)
        monitor.setMonitorMeasurements(0, 0, 800.convert(), 600.convert())

        assertTrue(monitor.isOnMonitor(0, 0), "upper left corner")
        assertTrue(monitor.isOnMonitor(799, 0), "upper right corner")
        assertTrue(monitor.isOnMonitor(0, 599), "lower left corner")
        assertTrue(monitor.isOnMonitor(799, 599), "lower right corner")
        assertTrue(monitor.isOnMonitor(400, 300), "middle")
        assertFalse(monitor.isOnMonitor(400, -1), "outer up")
        assertFalse(monitor.isOnMonitor(800, 300), "outer right")
        assertFalse(monitor.isOnMonitor(400, 600), "outer down")
        assertFalse(monitor.isOnMonitor(-1, 300), "outer left")
    }

    @Test
    fun `verify on offset monitor`() {
        val monitorManager = object : MonitorManagerMock() {
            override val screenModeObs: Observable<ScreenMode>
                get() = BehaviorSubject(ScreenMode.FULLSCREEN).asObservable()
        }
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)
        monitor.setMonitorMeasurements(800, 600, 800.convert(), 600.convert())

        assertTrue(monitor.isOnMonitor(800, 600), "upper left corner")
        assertTrue(monitor.isOnMonitor(1599, 600), "upper right corner")
        assertTrue(monitor.isOnMonitor(800, 1199), "lower left corner")
        assertTrue(monitor.isOnMonitor(1599, 1199), "lower right corner")
        assertTrue(monitor.isOnMonitor(1200, 900), "middle")
        assertFalse(monitor.isOnMonitor(1200, 599), "outer up")
        assertFalse(monitor.isOnMonitor(1600, 900), "outer right")
        assertFalse(monitor.isOnMonitor(1200, 1200), "outer down")
        assertFalse(monitor.isOnMonitor(799, 900), "outer left")
    }
}