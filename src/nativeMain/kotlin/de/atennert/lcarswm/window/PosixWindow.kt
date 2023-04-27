package de.atennert.lcarswm.window

import de.atennert.lcarswm.BAR_HEIGHT_WITH_OFFSET
import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.atom.NumberAtomReader
import de.atennert.lcarswm.atom.TextAtomReader
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.drawing.IFrameDrawer
import de.atennert.lcarswm.events.sendConfigureNotify
import de.atennert.lcarswm.keys.KeyManager
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.*
import kotlinx.cinterop.*
import xlib.*

private const val frameEventMask = SubstructureRedirectMask or FocusChangeMask or ButtonPressMask or ButtonReleaseMask

private val buttonsToGrab = setOf(Button1, Button2, Button3)

private val wmStateData = listOf<ULong>(NormalState.convert(), None.convert())
    .map { it.toUByteArray() }
    .combine()

class PosixWindow(
    private val logger: Logger,
    private val display: CPointer<Display>?,
    private val screen: Screen,
    private val atomLibrary: AtomLibrary,
    private val textAtomReader: TextAtomReader,
    private val numberAtomReader: NumberAtomReader,
    private val frameDrawer: IFrameDrawer,
    private val keyManager: KeyManager,
    override val id: Window,
    private val borderWidth: Int
) : ManagedWmWindow<Window> {
    override var frame: Window = 0.convert()
        private set

    override var titleBar: Window = 0.convert()
        private set

    override var title: String = TextAtomReader.NO_NAME
        private set

    override var wmClass: String = TextAtomReader.NO_NAME

    var isTransient: Boolean = false
        private set
    var transientFor: Window? = null
        private set

    var type: WindowType = WindowType.NORMAL
        private set

    private lateinit var measurements: WindowMeasurements
    private lateinit var screenMode: ScreenMode
    private var isFocused = true

    init {
        wmClass = textAtomReader.readTextProperty(id, Atoms.WM_CLASS)

        determineTransienceAndType()

        updateTitle()
    }

    private fun determineTransienceAndType() {
        val transientWindow = nativeHeap.alloc(None.toULong())
        if (wrapXGetTransientForHint(display, id, transientWindow.ptr) != 0) {
            isTransient = true
            if (transientWindow.value != screen.root && type != WindowType.DOCK) {
                transientFor = transientWindow.value
            }
        }
        nativeHeap.free(transientWindow)

        val windowTypeList = WindowType.values()
            .map { Pair(it, atomLibrary[WINDOW_TYPE_ATOM_MAP.getValue(it)]) }

        val windowTypeProperties =
            numberAtomReader.readULongArrayPropertyOrNull(id, Atoms.NET_WM_WINDOW_TYPE, Atoms.ATOM)
        type = windowTypeProperties?.firstNotNullOfOrNull { propertyAtom ->
            windowTypeList.firstOrNull { it.second == propertyAtom }
        }?.first ?: if (isTransient) WindowType.DIALOG else WindowType.NORMAL

        isTransient = isTransient || TRANSIENT_WINDOW_TYPES.contains(type)
    }

    override fun open(measurements: WindowMeasurements, screenMode: ScreenMode) {
        this.measurements = measurements
        this.screenMode = screenMode

        frame = wrapXCreateSimpleWindow(
            display,
            screen.root,
            measurements.x,
            measurements.y,
            measurements.width.convert(),
            measurements.frameHeight.convert(),
            0.convert(),
            0.convert(),
            0.convert(),
        )

        titleBar = wrapXCreateSimpleWindow(
            display,
            frame,
            0,
            measurements.frameHeight - BAR_HEIGHT_WITH_OFFSET,
            measurements.width.convert(),
            BAR_HEIGHT_WITH_OFFSET.convert(),
            0.convert(),
            0.convert(),
            0.convert(),
        )

        logger.logDebug("PosixWindow::open::reparenting $id ($title) to $frame")

        wrapXSelectInput(display, frame, frameEventMask)

        wrapXAddToSaveSet(display, id)

        wrapXSetWindowBorderWidth(display, id, 0.convert())

        wrapXReparentWindow(display, id, frame, 0, 0)

        buttonsToGrab.forEach { button ->
            keyManager.grabButton(
                button.convert(),
                AnyModifier,
                id,
                ButtonPressMask.convert(),
                GrabModeSync,
                None.convert()
            )
        }
        buttonsToGrab.forEach { button ->
            keyManager.grabButton(
                button.convert(),
                AnyModifier,
                titleBar,
                (ButtonPressMask or ButtonReleaseMask or ButtonMotionMask).convert(),
                GrabModeAsync,
                None.convert()
            )
        }

        wrapXResizeWindow(display, id, measurements.width.convert(), measurements.height.convert())

        wrapXUngrabServer(display)

        wrapXMapWindow(display, frame)

        wrapXMapWindow(display, titleBar)

        wrapXMapWindow(display, id)

        val format = 32
        val bytesPerData = format.div(8)
        val dataCount = wmStateData.size.div(bytesPerData)
        wrapXChangeProperty(
            display,
            id,
            atomLibrary[Atoms.WM_STATE],
            atomLibrary[Atoms.WM_STATE],
            format,
            PropModeReplace,
            wmStateData.toCValues(),
            dataCount
        )

        frameDrawer.drawFrame(measurements, screenMode, isFocused, title, titleBar)
    }

    override fun show() {
        TODO("Not yet implemented")
    }

    override fun moveResize(measurements: WindowMeasurements, screenMode: ScreenMode) {
        this.measurements = measurements
        this.screenMode = screenMode

        wrapXMoveResizeWindow(
            display,
            titleBar,
            0,
            measurements.frameHeight - BAR_HEIGHT_WITH_OFFSET,
            measurements.width.convert(),
            BAR_HEIGHT_WITH_OFFSET.convert()
        )

        wrapXMoveResizeWindow(
            display,
            frame,
            measurements.x,
            measurements.y,
            measurements.width.convert(),
            measurements.frameHeight.convert()
        )

        wrapXResizeWindow(
            display,
            id,
            measurements.width.convert(),
            measurements.height.convert()
        )

        sendConfigureNotify(display, id, measurements)

        frameDrawer.drawFrame(measurements, screenMode, isFocused, title, titleBar)
    }

    override fun updateTitle() {
        title = textAtomReader.readTextProperty(id, Atoms.NET_WM_NAME)
        if (title == TextAtomReader.NO_NAME) {
            title = textAtomReader.readTextProperty(id, Atoms.WM_NAME)
        }
        frameDrawer.drawFrame(measurements, screenMode, isFocused, title, titleBar)
    }

    override fun focus() {
        isFocused = true
        frameDrawer.drawFrame(measurements, screenMode, isFocused, title, titleBar)
    }

    override fun unfocus() {
        isFocused = false
        frameDrawer.drawFrame(measurements, screenMode, isFocused, title, titleBar)
    }

    override fun hide() {
        TODO("Not yet implemented")
    }

    override fun close() {
        logger.logDebug("WindowHandler::removeWindow::remove window $id")

        wrapXSelectInput(display, id, NoEventMask)
        buttonsToGrab.forEach {
            keyManager.ungrabButton(it.convert(), AnyModifier, frame)
            keyManager.ungrabButton(it.convert(), AnyModifier, id)
        }

        wrapXUnmapWindow(display, titleBar)
        wrapXUnmapWindow(display, frame)
        wrapXFlush(display)

        wrapXSetWindowBorderWidth(display, id, borderWidth.convert())

        wrapXReparentWindow(display, id, screen.root, 0, 0)
        wrapXRemoveFromSaveSet(display, id)
        wrapXDestroyWindow(display, titleBar)
        wrapXDestroyWindow(display, frame)
    }
}

