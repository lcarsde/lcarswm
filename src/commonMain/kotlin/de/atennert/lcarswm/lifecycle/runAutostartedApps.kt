package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.AUTOSTART_FILE
import de.atennert.lcarswm.HOME_CONFIG_DIR_PROPERTY
import de.atennert.lcarswm.command.Commander
import de.atennert.lcarswm.environment.Environment
import de.atennert.lcarswm.file.FileFactory
import de.atennert.lcarswm.file.Files
import de.atennert.lcarswm.log.Logger

/**
 * Get the users (if available) or default autostart file path.
 * @deprecated autostart files should not be used anymore but stay for compatibility
 */
private fun getAutostartFile(environment: Environment, files: Files): String? {
    val userConfigPath = environment[HOME_CONFIG_DIR_PROPERTY]
    val userAutostartPath = userConfigPath?.let { "$it$AUTOSTART_FILE" }

    return when {
        userAutostartPath != null && files.exists(userAutostartPath) ->
            userAutostartPath
        files.exists("/etc$AUTOSTART_FILE") ->
            "/etc$AUTOSTART_FILE"
        else -> null
    }
}

/**
 * Read the *.desktop file names from the directory.
 */
private fun readDesktopFiles(directoryPath: String, dirFactory: FileFactory): List<String> {
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
private fun checkAndExecute(path: String, files: Files, commander: Commander, logger: Logger) {
    val autoStart = try {
        Autostart().apply { files.readLines(path) { line -> this.readLine(line) } }
    } catch (e: Error) {
        logger.logWarning("::checkAndExecute::error loading application data: $path", e)
        return
    }

    if (!autoStart.hidden && !autoStart.excludeByShow){
        logger.logDebug("run $path")
        commander.run("/usr/share/lcarsde/tools/launcher.py $path")
    }
}

/**
 * Encapsulates the data from an autostart desktop file.
 */
private class Autostart {
    var hidden = false
        private set
    var excludeByShow = false
        private set

    /**
     * Evaluate a line from an autostart desktop file.
     */
    fun readLine(line: String) {
        if (!line.contains('=')) {
            return
        }
        val (key, value) = line.split('=')
        when (key.trim().lowercase()) {
            "hidden" -> hidden = value.trim().lowercase() == "true"
            "onlyshowin" -> excludeByShow = !value.lowercase().contains("lcarsde")
            "notshowin" -> excludeByShow = value.lowercase().contains("lcarsde")
            else -> { /* nothing to do */}
        }
    }
}

/**
 * Start all the apps / run the commands from the users or default autostart file.
 */
fun runAutostartApps(environment: Environment, dirFactory: FileFactory, commander: Commander, files: Files, logger: Logger) {
    getAutostartFile(environment, files)?.let { path ->
        files.readLines(path) {
            logger.logDebug("starting $it")
            commander.run(it)
        }
    }

    var localApps = emptyList<String>()
    val globalAutostart = "/etc/xdg/autostart"
    val localAutostart = environment[HOME_CONFIG_DIR_PROPERTY]
        ?.let { "$it/autostart" }

    localAutostart
        ?.let { readDesktopFiles(it, dirFactory) }
        ?.also { localApps = it }
        ?.map { "$localAutostart/$it" }
        ?.forEach { checkAndExecute(it, files, commander, logger) }

    readDesktopFiles(globalAutostart, dirFactory)
        // local definitions override global definitions
        .filterNot { localApps.contains(it) }
        .map { "$globalAutostart/$it" }
        .forEach { checkAndExecute(it, files, commander, logger) }
}
