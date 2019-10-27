package de.atennert.lcarswm.conversion

import kotlinx.cinterop.convert

fun ULong.toUByteArray(): UByteArray {
    return ubyteArrayOf((this.shr(24) and 0xFF.convert()).convert(),
        (this.shr(16) and 0xFF.convert()).convert(),
        (this.shr(8) and 0xFF.convert()).convert(),
        (this and 0xFF.convert()).convert())
}

fun String.toUByteArray(): UByteArray {
    return this.encodeToByteArray().asUByteArray()
}
