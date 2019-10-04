package de.atennert.lcarswm

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.PosixApi

/**
 *
 */

fun loadAppFromKeyBinding(posixApi: PosixApi, logger: Logger, keyBinding: String) {
    val programConfig = readFromConfig(posixApi, KEY_CONFIG_FILE, keyBinding) ?: return
    logger.logInfo("::loadAppFromKeyBinding::loading app for $keyBinding - ${programConfig[0]}")
    runProgram(posixApi, programConfig[0], programConfig)
}
