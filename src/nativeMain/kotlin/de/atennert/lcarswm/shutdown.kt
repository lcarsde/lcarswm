package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.SystemApi
import staticLogger


/**
 * Full shutdown routines.
 */
fun shutdown(system: SystemApi) {
    system.sync(false)

    staticLogger = null

    closeClosables()
}
