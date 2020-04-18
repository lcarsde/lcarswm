package de.atennert.lcarswm.events

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.drawing.FrameDrawer
import de.atennert.lcarswm.windowactions.WindowCoordinator
import de.atennert.lcarswm.windowactions.WindowNameReader
import de.atennert.lcarswm.windowactions.WindowRegistration
import xlib.PropertyNotify
import xlib.Window
import xlib.XEvent
import xlib.XPropertyEvent

class PropertyNotifyHandler(
    private val atomLibrary: AtomLibrary,
    private val windowRegistration: WindowRegistration,
    private val windowNameReader: WindowNameReader,
    private val frameDrawer: FrameDrawer,
    private val windowCoordinator: WindowCoordinator,
    private val rootWindowId: Window
) : XEventHandler {
    override val xEventType = PropertyNotify

    override fun handleEvent(event: XEvent): Boolean {
        val window = event.xany.window
        when {
            windowRegistration.isWindowManaged(window) -> handleClientMessage(event.xproperty)
            window == rootWindowId -> { /* nothing to do yet */ }
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

    private fun reloadWindowTitle(window: Window) {
        val framedWindow = windowRegistration[window]!! // this is checked above
        framedWindow.name = windowNameReader.getWindowName(window)
        frameDrawer.drawFrame(framedWindow, windowCoordinator.getMonitorForWindow(window))
    }
}