package de.atennert.lcarswm

import kotlin.test.*

/**
 * Tests for Monitor data container
 */
class MonitorTest {
    @Test
    fun `verify that monitors with the same ID are equal`() {
        val monitor1 = Monitor(123.toUInt(), "name1")
        val monitor2 = Monitor(123.toUInt(), "name2")

        assertEquals(monitor1, monitor2)
    }

    @Test
    fun `verify that monitors with different IDs are not equal`() {
        val monitor1 = Monitor(1.toUInt(), "name")
        val monitor2 = Monitor(2.toUInt(), "name")

        assertNotEquals(monitor1, monitor2)
    }

    @Test
    fun `return false when monitors have the same measurements`() {
        val monitor1 = Monitor(1.toUInt(), "name")
        val monitor2 = Monitor(2.toUInt(), "name")

        monitor1.setMeasurements(0, 0, 3.toUShort(), 4.toUShort())
        monitor2.setMeasurements(0, 0, 3.toUShort(), 4.toUShort())

        assertFalse(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `return true when monitors have different measurements`() {
        val monitor1 = Monitor(1.toUInt(), "name")
        val monitor2 = Monitor(2.toUInt(), "name")

        monitor1.setMeasurements(0, 0, 3.toUShort(), 4.toUShort())
        monitor2.setMeasurements(1, 2, 3.toUShort(), 4.toUShort())

        assertTrue(monitor1.hasDifferentMeasurements(monitor2))
    }

    @Test
    fun `monitors are clones when they have the same position`() {
        val monitor1 = Monitor(1.toUInt(), "name")
        val monitor2 = Monitor(2.toUInt(), "name")

        monitor1.setMeasurements(0, 0, 1.toUShort(), 2.toUShort())
        monitor2.setMeasurements(0, 0, 3.toUShort(), 4.toUShort())

        assertTrue(monitor1.isClone(monitor2))
    }

    @Test
    fun `monitors are not clones when they have the different positions`() {
        val monitor1 = Monitor(1.toUInt(), "name")
        val monitor2 = Monitor(2.toUInt(), "name")

        monitor1.setMeasurements(1, 2, 1.toUShort(), 2.toUShort())
        monitor2.setMeasurements(0, 0, 3.toUShort(), 4.toUShort())

        assertFalse(monitor1.isClone(monitor2))
    }

    @Test
    fun `setting measurements twice throws exception`() {
        val monitor = Monitor(9.toUInt(), "name")
        monitor.setMeasurements(0, 1, 2.toUShort(), 3.toUShort())

        assertFailsWith(IllegalStateException::class)
        { monitor.setMeasurements(1, 2, 3.toUShort(), 4.toUShort()) }
    }
}