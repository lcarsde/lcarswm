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
        assertEquals(0x34.toUByte(), uByteArray[1], "The second byte should be 0x34")
        assertEquals(0x56.toUByte(), uByteArray[2], "The third byte should be 0x56")
        assertEquals(0x78.toUByte(), uByteArray[3], "The fourth byte should be 0x78")
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

    @Test
    fun `combine UByteArray list to UByteArray`() {
        val uByteArrayList = listOf(
            ubyteArrayOf(0.convert(), 1.convert()),
            ubyteArrayOf(2.convert(), 3.convert(), 4.convert()))

        val combinedByteArray = uByteArrayList.combine()

        ubyteArrayOf(0.convert(), 1.convert(), 2.convert(), 3.convert(), 4.convert())
            .zip(combinedByteArray)
            .forEach { (expected, actual) -> assertEquals(expected, actual, "The values should be the same") }
    }

    @Test
    fun `combine empty UByteArray list to UByteArray`() {
        val uByteArrayList = listOf<UByteArray>()
        val combinedByteArray = uByteArrayList.combine()

        assertEquals(0, combinedByteArray.size, "The byte array should be empty")
    }
}