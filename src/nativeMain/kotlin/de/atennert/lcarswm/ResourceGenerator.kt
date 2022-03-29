package de.atennert.lcarswm

import de.atennert.lcarswm.environment.Environment

interface ResourceGenerator {
    fun createEnvironment(): Environment
}