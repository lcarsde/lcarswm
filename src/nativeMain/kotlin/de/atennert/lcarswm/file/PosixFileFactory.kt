package de.atennert.lcarswm.file

import platform.posix.opendir

/**
 * Returns directories using POSIX.
 */
class PosixFileFactory : FileFactory {
    override fun getDirectory(path: String): Directory? {
        return opendir(path)?.let { PosixDirectory(it) }
    }

    override fun getFile(path: String, accessMode: AccessMode): File {
        return PosixFile(path, accessMode)
    }
}