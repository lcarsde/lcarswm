package de.atennert.lcarswm.conversion

import kotlinx.cinterop.convert

/** Convert ULong value to byte array as used in X properties */
fun ULong.toUByteArray(): UByteArray {
    return ubyteArrayOf(
        (this and 0xFF.convert()).convert(),
        (this.shr(8) and 0xFF.convert()).convert(),
        (this.shr(16) and 0xFF.convert()).convert(),
        (this.shr(24) and 0xFF.convert()).convert()
    )
}

/** Convert string to byte array as used in X properties */
fun String.toUByteArray(): UByteArray {
    return this.encodeToByteArray().asUByteArray()
}

/** Concatenate UByte arrays to a single UByte array */
fun List<UByteArray>.combine(): UByteArray {
    val byteList = this.fold(mutableListOf<UByte>()) { list, bytes -> list.addAll(bytes); list }
    return byteList.toUByteArray()
}
