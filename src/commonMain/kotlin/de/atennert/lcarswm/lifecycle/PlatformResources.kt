package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.command.Commander
import de.atennert.lcarswm.file.DirectoryFactory

data class PlatformResources(val commander: Commander, val dirFactory: DirectoryFactory)
