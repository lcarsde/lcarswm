package de.atennert.lcarswm

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XcbConfigWindowTest {
    @Test
    fun `uncovered mask returns false`() {
        XcbConfigWindow.values()
            .forEach { assertFalse("${it.name}::${it.mask} in 0?") { it.isInValue(0) } }
    }

    @Test
    fun `covered mask returns true`() {
        XcbConfigWindow.values()
            .forEach { assertTrue("${it.name}::${it.mask} not in 0xFF?") { it.isInValue(0xFF) } }
    }
}