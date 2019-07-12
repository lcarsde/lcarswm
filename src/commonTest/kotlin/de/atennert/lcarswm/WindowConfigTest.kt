package de.atennert.lcarswm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *
 */
class WindowConfigTest {
    @Test
    fun `get all configured values with valid mask`() {
        val wc = WindowConfig(1, 2, 3, 4, 5, 6)

        val (_, valueList) = configureWindow(0xFF, wc)

        listOf<Long>(1, 2, 3, 4, 5, 6).forEach { assertTrue("unable to find $it") { valueList.contains(it) } }
    }

    @Test
    fun `get no values with empty mask`() {
        val wc = WindowConfig(1, 2, 3, 4, 5, 6)

        val (_, valueList) = configureWindow(0, wc)

        assertTrue("The list should be empty but wasn't") { valueList.isEmpty() }
    }

    @Test
    fun `get the right values for config keys`() {
        val wc = WindowConfig(
            XcbConfigWindow.X.mask.toLong(),
            XcbConfigWindow.Y.mask.toLong(),
            XcbConfigWindow.WIDTH.mask.toLong(),
            XcbConfigWindow.HEIGHT.mask.toLong(),
            XcbConfigWindow.STACK_MODE.mask.toLong(),
            XcbConfigWindow.SIBLING.mask.toLong()
        )

        XcbConfigWindow.values()
            .forEach { assertEquals(it.mask.toLong(), wc.getValue(it), "${it.mask} != ${wc.getValue(it)}") }
    }
}