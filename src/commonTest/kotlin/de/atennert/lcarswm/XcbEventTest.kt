package de.atennert.lcarswm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class XcbEventTest {
    @Test
    fun `get event type for code value`() {
        XcbEvent.values()
            .forEach { assertEquals(it, XcbEvent.getEventTypeForCode(it.code)) }
    }

    @Test
    fun `throw error on unknown code value`() {
        assertFailsWith<IllegalArgumentException> { XcbEvent.getEventTypeForCode(-1) }
    }
}