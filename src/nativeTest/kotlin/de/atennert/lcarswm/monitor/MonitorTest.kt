package de.atennert.lcarswm.monitor

import de.atennert.lcarswm.ScreenMode
import kotlin.test.*

/**
 * Tests for Monitor data container
 */
class MonitorTest {
    @Test
    fun `verify that monitors with the same ID are equal`() {
        val monitor1 = Monitor(123.toULong(), "name1", false)
        val monitor2 = Monitor(123.toULong(), "name2", false)

        assertEquals(monitor1, monitor2)
    }

    @Test
    fun `verify that monitors with different IDs are not equal`() {
        val monitor1 = Monitor(1.toULong(), "name", false)
        val monitor2 = Monitor(2.toULong(), "name", false)

        assertNotEquals(monitor1, monitor2)
    }

    @Test
    fun `return false when monitors have the same measurements`() {
        val monitor1 = Monitor(1.toULong(), "name", false)
        val monitor2 = Monitor(2.toULong(), "name", false)

        monitor1.setMeasurements(0, 0, 3.toUInt(), 4.toUInt())
        monitor2.setMeasurements(0, 0, 3.toUInt(), 4.toUInt())

        assertFalse(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `return true when monitors have different measurements`() {
        val monitor1 = Monitor(1.toULong(), "name", false)
        val monitor2 = Monitor(2.toULong(), "name", false)

        monitor1.setMeasurements(0, 0, 3.toUInt(), 4.toUInt())
        monitor2.setMeasurements(1, 2, 3.toUInt(), 4.toUInt())

        assertTrue(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `monitors are clones when they have the same position`() {
        val monitor1 = Monitor(1.toULong(), "name", false)
        val monitor2 = Monitor(2.toULong(), "name", false)

        monitor1.setMeasurements(0, 0, 1.toUInt(), 2.toUInt())
        monitor2.setMeasurements(0, 0, 3.toUInt(), 4.toUInt())

        assertTrue(monitor1.isClone(monitor2))
    }

    @Test
    fun `monitors are not clones when they have the different positions`() {
        val monitor1 = Monitor(1.toULong(), "name", false)
        val monitor2 = Monitor(2.toULong(), "name", false)

        monitor1.setMeasurements(1, 2, 1.toUInt(), 2.toUInt())
        monitor2.setMeasurements(0, 0, 3.toUInt(), 4.toUInt())

        assertFalse(monitor1.isClone(monitor2))
    }

    @Test
    fun `setting measurements twice throws exception`() {
        val monitor = Monitor(9.toULong(), "name", false)
        monitor.setMeasurements(0, 1, 2.toUInt(), 3.toUInt())

        assertFailsWith(IllegalStateException::class)
        { monitor.setMeasurements(1, 2, 3.toUInt(), 4.toUInt()) }
    }

    @Test
    fun `verify calculation of default window measurements`() {
        val monitor = Monitor(1.toULong(), "name", false)
        monitor.setMeasurements(0, 0, 800.toUInt(), 600.toUInt())

        val defaultMeasurements = monitor.getCurrentWindowMeasurements(ScreenMode.NORMAL)

        assertEquals(listOf(208, 242, 552, 292), defaultMeasurements)
    }

    @Test
    fun `verify calculation of maximized window measurements`() {
        val monitor = Monitor(1.toULong(), "name", false)
        monitor.setMeasurements(0, 0, 800.toUInt(), 600.toUInt())

        val defaultMeasurements = monitor.getCurrentWindowMeasurements(ScreenMode.MAXIMIZED)

        assertEquals(listOf(40, 48, 720, 504), defaultMeasurements)
    }

    @Test
    fun `verify calculation of full window measurements`() {
        val monitor = Monitor(1.toULong(), "name", false)
        monitor.setMeasurements(0, 0, 800.toUInt(), 600.toUInt())

        val defaultMeasurements = monitor.getCurrentWindowMeasurements(ScreenMode.FULLSCREEN)

        assertEquals(listOf(0, 0, 800, 600), defaultMeasurements)
    }
}