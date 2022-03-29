package de.atennert.lcarswm

import de.atennert.lcarswm.environment.Environment
import de.atennert.lcarswm.environment.PosixEnvironment

class PosixResourceGenerator : ResourceGenerator {
    override fun createEnvironment(): Environment = PosixEnvironment()
}