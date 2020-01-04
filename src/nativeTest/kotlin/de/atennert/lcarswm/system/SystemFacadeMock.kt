package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import platform.posix.FILE
import platform.posix.__pid_t
import xlib.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class SystemFacadeMock : SystemApi {
    val functionCalls = mutableListOf<FunctionCall>()

    val randrEventBase = 80
    val randrErrorBase = 160
    override fun rQueryExtension(eventBase: CPointer<IntVar>, errorBase: CPointer<IntVar>): Int {
        eventBase[0] = randrEventBase
        errorBase[0] = randrErrorBase
        return 0
    }

    override fun rSelectInput(window: Window, mask: Int) {
        functionCalls.add(FunctionCall("rSelectInput", window, mask))
    }

    val outputs = ulongArrayOf(1.convert(), 2.convert())
    override fun rGetScreenResources(window: Window): CPointer<XRRScreenResources>? {
        assertEquals(rootWindowId, window, "The window manager should only request screen resources for the root window")
        val screenResources = nativeHeap.alloc<XRRScreenResources>()
        screenResources.noutput = outputs.size
        screenResources.outputs = outputs.pin().addressOf(0)
        return screenResources.ptr
    }

    val primaryOutput = outputs[0]
    override fun rGetOutputPrimary(window: Window): RROutput {
        assertEquals(rootWindowId, window, "The window manager should only request the primary output for the root window")
        return primaryOutput
    }

    val outputNames = arrayOf("output1", "output2")
    override fun rGetOutputInfo(resources: CPointer<XRRScreenResources>, output: RROutput): CPointer<XRROutputInfo>? {
        val outputInfo = nativeHeap.alloc<XRROutputInfo>()
        val outputName = outputNames[outputs.indexOf(output)]
        outputInfo.name = outputName.encodeToByteArray().pin().addressOf(0)
        outputInfo.nameLen = outputName.length
        outputInfo.crtc = output
        return outputInfo.ptr
    }

    val crtcInfos = arrayOf(arrayOf(0, 0, 1000, 500), arrayOf(1000, 0, 1000, 500))
    override fun rGetCrtcInfo(resources: CPointer<XRRScreenResources>, crtc: RRCrtc): CPointer<XRRCrtcInfo>? {
        val usedCrtcInfos = crtcInfos[crtc.convert<Int>() - 1];
        val crtcInfo = nativeHeap.alloc<XRRCrtcInfo>()
        crtcInfo.x = usedCrtcInfos[0]
        crtcInfo.y = usedCrtcInfos[1]
        crtcInfo.width = usedCrtcInfos[2].convert()
        crtcInfo.height = usedCrtcInfos[3].convert()
        return crtcInfo.ptr
    }

    override fun fillArcs(drawable: Drawable, graphicsContext: GC, arcs: CValuesRef<XArc>, arcCount: Int): Int {
        functionCalls.add(FunctionCall("fillArcs", drawable, graphicsContext, arcs, arcCount))
        return 0
    }

    override fun fillRectangle(
        drawable: Drawable,
        graphicsContext: GC,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ): Int {
        functionCalls.add(FunctionCall("fillRectangle", drawable, graphicsContext, x, y, width, height))
        return 0
    }

    override fun fillRectangles(
        drawable: Drawable,
        graphicsContext: GC,
        rects: CValuesRef<XRectangle>,
        rectCount: Int
    ): Int {
        functionCalls.add(FunctionCall("fillRectangles", drawable, graphicsContext, rects, rectCount))
        return 0
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
        functionCalls.add(FunctionCall("putImage", drawable, graphicsContext, image, x, y, width, height))
        return 0
    }

    override fun createGC(drawable: Drawable, mask: ULong, gcValues: CValuesRef<XGCValues>?): GC? {
        functionCalls.add(FunctionCall("createGC", drawable, mask, gcValues))
        // the following is really evil, but we need a pointer for testing and can't directly create a GC
        return nativeHeap.alloc<GCVar>().ptr.reinterpret()
    }

    override fun freeGC(graphicsContext: GC): Int {
        functionCalls.add(FunctionCall("freeGC", graphicsContext))
        nativeHeap.free(graphicsContext)
        return 0
    }

    override fun createColormap(window: Window, visual: CValuesRef<Visual>, alloc: Int): Colormap {
        functionCalls.add(FunctionCall("createColormap", window, visual, alloc))
        return 0.convert()
    }

    override fun allocColor(colorMap: Colormap, color: CPointer<XColor>): Int {
        functionCalls.add(FunctionCall("allocColor", colorMap, color))
        color.pointed.pixel = 0.convert()
        return 0
    }

    override fun freeColors(colorMap: Colormap, pixels: CValuesRef<ULongVar>, pixelCount: Int): Int {
        functionCalls.add(FunctionCall("freeColors", colorMap, pixels, pixelCount))
        return 0
    }

    override fun freeColormap(colorMap: Colormap): Int {
        functionCalls.add(FunctionCall("freeColormap", colorMap))
        return 0
    }

    override fun readXpmFileToImage(imagePath: String, imageBuffer: CPointer<CPointerVar<XImage>>): Int {
        functionCalls.add(FunctionCall("readXpmFileToImage", imagePath, imageBuffer))
        return 0
    }

    override fun selectInput(window: Window, mask: Long): Int {
        functionCalls.add(FunctionCall("selectInput", window, mask))
        return 0
    }

    override fun setInputFocus(window: Window, revertTo: Int, time: Time): Int {
        functionCalls.add(FunctionCall("setInputFocus", window, revertTo, time))
        return 0
    }

    override fun grabKey(
        keyCode: Int,
        modifiers: UInt,
        window: Window,
        ownerEvents: Boolean,
        pointerMode: Int,
        keyboardMode: Int
    ): Int {
        functionCalls.add(FunctionCall("grabKey", keyCode, modifiers, window, ownerEvents, pointerMode, keyboardMode))
        return 0
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
        functionCalls.add(FunctionCall("grabButton", button, modifiers, window, ownerEvents, mask, pointerMode, keyboardMode, windowToConfineTo, cursor))
        return 0
    }

    val modifiers = UByteArray(8) { 1.shl(it).convert() }

    val winModifierPosition = 6

    private var startKeyCode = 0

    private val keyStrings = mapOf(
        Pair("Tab", XK_Tab),
        Pair("A", XK_A),
        Pair("B", XK_B),
        Pair("C", XK_C),
        Pair("I", XK_I),
        Pair("M", XK_M),
        Pair("Q", XK_Q),
        Pair("T", XK_T),
        Pair("X", XK_X),
        Pair("F4", XK_F4),
        Pair("Up", XK_Up),
        Pair("Down", XK_Down),
        Pair("space", XK_space),
        Pair("XF86AudioMute", XF86XK_AudioMute),
        Pair("XF86AudioRaiseVolume", XF86XK_AudioRaiseVolume),
        Pair("XF86AudioLowerVolume", XF86XK_AudioLowerVolume)
    )

    val keySyms = keyStrings.values.associateWith { startKeyCode++ }

    override fun getModifierMapping(): CPointer<XModifierKeymap>? {
        val keymap = nativeHeap.alloc<XModifierKeymap>()
        keymap.max_keypermod = 1
        keymap.modifiermap = modifiers.pin().addressOf(0)
        return keymap.ptr
    }

    override fun keysymToKeycode(keySym: KeySym): KeyCode {
        return keySyms[keySym.convert()]?.convert() ?: error("keySym not found")
    }

    override fun stringToKeysym(s: String): KeySym {
        return keyStrings.getValue(s).convert()
    }

    override fun sync(discardQueuedEvents: Boolean): Int {
        functionCalls.add(FunctionCall("sync", discardQueuedEvents))
        return 0
    }

    override fun sendEvent(window: Window, propagate: Boolean, eventMask: Long, event: CPointer<XEvent>): Int {
        functionCalls.add(FunctionCall("sendEvent", window, propagate, eventMask, event))
        return 0
    }

    override fun nextEvent(event: CPointer<XEvent>): Int {
        functionCalls.add(FunctionCall("nextEvent", event))
        return 0
    }

    override fun configureWindow(
        window: Window,
        configurationMask: UInt,
        configuration: CPointer<XWindowChanges>
    ): Int {
        functionCalls.add(FunctionCall("configureWindow", window, configurationMask, configuration))
        return 0
    }

    override fun reparentWindow(window: Window, parent: Window, x: Int, y: Int): Int {
        functionCalls.add(FunctionCall("reparentWindow", window, parent, x, y))
        return 0
    }

    override fun resizeWindow(window: Window, width: UInt, height: UInt): Int {
        functionCalls.add(FunctionCall("resizeWindow", window, width, height))
        return 0
    }

    override fun moveResizeWindow(window: Window, x: Int, y: Int, width: UInt, height: UInt): Int {
        functionCalls.add(FunctionCall("moveResizeWindow", window, x, y, width, height))
        return 0
    }

    override fun lowerWindow(window: Window): Int {
        functionCalls.add(FunctionCall("lowerWindow", window))
        return 0
    }

    override fun mapWindow(window: Window): Int {
        functionCalls.add(FunctionCall("mapWindow", window))
        return 0
    }

    override fun unmapWindow(window: Window): Int {
        functionCalls.add(FunctionCall("unmapWindow", window))
        return 0
    }

    override fun destroyWindow(window: Window): Int {
        functionCalls.add(FunctionCall("destroyWindow", window))
        return 0
    }

    private var display: CPointer<Display>? = null

    override fun getDisplay(): CPointer<Display>? {
        // no function call logging, as this is a not-out-calling utility function
        return this.display
    }

    override fun openDisplay(): Boolean {
        functionCalls.add(FunctionCall("openDisplay"))
        this.display = nativeHeap.allocPointerTo<Display>().value
        return true
    }

    override fun closeDisplay(): Int {
        functionCalls.add(FunctionCall("closeDisplay"))
        val currentDisplay = this.display
        if (currentDisplay != null) {
            nativeHeap.free(currentDisplay)
        }
        this.display = null
        return 0
    }

    override fun defaultScreenOfDisplay(): CPointer<Screen>? {
        functionCalls.add(FunctionCall("defaultScreenOfDisplay"))
        val screen = nativeHeap.alloc<Screen>()
        screen.root = rootWindowId
        screen.root_visual = nativeHeap.alloc<Visual>().ptr
        return screen.ptr
    }

    override fun defaultScreenNumber(): Int {
        functionCalls.add(FunctionCall("defaultScreenOfDisplay"))
        return 0
    }

    override fun grabServer(): Int {
        functionCalls.add(FunctionCall("grabServer"))
        return 0
    }

    override fun ungrabServer(): Int {
        functionCalls.add(FunctionCall("ungrabServer"))
        return 0
    }

    override fun addToSaveSet(window: Window): Int {
        functionCalls.add(FunctionCall("addToSaveSet", window))
        return 0
    }

    override fun removeFromSaveSet(window: Window): Int {
        functionCalls.add(FunctionCall("removeFromSaveSet", window))
        return 0
    }

    override fun queryTree(
        window: Window,
        rootReturn: CValuesRef<WindowVar>,
        parentReturn: CValuesRef<WindowVar>,
        childrenReturn: CValuesRef<CPointerVar<WindowVar>>,
        childrenReturnCounts: CValuesRef<UIntVar>
    ): Int {
        functionCalls.add(FunctionCall("queryTree", window, rootReturn, parentReturn, childrenReturn, childrenReturnCounts))
        return 0
    }

    override fun getWindowAttributes(window: Window, attributes: CPointer<XWindowAttributes>): Int {
        return 0
    }

    override fun getWMProtocols(
        window: Window,
        protocolsReturn: CPointer<CPointerVar<AtomVar>>,
        protocolCountReturn: CPointer<IntVar>
    ): Int {
        val protocols = arrayOf("WM_DELETE_WINDOW")
        val atomArray = nativeHeap.allocArray<AtomVar>(protocols.size)
        protocols.forEachIndexed { index, atomName ->
            atomArray[index] = atomMap.getOrPut(atomName) { atomCounter++ }
        }

        protocolCountReturn.pointed.value = protocols.size
        protocolsReturn.pointed.value = atomArray
        return 0
    }

    override fun setErrorHandler(handler: XErrorHandler): XErrorHandler? {
        functionCalls.add(FunctionCall("setErrorHandler", handler))
        return handler
    }

    val atomMap = mutableMapOf<String, Atom>()
    private var atomCounter: Atom = 1.convert()
    override fun internAtom(name: String, onlyIfExists: Boolean): Atom {
        functionCalls.add(FunctionCall("internAtom", name, onlyIfExists))
        val nextAtom = atomMap[name] ?: atomCounter++
        atomMap[name] = nextAtom
        return nextAtom
    }

    override fun changeProperty(window: Window, propertyAtom: Atom, typeAtom: Atom, data: UByteArray?, format: Int): Int {
        functionCalls.add(FunctionCall("changeProperty", window, propertyAtom, typeAtom, data, format))
        return 0
    }

    override fun deleteProperty(window: Window, propertyAtom: Atom): Int {
        functionCalls.add(FunctionCall("deleteProperty", window, propertyAtom))
        return 0
    }

    override fun killClient(window: Window): Int {
        functionCalls.add(FunctionCall("killClient", window))
        return 0
    }

    val rootWindowId: Window = 1.convert()
    var nextWindowId: Window = 2.convert() // 1 is root window

    /**
     * Utility method to get a window ID for testing.
     * This will increment the window ID counter.
     */
    fun getNewWindowId(): Window = nextWindowId++

    override fun createWindow(
        parentWindow: Window,
        measurements: List<Int>,
        visual: CPointer<Visual>?,
        attributeMask: ULong,
        attributes: CPointer<XSetWindowAttributes>
    ): Window {
        functionCalls.add(FunctionCall("createWindow", parentWindow, measurements, attributeMask, attributes))
        return nextWindowId++
    }

    override fun createSimpleWindow(parentWindow: Window, measurements: List<Int>): Window {
        functionCalls.add(FunctionCall("createSimpleWindow", parentWindow, measurements))
        return nextWindowId++
    }

    private var selectionOwner: Window = None.convert()

    override fun getSelectionOwner(atom: Atom): Window {
        functionCalls.add(FunctionCall("getSelectionOwner", atom))
        return selectionOwner
    }

    override fun setSelectionOwner(atom: Atom, window: Window, time: Time): Int {
        functionCalls.add(FunctionCall("setSelectionOwner", atom, window, time))
        selectionOwner = window
        return 0
    }

    val displayString = "TheDisplay"
    override fun getDisplayString(): String {
        functionCalls.add(FunctionCall("getDisplayString"))
        return displayString
    }

    override fun getenv(name: String): CPointer<ByteVar>? {
        functionCalls.add(FunctionCall("getenv", name))
        val envValue = ByteArray(0)
        return envValue.pin().addressOf(0)
    }

    private val fileMap = mutableMapOf<CPointer<FILE>, MutableList<String>>()

    override fun fopen(fileName: String, modes: String): CPointer<FILE>? {
        functionCalls.add(FunctionCall("fopen", fileName, modes))
        val newFilePointer = nativeHeap.alloc<FILE>()
        fileMap[newFilePointer.ptr] = getLines(fileName).toMutableList()
        return newFilePointer.ptr
    }

    open fun getLines(fileName: String): List<String> = emptyList()

    override fun fgets(buffer: CPointer<ByteVar>, bufferSize: Int, file: CPointer<FILE>): CPointer<ByteVar>? {
        val lines = fileMap[file]?: return null
        if (lines.isEmpty()) {
            return null
        }

        val nextLine = lines.removeAt(0)
        val maxUsableCharacters = bufferSize - 1
        if (nextLine.length > maxUsableCharacters) {
            lines.add(0, nextLine.drop(maxUsableCharacters))
        }

        val providedCharacters = nextLine.take(maxUsableCharacters) + "\u0000"
        providedCharacters.encodeToByteArray()
            .forEachIndexed { index, value -> buffer[index] = value }

        return buffer
    }

    override fun fputs(s: String, file: CPointer<FILE>): Int {
        // Only used for logging, which we usually don't want to check
        return 0
    }

    override fun fclose(file: CPointer<FILE>): Int {
        functionCalls.add(FunctionCall("fclose", file))
        assertTrue(fileMap.contains(file))
        fileMap.remove(file)
        nativeHeap.free(file)
        return 0
    }

    override fun feof(file: CPointer<FILE>): Int {
        return if (fileMap.contains(file) && fileMap[file]!!.isEmpty()) 1 else 0
    }

    override fun fork(): __pid_t {
        functionCalls.add(FunctionCall("fork"))
        return 0
    }

    override fun setsid(): __pid_t {
        functionCalls.add(FunctionCall("setsid"))
        return 0
    }

    override fun setenv(name: String, value: String): Int {
        functionCalls.add(FunctionCall("setenv", name, value))
        return 0
    }

    override fun perror(s: String) {
        functionCalls.add(FunctionCall("perror", s))
    }

    override fun exit(status: Int) {
        functionCalls.add(FunctionCall("exit", status))
    }

    override fun execvp(fileName: String, args: CPointer<CPointerVar<ByteVar>>): Int {
        functionCalls.add(FunctionCall("execvp", fileName, args))
        return 0
    }

    override fun gettimeofday(): Long {
        functionCalls.add(FunctionCall("gettimeofday"))
        return 0
    }
}