package de.atennert.lcarswm.window

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.NumberAtomReader
import de.atennert.lcarswm.atom.TextAtomReader
import de.atennert.lcarswm.drawing.Color
import de.atennert.lcarswm.drawing.ColorFactory
import de.atennert.lcarswm.drawing.FontProvider
import de.atennert.lcarswm.drawing.IFrameDrawer
import de.atennert.lcarswm.events.EventStore
import de.atennert.lcarswm.keys.KeyManager
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.*
import kotlinx.cinterop.*
import xlib.*

private const val clientEventMask = PropertyChangeMask or StructureNotifyMask or ColormapChangeMask
private const val clientNoPropagateMask = ButtonPressMask or ButtonReleaseMask or ButtonMotionMask

class PosixWindowFactory(
    private val logger: Logger,
    private val display: CPointer<Display>?,
    private val screen: Screen,
    private val colorFactory: ColorFactory,
    private val fontProvider: FontProvider,
    private val keyManager: KeyManager,
    private val monitorManager: MonitorManager,
    private val atomLibrary: AtomLibrary,
    private val eventStore: EventStore,
    private val focusHandler: WindowFocusHandler,
    private val windowList: WindowList,
    private val textAtomReader: TextAtomReader,
    private val numberAtomReader: NumberAtomReader,
    private val frameDrawer: IFrameDrawer,
) : WindowFactory<Window> {

    override fun createButton(
        text: String,
        backgroundColor: Color,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        onClick: () -> Unit
    ): PosixButton {
        return PosixButton(
            display, screen, colorFactory, fontProvider, monitorManager, keyManager,
            text, backgroundColor, x, y, width, height, onClick
        )
    }

    override fun createWindow(
        id: Window,
        isSetup: Boolean
    ): WmWindow<Window>? {
        wrapXGrabServer(display)
        val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
        if (wrapXGetWindowAttributes(display, id, windowAttributes.ptr) == False) {
            wrapXUngrabServer(display)
            // not a window or something ...
            return null
        }

        if (windowAttributes.override_redirect != False ||
            (isSetup && windowAttributes.map_state != IsViewable)
        ) {
            logger.logInfo("PosixWindowFactory::createWindow::skipping window $id")
            wrapXUngrabServer(display)

            if (!isSetup) {
                wrapXMapWindow(display, id)
            }

            nativeHeap.free(windowAttributes)
            return null
        }

        val attributeSet = nativeHeap.alloc<XSetWindowAttributes>()
        attributeSet.event_mask = clientEventMask
        attributeSet.do_not_propagate_mask = clientNoPropagateMask
        wrapXChangeWindowAttributes(display, id, (CWEventMask or CWDontPropagate).convert(), attributeSet.ptr)

        val window = when {
            PosixAppMenuWindow.isAppMenu(display, atomLibrary, id) -> PosixAppMenuWindow(
                display,
                screen.root,
                monitorManager,
                eventStore,
                atomLibrary,
                focusHandler,
                windowList,
                id,
                windowAttributes.border_width,
            )

            PosixStatusBarWindow.isStatusBar(display, atomLibrary, id) -> PosixStatusBarWindow(
                display,
                screen.root,
                monitorManager,
                eventStore,
                atomLibrary,
                id,
                windowAttributes.border_width,
            )

            else -> PosixWindow(
                logger,
                display,
                screen,
                atomLibrary,
                textAtomReader,
                numberAtomReader,
                frameDrawer,
                keyManager,
                id,
                windowAttributes.border_width
            )
        }

        nativeHeap.free(windowAttributes)
        return window
    }
}