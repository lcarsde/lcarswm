package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import kotlin.test.*

/**
 * Tests for Monitor data container
 */
class MonitorTest {
    @Test
    fun `verify that monitors with the same ID are equal`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 123.toULong(), "name1", false)
        val monitor2 = Monitor(monitorManager, 123.toULong(), "name2", false)

        assertEquals(monitor1, monitor2)
    }

    @Test
    fun `verify that monitors with different IDs are not equal`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.toULong(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.toULong(), "name", false)

        assertNotEquals(monitor1, monitor2)
    }

    @Test
    fun `return false when monitors have the same measurements`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.toULong(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.toULong(), "name", false)

        monitor1.setMonitorMeasurements(0, 0, 3.toUInt(), 4.toUInt())
        monitor2.setMonitorMeasurements(0, 0, 3.toUInt(), 4.toUInt())

        assertFalse(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `return true when monitors have different measurements`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.toULong(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.toULong(), "name", false)

        monitor1.setMonitorMeasurements(0, 0, 3.toUInt(), 4.toUInt())
        monitor2.setMonitorMeasurements(1, 2, 3.toUInt(), 4.toUInt())

        assertTrue(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `monitors are clones when they have the same position`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.toULong(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.toULong(), "name", false)

        monitor1.setMonitorMeasurements(0, 0, 1.toUInt(), 2.toUInt())
        monitor2.setMonitorMeasurements(0, 0, 3.toUInt(), 4.toUInt())

        assertTrue(monitor1.isClone(monitor2))
    }

    @Test
    fun `monitors are not clones when they have the different positions`() {
        val monitorManager = MonitorManagerMock()
        val monitor1 = Monitor(monitorManager, 1.toULong(), "name", false)
        val monitor2 = Monitor(monitorManager, 2.toULong(), "name", false)

        monitor1.setMonitorMeasurements(1, 2, 1.toUInt(), 2.toUInt())
        monitor2.setMonitorMeasurements(0, 0, 3.toUInt(), 4.toUInt())

        assertFalse(monitor1.isClone(monitor2))
    }

    @Test
    fun `setting measurements twice throws exception`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 9.toULong(), "name", false)
        monitor.setMonitorMeasurements(0, 1, 2.toUInt(), 3.toUInt())

        assertFailsWith(IllegalStateException::class)
        { monitor.setMonitorMeasurements(1, 2, 3.toUInt(), 4.toUInt()) }
    }

    @Test
    fun `verify that primary monitors return the correct normal screen mode`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 1.toULong(), "name", true)

        assertEquals(ScreenMode.NORMAL, monitor.getScreenMode(), "The primary monitor should return the normal mode when the monitor manager defines normal mode")
    }

    @Test
    fun `verify calculation of default window measurements`() {
        val monitorManager = MonitorManagerMock()
        val monitor = Monitor(monitorManager, 1.toULong(), "name", true)
        monitor.setMonitorMeasurements(0, 0, 800.toUInt(), 600.toUInt())

        val defaultMeasurements = monitor.getWindowMeasurements()

        assertEquals(listOf(208, 242, 552, 292), defaultMeasurements)
    }

    @Test
    fun `verify calculation of maximized window measurements`() {
        val monitorManager = object : MonitorManagerMock() {
            override fun getScreenMode(): ScreenMode = ScreenMode.MAXIMIZED
        }
        val monitor = Monitor(monitorManager, 1.toULong(), "name", false)
        monitor.setMonitorMeasurements(0, 0, 800.toUInt(), 600.toUInt())

        val defaultMeasurements = monitor.getWindowMeasurements()

        assertEquals(listOf(40, 48, 720, 504), defaultMeasurements)
    }

    @Test
    fun `verify calculation of full window measurements`() {
        val monitorManager = object : MonitorManagerMock() {
            override fun getScreenMode(): ScreenMode = ScreenMode.FULLSCREEN
        }
        val monitor = Monitor(monitorManager, 1.toULong(), "name", false)
        monitor.setMonitorMeasurements(0, 0, 800.toUInt(), 600.toUInt())

        val defaultMeasurements = monitor.getWindowMeasurements()

        assertEquals(listOf(0, 0, 800, 600), defaultMeasurements)
    }
}