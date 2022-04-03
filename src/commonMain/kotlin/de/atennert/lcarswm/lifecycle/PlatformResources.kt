package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.command.Commander
import de.atennert.lcarswm.environment.Environment
import de.atennert.lcarswm.file.FileFactory
import de.atennert.lcarswm.file.Files

data class PlatformResources(
    val commander: Commander,
    val fileFactory: FileFactory,
    val files: Files,
    val environment: Environment
)
