package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.AUTOSTART_FILE
import de.atennert.lcarswm.HOME_CONFIG_DIR_PROPERTY
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
 */
private fun getAutostartFile(posixApi: PosixApi): String {
    val userConfigPath = posixApi.getenv(HOME_CONFIG_DIR_PROPERTY)
    val userAutostartPath = userConfigPath?.toKString()?.let { "$it$AUTOSTART_FILE" }

    return if (userAutostartPath != null && fileExist(posixApi, userAutostartPath)) {
        userAutostartPath
    } else {
        // Fallback to autostart file in etc
        "/etc$AUTOSTART_FILE"
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
 * Start all the apps / run the commands from the users or default autostart file.
 */
fun runAutostartApps(posixApi: PosixApi) {
    val autostartPath = getAutostartFile(posixApi)

    FileReader(posixApi, autostartPath).readLines { runCommand(posixApi, it) }
}