package de.atennert.lcarswm.atom

import de.atennert.lcarswm.system.api.WindowUtilApi
import xlib.Atom

/**
 * This class is supposed to hold the required X atoms and provide them to other classes.
 */
class AtomLibrary(windowUtils: WindowUtilApi) {

    private val atomMap: Map<Atoms, Atom> = Atoms.values().asList().associateWith { windowUtils.internAtom(it.atomName) }

    operator fun get(atom: Atoms): Atom = atomMap.getValue(atom)
}