package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.AUTOSTART_FILE
import de.atennert.lcarswm.HOME_CONFIG_DIR_PROPERTY
import de.atennert.lcarswm.file.DirectoryFactory
import de.atennert.lcarswm.runProgram
import de.atennert.lcarswm.settings.FileReader
import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.toKString
import platform.posix.F_OK

/**
 * Check whether a given file exists.
 */
private fun fileExist(posixApi: PosixApi, filePath: String): Boolean {
    return posixApi.access(filePath, F_OK) != -1
}

/**
 * Get the users (if available) or default autostart file path.
 * @deprecated autostart files should not be used anymore but stay for compatibility
 */
private fun getAutostartFile(posixApi: PosixApi): String? {
    val userConfigPath = posixApi.getenv(HOME_CONFIG_DIR_PROPERTY)
    val userAutostartPath = userConfigPath?.toKString()?.let { "$it$AUTOSTART_FILE" }

    return when {
        userAutostartPath != null && fileExist(posixApi, userAutostartPath) ->
            userAutostartPath
        fileExist(posixApi, "/etc$AUTOSTART_FILE") ->
            "/etc$AUTOSTART_FILE"
        else -> null
    }
}

/**
 * Run a command from the autostart file.
 */
private fun runCommand(posixApi: PosixApi, command: String) {
    val commandParts = command.split(' ')
    runProgram(posixApi, commandParts[0], commandParts)
}

/**
 * Read the *.desktop file names from the directory.
 */
fun readDesktopFiles(directoryPath: String, dirFactory: DirectoryFactory): List<String> {
    val directory = dirFactory.getDirectory(directoryPath)
        ?: return emptyList()

    val files = directory.readFiles()
        .filter { file -> file.endsWith(".desktop") }

    directory.close()
    return files
}

/**
 * Read the data from *.desktop file, check if we can/should autostart it and do so.
 */
fun Iterable<String>.checkAndExecute(posixApi: PosixApi) {
    this.map { path ->
        Autostart().apply { FileReader(posixApi, path).readLines { line -> this.readLine(line) } }
    }
        .filterNot { it.hidden || it.excludeByShow }
        .forEach { it.exec?.let { exec -> runCommand(posixApi, exec) } }
}

/**
 * Encapsulates the data from an autostart desktop file.
 */
private class Autostart {
    var hidden = false
        private set
    var excludeByShow = false
        private set
    var exec: String? = null
        private set

    /**
     * Evaluate a line from an autostart desktop file.
     */
    fun readLine(line: String) {
        val (key, value) = line.split('=')
        when (key.trim().lowercase()) {
            "hidden" -> hidden = value.trim().lowercase() == "true"
            "onlyshowin" -> excludeByShow = !value.lowercase().contains("lcarsde")
            "notshowin" -> excludeByShow = value.lowercase().contains("lcarsde")
            "exec" -> exec = value.trim()
            else -> { /* nothing to do */}
        }
    }
}

/**
 * Start all the apps / run the commands from the users or default autostart file.
 */
fun runAutostartApps(posixApi: PosixApi, dirFactory: DirectoryFactory) {
    getAutostartFile(posixApi)?.let { path ->
        FileReader(posixApi, path).readLines { runCommand(posixApi, it) }
    }

    var localApps = emptyList<String>()
    val globalAutostart = "/etc/xdg/autostart"
    val localAutostart = posixApi.getenv(HOME_CONFIG_DIR_PROPERTY)
        ?.toKString()
        ?.let { "$it/autostart" }

    localAutostart
        ?.let { readDesktopFiles(it, dirFactory) }
        ?.also { localApps = it }
        ?.map { "$localAutostart/$it" }
        ?.checkAndExecute(posixApi)

    readDesktopFiles(globalAutostart, dirFactory)
        // local definitions override global definitions
        .filterNot { localApps.contains(it) }
        .map { "$globalAutostart/$it" }
        .checkAndExecute(posixApi)
}
