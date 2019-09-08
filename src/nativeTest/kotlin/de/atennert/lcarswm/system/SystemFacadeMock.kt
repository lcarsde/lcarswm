package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import platform.posix.FILE
import platform.posix.__pid_t
import xlib.*

class SystemFacadeMock : SystemApi {
    override fun openDisplay(name: String?): CPointer<Display>? = null

    override fun closeDisplay(display: CValuesRef<Display>): Int = 0

    override fun defaultScreenOfDisplay(display: CValuesRef<Display>): CPointer<Screen>? = null

    override fun grabServer(display: CValuesRef<Display>): Int = 0

    override fun ungrabServer(display: CValuesRef<Display>): Int = 0

    override fun addToSaveSet(display: CValuesRef<Display>, window: Window): Int = 0

    override fun removeFromSaveSet(display: CValuesRef<Display>, window: Window): Int = 0

    override fun queryTree(
        display: CValuesRef<Display>,
        window: Window,
        rootReturn: CValuesRef<WindowVar>,
        parentReturn: CValuesRef<WindowVar>,
        childrenReturn: CValuesRef<CPointerVar<WindowVar>>,
        childrenReturnCounts: CValuesRef<UIntVar>
    ): Int = 0

    override fun getWindowAttributes(
        display: CValuesRef<Display>,
        window: Window,
        attributes: CValuesRef<XWindowAttributes>
    ): Int = 0

    override fun getWMProtocols(
        display: CValuesRef<Display>,
        window: Window,
        protocolsReturn: CValuesRef<CPointerVar<AtomVar>>,
        protocolCountReturn: CValuesRef<IntVar>
    ): Int = 0

    override fun setErrorHandler(handler: XErrorHandler): XErrorHandler? = null

    override fun internAtom(display: CValuesRef<Display>, name: String, onlyIfExists: Boolean): Atom = 0.convert()

    override fun killClient(display: CValuesRef<Display>, window: Window): Int = 0

    override fun sync(display: CValuesRef<Display>, discardQueuedEvents: Boolean): Int = 0

    override fun sendEvent(
        display: CValuesRef<Display>,
        window: Window,
        propagate: Boolean,
        eventMask: Long,
        event: CValuesRef<XEvent>
    ): Int = 0

    override fun nextEvent(display: CValuesRef<Display>, event: CValuesRef<XEvent>): Int = 0

    override fun configureWindow(
        display: CValuesRef<Display>,
        window: Window,
        configurationMask: UInt,
        configuration: CValuesRef<XWindowChanges>
    ): Int = 0

    override fun reparentWindow(display: CValuesRef<Display>, window: Window, parent: Window, x: Int, y: Int): Int = 0

    override fun resizeWindow(display: CValuesRef<Display>, window: Window, width: UInt, height: UInt): Int = 0

    override fun moveResizeWindow(
        display: CValuesRef<Display>,
        window: Window,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun mapWindow(display: CValuesRef<Display>, window: Window): Int = 0

    override fun unmapWindow(display: CValuesRef<Display>, window: Window): Int = 0

    override fun destroyWindow(display: CValuesRef<Display>, window: Window): Int = 0

    override fun getenv(name: String): CPointer<ByteVar>? = null

    override fun fopen(fileName: String, modes: String): CPointer<FILE>? = null

    override fun fgets(buffer: CValuesRef<ByteVar>, bufferSize: Int, file: CValuesRef<FILE>): CPointer<ByteVar>? = null

    override fun fclose(file: CValuesRef<FILE>): Int = 0

    override fun fork(): __pid_t = 0

    override fun setsid(): __pid_t = 0

    override fun perror(s: String) {}

    override fun exit(status: Int) {}

    override fun execvp(fileName: String, args: CValuesRef<CPointerVar<ByteVar>>): Int = 0

    override fun selectInput(display: CValuesRef<Display>, window: Window, mask: Long): Int = 0

    override fun setInputFocus(display: CValuesRef<Display>, window: Window, revertTo: Int, time: Time): Int = 0

    override fun grabKey(
        display: CValuesRef<Display>,
        keyCode: Int,
        modifiers: UInt,
        window: Window,
        ownerEvents: Boolean,
        pointerMode: Int,
        keyboardMode: Int
    ): Int = 0

    override fun grabButton(
        display: CValuesRef<Display>,
        button: UInt,
        modifiers: UInt,
        window: Window,
        ownerEvents: Boolean,
        mask: UInt,
        pointerMode: Int,
        keyboardMode: Int,
        windowToConfineTo: Window,
        cursor: Cursor
    ): Int = 0

    override fun getModifierMapping(display: CValuesRef<Display>): CPointer<XModifierKeymap>? = null

    override fun keysymToKeycode(display: CValuesRef<Display>, keySym: KeySym): KeyCode = 0.convert()

    override fun rQueryExtension(
        display: CValuesRef<Display>,
        eventBase: CValuesRef<IntVar>,
        errorBase: CValuesRef<IntVar>
    ): Int = 0

    override fun rSelectInput(display: CValuesRef<Display>, window: Window, mask: Int) {}

    override fun rGetScreenResources(display: CValuesRef<Display>, window: Window): CPointer<XRRScreenResources>? = null

    override fun rGetOutputPrimary(display: CValuesRef<Display>, window: Window): RROutput = 0.convert()

    override fun rGetOutputInfo(
        display: CValuesRef<Display>,
        resources: CPointer<XRRScreenResources>,
        output: RROutput
    ): CPointer<XRROutputInfo>? = null

    override fun rGetCrtcInfo(
        display: CValuesRef<Display>,
        resources: CPointer<XRRScreenResources>,
        crtc: RRCrtc
    ): CPointer<XRRCrtcInfo>? = null

    override fun fillArcs(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        arcs: CValuesRef<XArc>,
        arcCount: Int
    ): Int = 0

    override fun fillRectangle(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun fillRectangles(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC,
        rects: CValuesRef<XRectangle>,
        rectCount: Int
    ): Int = 0

    override fun putImage(
        display: CValuesRef<Display>,
        drawable: Drawable,
        graphicsContext: GC?,
        image: CValuesRef<XImage>,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun createGC(
        display: CValuesRef<Display>,
        drawable: Drawable,
        mask: ULong,
        gcValues: CValuesRef<XGCValues>?
    ): GC? = null

    override fun freeGC(display: CValuesRef<Display>, graphicsContext: GC?): Int = 0

    override fun createColormap(
        display: CValuesRef<Display>,
        window: Window,
        visual: CValuesRef<Visual>?,
        alloc: Int
    ): Colormap = 0.convert()

    override fun allocColor(display: CValuesRef<Display>, colorMap: Colormap, color: CValuesRef<XColor>): Int = 0

    override fun freeColors(
        display: CValuesRef<Display>,
        colorMap: Colormap,
        pixels: CValuesRef<ULongVar>,
        pixelCount: Int
    ): Int = 0

    override fun freeColormap(display: CValuesRef<Display>, colorMap: Colormap): Int = 0

    override fun readXpmFileToImage(
        display: CValuesRef<Display>,
        imagePath: String,
        imageBuffer: CValuesRef<CPointerVar<XImage>>
    ): Int = 0
}