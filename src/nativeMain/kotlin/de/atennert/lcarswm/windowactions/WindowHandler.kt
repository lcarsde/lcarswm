package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms.WM_STATE
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
class WindowHandler(
    private val system: SystemApi,
    private val logger: Logger,
    private val windowCoordinator: WindowCoordinator,
    private val focusHandler: WindowFocusHandler,
    private val atomLibrary: AtomLibrary,
    private val rootWindow: Window
) : WindowRegistration {

    private val frameEventMask = SubstructureRedirectMask or FocusChangeMask or EnterWindowMask or
            LeaveWindowMask or ButtonPressMask or ButtonReleaseMask

    private val wmStateData = listOf<ULong>(NormalState.convert(), None.convert())
        .map { it.toUByteArray() }
        .combine()

    private val registeredWindows = mutableMapOf<Window, FramedWindow>()

    override fun addWindow(windowId: Window, isSetup: Boolean) {
        val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
        system.getWindowAttributes(windowId, windowAttributes.ptr)

        if (windowAttributes.override_redirect != X_FALSE ||
            (isSetup && windowAttributes.map_state != IsViewable)) {
            logger.logInfo("WindowRegistration::addWindow::skipping window $windowId")

            if (!isSetup) {
                system.mapWindow(windowId)
            }

            nativeHeap.free(windowAttributes)
            return
        }

        val window = FramedWindow(windowId)

        val measurements = windowCoordinator.addWindowToMonitor(window)

        window.frame = system.createSimpleWindow(rootWindow, measurements)

        system.selectInput(window.frame, frameEventMask)

        system.addToSaveSet(windowId)

        system.reparentWindow(windowId, window.frame, 0, 0)

        system.resizeWindow(window.id, measurements[2].convert(), measurements[3].convert())

        system.mapWindow(window.frame)

        system.mapWindow(window.id)

        system.changeProperty(window.id, atomLibrary[WM_STATE], atomLibrary[WM_STATE], wmStateData, 32)

        registeredWindows[windowId] = window

        focusHandler.setFocusedWindow(windowId)

        nativeHeap.free(windowAttributes)
    }

    override fun isWindowManaged(windowId: Window): Boolean = registeredWindows.containsKey(windowId)

    override fun removeWindow(windowId: Window) {
        val framedWindow = registeredWindows.remove(windowId)!!
        
        system.unmapWindow(framedWindow.frame)
        system.reparentWindow(windowId, rootWindow, 0, 0)
        system.removeFromSaveSet(windowId)
        system.destroyWindow(framedWindow.frame)

        windowCoordinator.removeWindow(framedWindow)
    }
}
