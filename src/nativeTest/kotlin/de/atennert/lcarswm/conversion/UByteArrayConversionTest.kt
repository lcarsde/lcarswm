package de.atennert.lcarswm.conversion

import kotlinx.cinterop.convert
import kotlin.test.Test
import kotlin.test.assertEquals

class UByteArrayConversionTest {

    @Test
    fun `convert ULong to UByteArray`() {
        val uLongValue: ULong = 0x12345678.convert()
        val uByteArray = uLongValue.toUByteArray()

        assertEquals(4, uByteArray.size, "A ULong should turn into a UByteArray of size 4")

        assertEquals(0x12.toUByte(), uByteArray[0], "The first byte should be 0x12")
        assertEquals(0x34.toUByte(), uByteArray[1], "The first byte should be 0x34")
        assertEquals(0x56.toUByte(), uByteArray[2], "The first byte should be 0x56")
        assertEquals(0x78.toUByte(), uByteArray[3], "The first byte should be 0x78")
    }

    @Test
    fun `convert String to UByteArray`() {
        val testString = "Hello World"
        val stringAsAscii = arrayOf(72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100).map { it.toUByte() }

        val uByteArray = testString.toUByteArray()

        assertEquals(testString.length, uByteArray.size, "The UByte array should have the same size as the string")

        uByteArray.zip(stringAsAscii)
            .forEach { (expected, actual) -> assertEquals(expected, actual, "The values should be the same") }
    }
}