package de.atennert.lcarswm.file

import platform.posix.F_OK
import platform.posix.access

class PosixFiles : Files {
    override fun exists(path: String): Boolean {
        return access(path, F_OK) == 0
    }
}