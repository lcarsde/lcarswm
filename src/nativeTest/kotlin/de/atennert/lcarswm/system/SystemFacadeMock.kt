package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import platform.posix.FILE
import platform.posix.__pid_t
import xlib.*

open class SystemFacadeMock : SystemApi {
    override fun closeDisplay(): Int = 0

    override fun defaultScreenOfDisplay(): CPointer<Screen>? = null

    override fun grabServer(): Int = 0

    override fun ungrabServer(): Int = 0

    override fun addToSaveSet(window: Window): Int = 0

    override fun removeFromSaveSet(window: Window): Int = 0

    override fun queryTree(
        window: Window,
        rootReturn: CValuesRef<WindowVar>,
        parentReturn: CValuesRef<WindowVar>,
        childrenReturn: CValuesRef<CPointerVar<WindowVar>>,
        childrenReturnCounts: CValuesRef<UIntVar>
    ): Int = 0

    override fun getWindowAttributes(
        window: Window,
        attributes: CValuesRef<XWindowAttributes>
    ): Int = 0

    override fun getWMProtocols(
        window: Window,
        protocolsReturn: CValuesRef<CPointerVar<AtomVar>>,
        protocolCountReturn: CValuesRef<IntVar>
    ): Int = 0

    override fun setErrorHandler(handler: XErrorHandler): XErrorHandler? = null

    override fun internAtom(name: String, onlyIfExists: Boolean): Atom = 0.convert()

    override fun changeProperty(window: Window, propertyAtom: Atom, typeAtom: Atom, data: UByteArray?): Int = 0

    override fun killClient(window: Window): Int = 0

    override fun createSimpleWindow(parentWindow: Window, measurements: List<Int>): Window = 0.convert()

    override fun sync(discardQueuedEvents: Boolean): Int = 0

    override fun sendEvent(
        window: Window,
        propagate: Boolean,
        eventMask: Long,
        event: CPointer<XEvent>
    ): Int = 0

    override fun nextEvent(event: CPointer<XEvent>): Int = 0

    override fun getDisplay(): CPointer<Display>? = null

    override fun configureWindow(
        window: Window,
        configurationMask: UInt,
        configuration: CPointer<XWindowChanges>
    ): Int = 0

    override fun reparentWindow(window: Window, parent: Window, x: Int, y: Int): Int = 0

    override fun resizeWindow(window: Window, width: UInt, height: UInt): Int = 0

    override fun moveResizeWindow(
        window: Window,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun mapWindow(window: Window): Int = 0

    override fun unmapWindow(window: Window): Int = 0

    override fun destroyWindow(window: Window): Int = 0

    override fun getenv(name: String): CPointer<ByteVar>? = null

    override fun fopen(fileName: String, modes: String): CPointer<FILE>? = null

    override fun fgets(buffer: CValuesRef<ByteVar>, bufferSize: Int, file: CPointer<FILE>): CPointer<ByteVar>? = null

    override fun fputs(s: String, file: CPointer<FILE>): Int = 0

    override fun fclose(file: CPointer<FILE>): Int = 0

    override fun fork(): __pid_t = 0

    override fun setsid(): __pid_t = 0

    override fun perror(s: String) {}

    override fun exit(status: Int) {}

    override fun execvp(fileName: String, args: CValuesRef<CPointerVar<ByteVar>>): Int = 0

    override fun gettimeofday(): Long = 0

    override fun selectInput(window: Window, mask: Long): Int = 0

    override fun setInputFocus(window: Window, revertTo: Int, time: Time): Int = 0

    override fun grabKey(
        keyCode: Int,
        modifiers: UInt,
        window: Window,
        ownerEvents: Boolean,
        pointerMode: Int,
        keyboardMode: Int
    ): Int = 0

    override fun grabButton(
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

    override fun getModifierMapping(): CPointer<XModifierKeymap>? = null

    override fun keysymToKeycode(keySym: KeySym): KeyCode = 0.convert()

    override fun rQueryExtension(eventBase: CValuesRef<IntVar>, errorBase: CValuesRef<IntVar>): Int = 0

    override fun rSelectInput(window: Window, mask: Int) {}

    override fun rGetScreenResources(window: Window): CPointer<XRRScreenResources>? = null

    override fun rGetOutputPrimary(window: Window): RROutput = 0.convert()

    override fun rGetOutputInfo(
        resources: CPointer<XRRScreenResources>,
        output: RROutput
    ): CPointer<XRROutputInfo>? = null

    override fun rGetCrtcInfo(
        resources: CPointer<XRRScreenResources>,
        crtc: RRCrtc
    ): CPointer<XRRCrtcInfo>? = null

    override fun fillArcs(
        drawable: Drawable,
        graphicsContext: GC,
        arcs: CValuesRef<XArc>,
        arcCount: Int
    ): Int = 0

    override fun fillRectangle(
        drawable: Drawable,
        graphicsContext: GC,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun fillRectangles(
        drawable: Drawable,
        graphicsContext: GC,
        rects: CValuesRef<XRectangle>,
        rectCount: Int
    ): Int = 0

    override fun putImage(
        drawable: Drawable,
        graphicsContext: GC?,
        image: CValuesRef<XImage>,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int = 0

    override fun createGC(
        drawable: Drawable,
        mask: ULong,
        gcValues: CValuesRef<XGCValues>?
    ): GC? = null

    override fun freeGC(graphicsContext: GC?): Int = 0

    override fun createColormap(
        window: Window,
        visual: CValuesRef<Visual>?,
        alloc: Int
    ): Colormap = 0.convert()

    override fun allocColor(colorMap: Colormap, color: CValuesRef<XColor>): Int = 0

    override fun freeColors(
        colorMap: Colormap,
        pixels: CValuesRef<ULongVar>,
        pixelCount: Int
    ): Int = 0

    override fun freeColormap(colorMap: Colormap): Int = 0

    override fun readXpmFileToImage(
        imagePath: String,
        imageBuffer: CValuesRef<CPointerVar<XImage>>
    ): Int = 0
}