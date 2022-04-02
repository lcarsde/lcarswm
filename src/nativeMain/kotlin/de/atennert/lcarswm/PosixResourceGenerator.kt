package de.atennert.lcarswm

import de.atennert.lcarswm.environment.Environment
import de.atennert.lcarswm.environment.PosixEnvironment
import de.atennert.lcarswm.file.Files
import de.atennert.lcarswm.file.PosixFiles

class PosixResourceGenerator : ResourceGenerator {
    override fun createEnvironment(): Environment = PosixEnvironment()

    override fun createFiles(): Files = PosixFiles()
}