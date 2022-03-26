package de.atennert.lcarswm.file

import platform.posix.opendir

/**
 * Returns directories using POSIX.
 */
class PosixDirectoryFactory : DirectoryFactory {
    override fun getDirectory(path: String): Directory? {
        return opendir(path)?.let { PosixDirectory(it) }
    }
}