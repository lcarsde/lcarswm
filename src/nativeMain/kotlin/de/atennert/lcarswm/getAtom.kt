package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.*
import xcb.xcb_intern_atom
import xcb.xcb_intern_atom_reply

/**
 * Get an atom by name from the X window management.
 */
fun getAtom(xcbConnection: CPointer<xcb_connection_t>, atomName: String): UInt {
    val atomCookie = xcb_intern_atom(xcbConnection, 0.convert(), atomName.length.convert(), atomName)

    // TODO we should really find another solution than returning 0 ... Maybe fail if any atom is not available?
    val atomReply = xcb_intern_atom_reply(xcbConnection, atomCookie, null) ?: return 0.convert()

    val atom = atomReply.pointed.atom
    nativeHeap.free(atomReply)
    return atom
}
