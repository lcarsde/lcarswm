package de.atennert.lcarswm.window

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.system.api.WindowUtilApi
import xlib.Window

class WindowListAtomHandler(val rootWindow: Window, val windowUtilApi: WindowUtilApi, val atomLibrary: AtomLibrary) :
    WindowList.Observer {

    private var windows = listOf<Window>()

    override fun windowAdded(window: FramedWindow) {
        windows = windows.plus(window.id)
        windowUtilApi.changeProperty(
            rootWindow,
            atomLibrary[Atoms.NET_CLIENT_LIST],
            atomLibrary[Atoms.WINDOW],
            windows.toULongArray().toUByteArray(),
            32
        )
    }

    override fun windowRemoved(window: FramedWindow) {
        windows = windows.minus(window.id)
        windowUtilApi.changeProperty(
            rootWindow,
            atomLibrary[Atoms.NET_CLIENT_LIST],
            atomLibrary[Atoms.WINDOW],
            windows.toULongArray().toUByteArray(),
            32
        )
    }

    override fun windowUpdated(window: FramedWindow) {
        // nothing to do
    }
}
