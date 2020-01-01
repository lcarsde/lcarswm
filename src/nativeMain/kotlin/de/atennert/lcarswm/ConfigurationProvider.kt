package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.PosixApi

class ConfigurationProvider(posixApi: PosixApi, configurationFilePath: String) {

    operator fun get(propertyKey: String): String? {
        return ""
    }
}