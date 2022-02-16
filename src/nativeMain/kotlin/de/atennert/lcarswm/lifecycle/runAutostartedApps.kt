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

// TODO run apps in ~/.config/autostart
// TODO run apps in /etc/xdg/autostart
// don't run doubles from local autostart
// check for Hidden=true
// check OnlyShownIn
// check NotShowIn

/**
 * Start all the apps / run the commands from the users or default autostart file.
 */
fun runAutostartApps(posixApi: PosixApi) {
    getAutostartFile(posixApi)?.let { path ->
        FileReader(posixApi, path).readLines { runCommand(posixApi, it) }
    }
}