package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.command.Commander
import de.atennert.lcarswm.environment.Environment
import de.atennert.lcarswm.file.DirectoryFactory
import de.atennert.lcarswm.file.Files

data class PlatformResources(
    val commander: Commander,
    val dirFactory: DirectoryFactory,
    val files: Files,
    val environment: Environment
)
