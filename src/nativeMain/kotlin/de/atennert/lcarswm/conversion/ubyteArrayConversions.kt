package de.atennert.lcarswm.conversion

import kotlinx.cinterop.*

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

/** convert this ubyte array pointer to a string */
fun CPointer<UByteVar>.toKString(): String {
    val byteString = mutableListOf<Byte>()
    var i = 0

    while (true) {
        val value = this[i]
        if (value.convert<Int>() == 0) {
            break
        }

        byteString.add(value.convert())
        i++
    }
    return byteString.toByteArray().toKString()
}

/** convert this ubyte array pointer to a string */
fun CPointer<UByteVar>?.toKString(): String = this?.toKString() ?: ""
