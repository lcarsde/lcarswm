package de.atennert.lcarswm.events

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.drawing.FrameDrawer
import de.atennert.lcarswm.window.WindowCoordinator
import de.atennert.lcarswm.atom.TextAtomReader
import de.atennert.lcarswm.window.WindowRegistration
import xlib.PropertyNotify
import xlib.Window
import xlib.XEvent
import xlib.XPropertyEvent

class PropertyNotifyHandler(
    private val atomLibrary: AtomLibrary,
    private val windowRegistration: WindowRegistration,
    private val textAtomReader: TextAtomReader,
    private val frameDrawer: FrameDrawer,
    private val windowCoordinator: WindowCoordinator,
    private val rootWindowId: Window
) : XEventHandler {
    override val xEventType = PropertyNotify

    override fun handleEvent(event: XEvent): Boolean {
        val windowId = event.xany.window
        when {
            windowRegistration.isWindowManaged(windowId) -> handleClientMessage(event.xproperty)
            windowId == rootWindowId -> { /* nothing to do yet */ }
            else -> { /* nothing to do */ }
        }
        return false
    }

    private fun handleClientMessage(event: XPropertyEvent) {
        when (event.atom) {
            atomLibrary[Atoms.WM_NAME] -> reloadWindowTitle(event.window)
            atomLibrary[Atoms.NET_WM_NAME] -> reloadWindowTitle(event.window)
        }
    }

    private fun reloadWindowTitle(windowId: Window) {
        val window = windowRegistration[windowId]!! // this is checked above
        window.title = textAtomReader.readTextProperty(windowId, Atoms.NET_WM_NAME)
        if (window.title == TextAtomReader.NO_NAME) {
            window.title = textAtomReader.readTextProperty(windowId, Atoms.WM_NAME)
        }
        frameDrawer.drawFrame(window, windowCoordinator.getMonitorForWindow(windowId))
    }
}