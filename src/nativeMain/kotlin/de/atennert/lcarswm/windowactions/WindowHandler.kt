package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.BAR_HEIGHT_WITH_OFFSET
import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

/**
 * Manages the all handled client windows.
 */
class WindowHandler(
    private val system: SystemApi,
    private val logger: Logger,
    private val windowCoordinator: WindowCoordinator,
    private val focusHandler: WindowFocusHandler,
    private val atomLibrary: AtomLibrary,
    private val screen: Screen,
    private val windowNameReader: WindowNameReader
) : WindowRegistration {

    private val frameEventMask = SubstructureRedirectMask or FocusChangeMask or
            EnterWindowMask or LeaveWindowMask or
            ButtonPressMask or ButtonReleaseMask

    private val clientEventMask = PropertyChangeMask or StructureNotifyMask or ColormapChangeMask

    private val clientNoPropagateMask = ButtonPressMask or ButtonReleaseMask or ButtonMotionMask

    private val wmStateData = listOf<ULong>(NormalState.convert(), None.convert())
        .map { it.toUByteArray() }
        .combine()

    private val registeredWindows = mutableMapOf<Window, FramedWindow>()

    override fun addWindow(windowId: Window, isSetup: Boolean) {
        system.grabServer()
        val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
        system.getWindowAttributes(windowId, windowAttributes.ptr)

        if (windowAttributes.override_redirect != X_FALSE ||
            (isSetup && windowAttributes.map_state != IsViewable)) {
            logger.logInfo("WindowHandler::addWindow::skipping window $windowId")
            system.ungrabServer()

            if (!isSetup) {
                system.mapWindow(windowId)
            }

            nativeHeap.free(windowAttributes)
            return
        }

        val attributeSet = nativeHeap.alloc<XSetWindowAttributes>()
        attributeSet.event_mask = clientEventMask
        attributeSet.do_not_propagate_mask = clientNoPropagateMask
        system.changeWindowAttributes(windowId, (CWEventMask or CWDontPropagate).convert(), attributeSet.ptr)

        val window = FramedWindow(windowId, windowAttributes.border_width)

        window.name = windowNameReader.getWindowName(windowId)

        val measurements = windowCoordinator.addWindowToMonitor(window)

        window.frame = system.createSimpleWindow(screen.root,
            listOf(measurements.x, measurements.y, measurements.width, measurements.frameHeight))

        window.titleBar = system.createSimpleWindow(window.frame,
            listOf(0, measurements.frameHeight - BAR_HEIGHT_WITH_OFFSET, measurements.width, BAR_HEIGHT_WITH_OFFSET))

        val isAppSelector = isAppSelector(windowId)

        logger.logDebug("WindowHandler::addWindow::reparenting $windowId (${window.name}) to ${window.frame}; isAppSelector: $isAppSelector")

        system.selectInput(window.frame, frameEventMask)

        system.addToSaveSet(windowId)

        system.setWindowBorderWidth(windowId, 0.convert())

        system.reparentWindow(windowId, window.frame, 0, 0)

        system.resizeWindow(window.id, measurements.width.convert(), measurements.height.convert())

        system.ungrabServer()

        system.mapWindow(window.frame)

        system.mapWindow(window.titleBar)

        system.mapWindow(window.id)

        system.changeProperty(window.id, atomLibrary[Atoms.WM_STATE], atomLibrary[Atoms.WM_STATE], wmStateData, 32)

        registeredWindows[windowId] = window

        focusHandler.setFocusedWindow(windowId)

        nativeHeap.free(windowAttributes)
    }

    override fun isWindowManaged(windowId: Window): Boolean = registeredWindows.containsKey(windowId)

    override operator fun get(windowId: Window): FramedWindow? = registeredWindows[windowId]

    override fun isWindowParentedBy(windowId: Window, parentId: Window): Boolean {
        val framedWindow = registeredWindows[windowId] ?: return false
        return framedWindow.frame == parentId
    }

    override fun removeWindow(windowId: Window) {
        val isWindowKnown = isWindowManaged(windowId)
        logger.logDebug("WindowHandler::removeWindow::remove window $windowId ... is known: $isWindowKnown")
        if (!isWindowKnown) {
            return
        }
        val framedWindow = registeredWindows.remove(windowId)!!

        system.selectInput(windowId, NoEventMask)

        system.unmapWindow(framedWindow.titleBar)
        system.unmapWindow(framedWindow.frame)
        system.flush()

        system.setWindowBorderWidth(windowId, framedWindow.borderWidth.convert())

        system.reparentWindow(windowId, screen.root, 0, 0)
        system.removeFromSaveSet(windowId)
        system.destroyWindow(framedWindow.titleBar)
        system.destroyWindow(framedWindow.frame)

        windowCoordinator.removeWindow(framedWindow)
        focusHandler.removeWindow(windowId)
    }

    private fun isAppSelector(windowId: Window): Boolean {
        val textProperty = nativeHeap.alloc<XTextProperty>()
        val result = system.getTextProperty(windowId, textProperty.ptr, atomLibrary[Atoms.LCARSWM_APP_SELECTOR])
        system.free(textProperty.value)
        nativeHeap.free(textProperty)
        return result != 0
    }
}
