package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
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

        assertEquals(ScreenMode.NORMAL, monitor.getScreenMode(), "The primary monitor should return the normal mode when the monitor manager defines normal mode")
    }

    @Test
    fun `verify that non-primary monitors return the maximized on normal screen mode`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)

        assertEquals(ScreenMode.MAXIMIZED, monitor.getScreenMode(), "A non-primary monitor should return the maximized mode when the monitor manager defines normal mode")
    }

    @Test
    fun `verify calculation of default window measurements`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 1.convert(), "name", true)
        monitor.setMonitorMeasurements(0, 0, 800.convert(), 600.convert())

        val defaultMeasurements = monitor.getWindowMeasurements()

        assertEquals(listOf(208, 242, 552, 292, 356), defaultMeasurements)
    }

    @Test
    fun `verify calculation of maximized window measurements`() {
        val monitorManager = object : MonitorManagerMock() {
            override fun getScreenMode(): ScreenMode = ScreenMode.MAXIMIZED
        }
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)
        monitor.setMonitorMeasurements(0, 0, 800.convert(), 600.convert())

        val defaultMeasurements = monitor.getWindowMeasurements()

        assertEquals(listOf(40, 48, 720, 504, 552), defaultMeasurements)
    }

    @Test
    fun `verify calculation of full window measurements`() {
        val monitorManager = object : MonitorManagerMock() {
            override fun getScreenMode(): ScreenMode = ScreenMode.FULLSCREEN
        }
        val monitor = Monitor(monitorManager, 1.convert(), "name", false)
        monitor.setMonitorMeasurements(0, 0, 800.convert(), 600.convert())

        val defaultMeasurements = monitor.getWindowMeasurements()

        assertEquals(listOf(0, 0, 800, 600, 600), defaultMeasurements)
    }
}