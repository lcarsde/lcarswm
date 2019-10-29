package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.WindowUtilApi
import xlib.Atom

/**
 * This class is supposed to hold the required X atoms and provide them to other classes.
 */
class AtomLibrary(windowUtils: WindowUtilApi) {

    private val usedAtoms = listOf(
        "WINDOW",
        "ATOM",
        "UTF8_STRING",

        "WM_DELETE_WINDOW",
        "WM_PROTOCOLS",
        "WM_STATE",

        "_NET_WM_NAME",
        "_NET_SUPPORTED",
        "_NET_SUPPORTING_WM_CHECK"
    )

    private val atomMap = usedAtoms.associateWith { windowUtils.internAtom(it) }

    operator fun get(atomName: String): Atom =
        atomMap[atomName] ?: throw IllegalArgumentException("AtomLibrary::Unknown atom $atomName")
}