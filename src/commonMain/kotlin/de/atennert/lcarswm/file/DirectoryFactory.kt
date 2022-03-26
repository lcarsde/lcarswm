package de.atennert.lcarswm.file

/**
 * Used to get instances of Directory.
 */
interface DirectoryFactory {
    fun getDirectory(path: String): Directory?
}