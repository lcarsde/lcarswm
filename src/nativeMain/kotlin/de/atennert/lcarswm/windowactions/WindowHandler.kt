package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms.*
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
    private val rootWindow: Window
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
        val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
        system.getWindowAttributes(windowId, windowAttributes.ptr)

        if (windowAttributes.override_redirect != X_FALSE ||
            (isSetup && windowAttributes.map_state != IsViewable)) {
            logger.logInfo("WindowHandler::addWindow::skipping window $windowId")

            if (!isSetup) {
                system.mapWindow(windowId)
            }

            nativeHeap.free(windowAttributes)
            return
        }

        val window = FramedWindow(windowId, windowAttributes.border_width)

        val measurements = windowCoordinator.addWindowToMonitor(window)

        val attributeSet = nativeHeap.alloc<XSetWindowAttributes>()
        attributeSet.event_mask = clientEventMask
        attributeSet.do_not_propagate_mask = clientNoPropagateMask
        system.changeWindowAttributes(windowId, (CWEventMask or CWDontPropagate).convert(), attributeSet.ptr)

        window.name = getWindowName(windowId)

        window.frame = system.createSimpleWindow(rootWindow, measurements)

        logger.logDebug("WindowHandler::addWindow::reparenting $windowId (${window.name}) to ${window.frame}")

        system.selectInput(window.frame, frameEventMask)

        system.addToSaveSet(windowId)

        system.setWindowBorderWidth(windowId, 0.convert())

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
        
        system.unmapWindow(framedWindow.frame)
        system.flush()

        system.setWindowBorderWidth(windowId, framedWindow.borderWidth.convert())

        system.reparentWindow(windowId, rootWindow, 0, 0)
        system.removeFromSaveSet(windowId)
        system.destroyWindow(framedWindow.frame)

        windowCoordinator.removeWindow(framedWindow)
        focusHandler.removeWindow(windowId)
    }

    private fun getWindowName(windowId: Window): String {
        val textProperty = nativeHeap.alloc<XTextProperty>()
        var result = system.getTextProperty(windowId, textProperty.ptr, atomLibrary[NET_WM_NAME])

        if (result != 0 || textProperty.encoding != atomLibrary[UTF_STRING]) {
            result = system.getTextProperty(windowId, textProperty.ptr, atomLibrary[WM_NAME])
            if (result == 0) {
                return "No name"
            }
        }

        val name = when (textProperty.encoding) {
            atomLibrary[COMPOUND_TEXT] -> {
                val localeString = getByteArrayListFromCompound(textProperty).toKString()
                val readBytes = ULongArray(1).pin()
                val utfBytes = system.localeToUtf8(localeString, (-1).convert(), readBytes.addressOf(0))
                    ?: system.localeToUtf8(localeString, readBytes.get()[0].convert(), null)
                    ?: return "No locale name"
                ByteArray(readBytes.get()[0].convert()) {utfBytes[it]}.toKString()
            }
            atomLibrary[UTF_STRING] -> {
                getByteArray(textProperty).toKString()
            }
            atomLibrary[STRING] -> {
                val latinString = getByteArray(textProperty).takeWhile { c ->
                    // filter forbidden control characters
                    c.toInt() == 9 ||
                            c.toInt() == 10 ||
                            c.toInt() in 33..126 ||
                            c.toInt() > 160
                }.toByteArray()
                    .toKString()
                val readBytes = ULongArray(1).pin()
                val utfBytes = system.convertLatinToUtf8(latinString, (-1).convert(), readBytes.addressOf(0))
                    ?: system.convertLatinToUtf8(latinString, readBytes.get()[0].convert(), null)
                    ?: return "No latin name"
                ByteArray(readBytes.get()[0].convert()) {utfBytes[it]}.toKString()
            }
            else -> ""
        }
        system.free(textProperty.value)
        nativeHeap.free(textProperty)
        return name.toUpperCase()
    }

    private fun getByteArrayListFromCompound(textProperty: XTextProperty): ByteArray {
        val resultList = nativeHeap.allocPointerTo<CPointerVar<ByteVar>>()
        val listCount = IntArray(1).pin()
        system.xmbTextPropertyToTextList(textProperty.ptr, resultList.ptr, listCount.addressOf(0))

        val byteList = mutableListOf<Byte>()
        var index = 0
        var value = resultList.value?.get(0)?.get(0)
        while (value != null && value.toInt() != 0) {
            byteList.add(value)
            index++
            value = resultList.value?.get(0)?.get(index)
        }
        byteList.add(0)
        nativeHeap.free(resultList)
        return byteList.toByteArray()
    }

    private fun getByteArray(textProperty: XTextProperty): ByteArray {
        return UByteArray(textProperty.nitems.convert()) { textProperty.value?.get(it)!! }
            .fold(mutableListOf<Byte>()) { list, ub ->
                list.add(ub.convert())
                if (ub.convert<Int>() == 0) {
                    return@fold list
                }
                list
            }
            .toByteArray()
    }
}
