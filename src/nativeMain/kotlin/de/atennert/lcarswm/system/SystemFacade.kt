package de.atennert.lcarswm.system

import de.atennert.lcarswm.X_FALSE
import de.atennert.lcarswm.X_TRUE
import de.atennert.lcarswm.signal.Signal
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import platform.posix.sigfillset
import platform.posix.sigemptyset
import platform.posix.sigprocmask
import platform.posix.FILE
import platform.posix.__pid_t
import platform.posix.sigaction
import platform.posix.sigset_t
import platform.posix.timeval
import xlib.*

/**
 * This is the facade for accessing system functions.
 */
class SystemFacade : SystemApi {
    private var display: CPointer<Display>? = null

    override fun openDisplay(): Boolean {
        this.display = XOpenDisplay(null)
        return this.display != null
    }

    override fun closeDisplay(): Int {
        return XCloseDisplay(display)
    }

    override fun defaultScreenOfDisplay(): CPointer<Screen>? {
        return XDefaultScreenOfDisplay(display)
    }

    override fun defaultScreenNumber(): Int {
        return XDefaultScreen(display)
    }

    override fun grabServer(): Int {
        return XGrabServer(display)
    }

    override fun ungrabServer(): Int {
        return XUngrabServer(display)
    }

    override fun addToSaveSet(window: Window): Int {
        return XAddToSaveSet(display, window)
    }

    override fun removeFromSaveSet(window: Window): Int {
        return XRemoveFromSaveSet(display, window)
    }

    override fun queryTree(
        window: Window,
        rootReturn: CValuesRef<WindowVar>,
        parentReturn: CValuesRef<WindowVar>,
        childrenReturn: CValuesRef<CPointerVar<WindowVar>>,
        childrenReturnCounts: CValuesRef<UIntVar>
    ): Int {
        return XQueryTree(display, window, rootReturn, parentReturn, childrenReturn, childrenReturnCounts)
    }

    override fun getWindowAttributes(
        window: Window,
        attributes: CPointer<XWindowAttributes>
    ): Int {
        return XGetWindowAttributes(display, window, attributes)
    }

    override fun changeWindowAttributes(window: Window, mask: ULong, attributes: CPointer<XSetWindowAttributes>): Int {
        return XChangeWindowAttributes(display, window, mask, attributes)
    }

    override fun getWMProtocols(
        window: Window,
        protocolsReturn: CPointer<CPointerVar<AtomVar>>,
        protocolCountReturn: CPointer<IntVar>
    ): Int {
        return XGetWMProtocols(display, window, protocolsReturn, protocolCountReturn)
    }

    override fun setErrorHandler(handler: XErrorHandler): XErrorHandler? {
        return XSetErrorHandler(handler)
    }

    override fun internAtom(name: String, onlyIfExists: Boolean): Atom {
        return XInternAtom(display, name, convertToXBoolean(onlyIfExists))
    }

    override fun changeProperty(
        window: Window,
        propertyAtom: Atom,
        typeAtom: Atom,
        data: UByteArray?,
        format: Int,
        mode: Int
    ): Int {
        val bytesPerData = format.div(8)
        val dataCount = data?.size?.div(bytesPerData) ?: 0
        return XChangeProperty(display, window, propertyAtom, typeAtom, format, PropModeReplace, data?.toCValues(), dataCount)
    }

    override fun deleteProperty(window: Window, propertyAtom: Atom): Int {
        return XDeleteProperty(display, window, propertyAtom)
    }

    override fun getTextProperty(window: Window, textProperty: CPointer<XTextProperty>, propertyAtom: Atom): Int {
        return XGetTextProperty(display, window, textProperty, propertyAtom)
    }

    override fun xmbTextPropertyToTextList(
        textProperty: CPointer<XTextProperty>,
        resultList: CPointer<CPointerVar<CPointerVar<ByteVar>>>,
        stringCount: CPointer<IntVar>
    ): Int {
        return XmbTextPropertyToTextList(display, textProperty, resultList, stringCount)
    }

    override fun localeToUtf8(
        localeString: String,
        stringSize: Long,
        bytesRead: CPointer<ULongVar>?
    ): CPointer<ByteVar>? {
        return g_locale_to_utf8(localeString, stringSize, bytesRead, null, null)
    }

    override fun convertLatinToUtf8(
        latinString: String,
        stringSize: Long,
        bytesRead: CPointer<ULongVar>?
    ): CPointer<ByteVar>? {
        return g_convert(latinString, stringSize, "utf-8", "iso-8859-1",
            bytesRead, null, null)
    }

    override fun killClient(window: Window): Int {
        return XKillClient(display, window)
    }

    override fun createWindow(
        parentWindow: Window,
        measurements: List<Int>,
        visual: CPointer<Visual>?,
        attributeMask: ULong,
        attributes: CPointer<XSetWindowAttributes>
    ): Window {
        return XCreateWindow(display, parentWindow, measurements[0], measurements[1], measurements[2].convert(), measurements[3].convert(), 0.convert(), CopyFromParent.convert(), InputOutput, visual, attributeMask, attributes)
    }

    override fun createSimpleWindow(parentWindow: Window, measurements: List<Int>): Window {
        return XCreateSimpleWindow(display, parentWindow, measurements[0], measurements[1],
            measurements[2].convert(), measurements[4].convert(), 0.convert(), 0.convert(), 0.convert())
    }

    override fun getSelectionOwner(atom: Atom): Window {
        return XGetSelectionOwner(display, atom)
    }

    override fun setSelectionOwner(atom: Atom, window: Window, time: Time): Int {
        return XSetSelectionOwner(display, atom, window, time)
    }

    override fun getDisplayString(): String {
        return XDisplayString(this.display)?.toKString() ?: ""
    }

    override fun flush() {
        XFlush(this.display)
    }

    override fun synchronize(sync: Boolean) {
        XSynchronize(display, convertToXBoolean(sync))
    }

    override fun free(xObject: CPointer<*>?) {
        XFree(xObject)
    }

    override fun sync(discardQueuedEvents: Boolean): Int {
        return XSync(display, convertToXBoolean(discardQueuedEvents))
    }

    override fun sendEvent(
        window: Window,
        propagate: Boolean,
        eventMask: Long,
        event: CPointer<XEvent>
    ): Int {
        return XSendEvent(display, window, convertToXBoolean(propagate), eventMask, event)
    }

    override fun nextEvent(event: CPointer<XEvent>): Int {
        return XNextEvent(display, event)
    }

    override fun getQueuedEvents(mode: Int): Int {
        return XEventsQueued(display, mode)
    }

    override fun getDisplay(): CPointer<Display>? = this.display

    override fun configureWindow(
        window: Window,
        configurationMask: UInt,
        configuration: CPointer<XWindowChanges>
    ): Int {
        return XConfigureWindow(display, window, configurationMask, configuration)
    }

    override fun setWindowBorderWidth(window: Window, borderWidth: UInt): Int {
        return XSetWindowBorderWidth(display, window, borderWidth)
    }

    override fun reparentWindow(window: Window, parent: Window, x: Int, y: Int): Int {
        return XReparentWindow(display, window, parent, x, y)
    }

    override fun resizeWindow(window: Window, width: UInt, height: UInt): Int {
        return XResizeWindow(display, window, width, height)
    }

    override fun moveResizeWindow(
        window: Window,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int {
        return XMoveResizeWindow(display, window, x, y, width, height)
    }

    override fun lowerWindow(window: Window): Int {
        return XLowerWindow(display, window)
    }

    override fun mapWindow(window: Window): Int {
        return XMapWindow(display, window)
    }

    override fun unmapWindow(window: Window): Int {
        return XUnmapWindow(display, window)
    }

    override fun destroyWindow(window: Window): Int {
        return XDestroyWindow(display, window)
    }

    override fun getenv(name: String): CPointer<ByteVar>? {
        return platform.posix.getenv(name)
    }

    override fun fopen(fileName: String, modes: String): CPointer<FILE>? {
        return platform.posix.fopen(fileName, modes)
    }

    override fun fgets(buffer: CPointer<ByteVar>, bufferSize: Int, file: CPointer<FILE>): CPointer<ByteVar>? {
        return platform.posix.fgets(buffer, bufferSize, file)
    }

    override fun fputs(s: String, file: CPointer<FILE>): Int {
        return platform.posix.fputs(s, file)
    }

    override fun fclose(file: CPointer<FILE>): Int {
        return platform.posix.fclose(file)
    }

    override fun feof(file: CPointer<FILE>): Int {
        return platform.posix.feof(file)
    }

    override fun fork(): __pid_t {
        return platform.posix.fork()
    }

    override fun setsid(): __pid_t {
        return platform.posix.setsid()
    }

    override fun setenv(name: String, value: String): Int {
        return platform.posix.setenv(name, value, X_TRUE)
    }

    override fun perror(s: String) {
        platform.posix.perror(s)
    }

    override fun exit(status: Int) {
        platform.posix.exit(status)
    }

    override fun execvp(fileName: String, args: List<String>): Int {
        val byteArgs = args.map { it.encodeToByteArray().pin().addressOf(0).pointed }
        val convertedArgs = nativeHeap.allocArrayOfPointersTo(byteArgs)

        return platform.posix.execvp(fileName, convertedArgs)
    }

    override fun gettimeofday(): Long {
        val timeStruct = nativeHeap.alloc<timeval>()
        platform.posix.gettimeofday(timeStruct.ptr, null)
        val time = timeStruct.tv_sec
        nativeHeap.free(timeStruct)
        return time
    }

    override fun usleep(time: UInt) {
        platform.posix.usleep(time)
    }

    override fun abort() {
        platform.posix.abort()
    }

    override fun sigFillSet(sigset: CPointer<sigset_t>) {
        sigfillset(sigset)
    }

    override fun sigEmptySet(sigset: CPointer<sigset_t>) {
        sigemptyset(sigset)
    }

    override fun sigAction(signal: Signal, newSigAction: CPointer<sigaction>, oldSigAction: CPointer<sigaction>?) {
        sigaction(signal.signalValue, newSigAction, oldSigAction)
    }

    override fun sigProcMask(how: Int, newSigset: CPointer<sigset_t>?, oldSigset: CPointer<sigset_t>?) {
        sigprocmask(how, newSigset, oldSigset)
    }

    override fun selectInput(window: Window, mask: Long): Int {
        return XSelectInput(display, window, mask)
    }

    override fun setInputFocus(window: Window, revertTo: Int, time: Time): Int {
        return XSetInputFocus(display, window, revertTo, time)
    }

    override fun grabKey(
        keyCode: Int,
        modifiers: UInt,
        window: Window,
        keyboardMode: Int
    ): Int {
        return XGrabKey(display, keyCode, modifiers, window, X_FALSE, GrabModeAsync, keyboardMode)
    }

    override fun ungrabKey(window: Window) {
        XUngrabKey(display, AnyKey.convert(), AnyModifier, window)
    }

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
    ): Int {
        return XGrabButton(display, button, modifiers, window, convertToXBoolean(ownerEvents), mask, pointerMode, keyboardMode, windowToConfineTo, cursor)
    }

    override fun getModifierMapping(): CPointer<XModifierKeymap>? {
        return XGetModifierMapping(display)
    }

    override fun getDisplayKeyCodeMinMaxCounts(): Pair<Int, Int> {
        val keyCodes = IntArray(2)
        keyCodes.usePinned {
            XDisplayKeycodes(display, it.addressOf(0), it.addressOf(1))
        }
        return Pair(keyCodes[0], keyCodes[1])
    }

    override fun getKeyboardMapping(
        firstKeyCode: KeyCode,
        keyCodeCount: Int,
        keySymsPerKeyCode: CPointer<IntVar>
    ): CPointer<KeySymVar>? {
        return XGetKeyboardMapping(display, firstKeyCode, keyCodeCount, keySymsPerKeyCode)
    }

    override fun keysymToKeycode(keySym: KeySym): KeyCode {
        return XKeysymToKeycode(display, keySym)
    }

    override fun stringToKeysym(s: String): KeySym {
        return XStringToKeysym(s)
    }

    override fun freeModifiermap(modifierMap: CPointer<XModifierKeymap>?) {
        XFreeModifiermap(modifierMap)
    }

    override fun rQueryExtension(eventBase: CPointer<IntVar>, errorBase: CPointer<IntVar>): Int {
        return XRRQueryExtension(display, eventBase, errorBase)
    }

    override fun rSelectInput(window: Window, mask: Int) {
        XRRSelectInput(display, window, mask)
    }

    override fun rGetScreenResources(window: Window): CPointer<XRRScreenResources>? {
        return XRRGetScreenResources(display, window)
    }

    override fun rGetOutputPrimary(window: Window): RROutput {
        return XRRGetOutputPrimary(display, window)
    }

    override fun rGetOutputInfo(
        resources: CPointer<XRRScreenResources>,
        output: RROutput
    ): CPointer<XRROutputInfo>? {
        return XRRGetOutputInfo(display, resources, output)
    }

    override fun rGetCrtcInfo(
        resources: CPointer<XRRScreenResources>,
        crtc: RRCrtc
    ): CPointer<XRRCrtcInfo>? {
        return XRRGetCrtcInfo(display, resources, crtc)
    }

    override fun fillArcs(
        drawable: Drawable,
        graphicsContext: GC,
        arcs: CValuesRef<XArc>,
        arcCount: Int
    ): Int {
        return XFillArcs(display, drawable, graphicsContext, arcs, arcCount)
    }

    override fun fillRectangle(
        drawable: Drawable,
        graphicsContext: GC,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int {
        return XFillRectangle(display, drawable, graphicsContext, x, y, width, height)
    }

    override fun fillRectangles(
        drawable: Drawable,
        graphicsContext: GC,
        rects: CValuesRef<XRectangle>,
        rectCount: Int
    ): Int {
        return XFillRectangles(display, drawable, graphicsContext, rects, rectCount)
    }

    override fun putImage(
        drawable: Drawable,
        graphicsContext: GC,
        image: CValuesRef<XImage>,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int {
        return XPutImage(display, drawable, graphicsContext, image, 0, 0, x, y, width, height)
    }

    override fun createGC(
        drawable: Drawable,
        mask: ULong,
        gcValues: CValuesRef<XGCValues>?
    ): GC? {
        return XCreateGC(display, drawable, mask, gcValues)
    }

    override fun freeGC(graphicsContext: GC): Int {
        return XFreeGC(display, graphicsContext)
    }

    override fun createColormap(
        window: Window,
        visual: CValuesRef<Visual>,
        alloc: Int
    ): Colormap {
        return XCreateColormap(display, window, visual, alloc)
    }

    override fun allocColor(colorMap: Colormap, color: CPointer<XColor>): Int {
        return XAllocColor(display, colorMap, color)
    }

    override fun freeColors(
        colorMap: Colormap,
        pixels: CValuesRef<ULongVar>,
        pixelCount: Int
    ): Int {
        return XFreeColors(display, colorMap, pixels, pixelCount, 0.convert())
    }

    override fun freeColormap(colorMap: Colormap): Int {
        return XFreeColormap(display, colorMap)
    }

    override fun readXpmFileToImage(
        imagePath: String,
        imageBuffer: CPointer<CPointerVar<XImage>>
    ): Int {
        return XpmReadFileToImage(display, imagePath, imageBuffer, null, null)
    }

    override fun createPixmap(drawable: Drawable, width: UInt, height: UInt, depth: UInt): Pixmap {
        return XCreatePixmap(display, drawable, width, height, depth)
    }

    override fun xftGetContext(screen: Int): CPointer<PangoContext>? {
        return pango_xft_get_context(display, screen)
    }

    override fun newLayout(pango: CPointer<PangoContext>?): CPointer<PangoLayout>? {
        return pango_layout_new(pango)
    }

    override fun getFontDescription(): CPointer<PangoFontDescription>? {
        return pango_font_description_new()
    }

    private fun convertToXBoolean(ownerEvents: Boolean): Int = when (ownerEvents) {
        true  -> X_TRUE
        false -> X_FALSE
    }
}