import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShutdownTest {
    @Test
    fun `shutdown when there's no display to get`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            override fun openDisplay(): Boolean {
                super.openDisplay()
                return false
            }
        }
        runWindowManager(testFacade, logger)

        checkThatTheLoggerIsClosed(logger)
        checkForTryingToOpenDisplay(testFacade.functionCalls)
        checkThatThereIsNoUnexpectedInteraction(testFacade.functionCalls)
    }

    @Test
    fun `shutdown when there's no default screen to get`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            override fun defaultScreenOfDisplay(): CPointer<Screen>? {
                super.defaultScreenOfDisplay()
                return null
            }
        }
        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls
            .dropWhile { it.name != "defaultScreenOfDisplay" }
            .toMutableList()

        checkThatTheLoggerIsClosed(logger)
        checkRequestForDisplaysDefaultScreen(functionCalls)
        checkThatTheDisplayWasClosed(functionCalls)

        checkThatThereIsNoUnexpectedInteraction(functionCalls)
    }

    @Test
    fun `shutdown when there is already a screen owner`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            override fun defaultScreenNumber(): Int {
                return 23
            }

            override fun getSelectionOwner(atom: Atom): Window {
                val defaultResponse = super.getSelectionOwner(atom)
                return if (atom == atomMap["WM_S${defaultScreenNumber()}"]) {
                    21.convert() // this screen is taken
                } else {
                    defaultResponse
                }
            }
        }

        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls
            .dropWhile { it.name != "getSelectionOwner" }
            .toMutableList()

        checkThatTheLoggerIsClosed(logger)
        checkRequestForCurrentSelectionOwner(functionCalls)
        checkThatSupportWindowWasDestroyed(functionCalls)
        checkThatTheDisplayWasClosed(functionCalls)

        checkThatThereIsNoUnexpectedInteraction(functionCalls)
    }

    @Test
    fun `shutdown when the wm can not become screen owner`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            var selectionOwnerCounter = 0

            var window: Window = 123.convert()

            override fun createSimpleWindow(parentWindow: Window, measurements: List<Int>): Window {
                return window++
            }

            override fun createWindow(
                parentWindow: Window,
                measurements: List<Int>,
                visual: CPointer<Visual>?,
                attributeMask: ULong,
                attributes: CPointer<XSetWindowAttributes>
            ): Window {
                return window++
            }

            override fun defaultScreenNumber(): Int = 23

            override fun getSelectionOwner(atom: Atom): Window {
                return if (atom == atomMap["WM_S${defaultScreenNumber()}"]) {
                    super.functionCalls.add(FunctionCall("getSelectionOwner", atom, selectionOwnerCounter++))
                    None.convert()
                } else {
                    super.getSelectionOwner(atom)
                }
            }
        }

        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls
            .dropWhile { it.name != "getSelectionOwner" || it.parameters.elementAtOrNull(1) != 1 }
            .toMutableList()

        checkThatTheLoggerIsClosed(logger)
        checkRequestForCurrentSelectionOwner(functionCalls)
        checkThatSupportWindowWasDestroyed(functionCalls)
        checkThatTheDisplayWasClosed(functionCalls)

        checkThatThereIsNoUnexpectedInteraction(functionCalls)
    }

    @Test
    fun `shutdown when there is an error response for select input`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            private lateinit var errorHandler: XErrorHandler

            override fun setErrorHandler(handler: XErrorHandler): XErrorHandler? {
                this.errorHandler = handler
                return super.setErrorHandler(handler)
            }

            override fun sync(discardQueuedEvents: Boolean): Int {
                this.errorHandler.invoke(null, null)
                return super.sync(discardQueuedEvents)
            }
        }
        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls
            .dropWhile { it.name != "setErrorHandler" }
            .toMutableList()

        checkThatTheLoggerIsClosed(logger)
        checkSettingOfErrorHandler(functionCalls)
        checkSelectInputSetting(functionCalls, ROOT_WINDOW_MASK)
        checkSynchronizationRequest(functionCalls)
        checkThatSupportWindowWasDestroyed(functionCalls)
        checkThatTheDisplayWasClosed(functionCalls)

        checkThatThereIsNoUnexpectedInteraction(functionCalls)
    }

    @Test
    fun `shutdown after sending shutdown key combo`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            val modifiers = UByteArray(8) { 1.shl(it).convert() }

            val winModifierPosition = 6

            val KEY_SYMS = mapOf(
                Pair(XK_Tab, 0),
                Pair(XK_Up, 1),
                Pair(XK_Down, 2),
                Pair(XK_M, 3),
                Pair(XK_Q, 4),
                Pair(XK_F4, 5),
                Pair(XK_T, 6),
                Pair(XK_B, 7),
                Pair(XK_I, 8),
                Pair(XF86XK_AudioMute, 9),
                Pair(XF86XK_AudioLowerVolume, 10),
                Pair(XF86XK_AudioRaiseVolume, 11)
            )

            override fun nextEvent(event: CPointer<XEvent>): Int {
                super.nextEvent(event)
                event.pointed.type = KeyRelease
                event.pointed.xkey.keycode = KEY_SYMS.getValue(XK_Q).convert()
                event.pointed.xkey.state = modifiers[winModifierPosition].convert()
                return 0
            }

            override fun getModifierMapping(): CPointer<XModifierKeymap>? {
                val keymap = nativeHeap.alloc<XModifierKeymap>()
                keymap.max_keypermod = 1
                keymap.modifiermap = modifiers.pin().addressOf(0)
                return keymap.ptr
            }

            override fun keysymToKeycode(keySym: KeySym): KeyCode {
                return KEY_SYMS[keySym.convert()]?.convert() ?: error("keySym not found")
            }
        }

        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls
            .dropWhile { it.name != "nextEvent" }
            .toMutableList()

        checkThatTheLoggerIsClosed(logger)
        checkRequestOfNextEvent(functionCalls)
        checkFreeingOfColors(functionCalls)
        checkFreeingOfColorMap(functionCalls)
        checkSelectInputSetting(functionCalls, NoEventMask)
        checkWindowPropertyRemoval(functionCalls, testFacade.atomMap, "_NET_SUPPORTED")
        checkThatSupportWindowWasDestroyed(functionCalls)
        checkThatTheDisplayWasClosed(functionCalls)

        checkThatThereIsNoUnexpectedInteraction(functionCalls)
    }

    private fun checkThatTheLoggerIsClosed(logger: LoggerMock) {
        assertTrue(logger.closed, "The logger needs to be closed")
    }

    private fun checkForTryingToOpenDisplay(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "openDisplay",
            functionCalls.removeAt(0).name,
            "startup should try to get display"
        )
    }

    private fun checkRequestForDisplaysDefaultScreen(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "defaultScreenOfDisplay",
            functionCalls.removeAt(0).name,
            "startup needs to request the default display for the screen"
        )
    }

    private fun checkRequestForCurrentSelectionOwner(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "getSelectionOwner",
            functionCalls.removeAt(0).name,
            "should call getSelectionOwner to check if other WM is active"
        )
    }

    private fun checkSettingOfErrorHandler(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "setErrorHandler",
            functionCalls.removeAt(0).name,
            "startup should set an error handler to get notified if another WM is already active (selected the input on the root window)"
        )
    }

    private fun checkSelectInputSetting(
        functionCalls: MutableList<FunctionCall>,
        mask: Long
    ) {
        val selectInputCall = functionCalls.removeAt(0)
        assertEquals(
            "selectInput",
            selectInputCall.name,
            "selectInput needs to be called"
        )
        assertEquals(
            mask,
            selectInputCall.parameters[1],
            "selectInput should be called with the mask $mask"
        )
    }

    private fun checkWindowPropertyRemoval(
        functionCalls: MutableList<FunctionCall>,
        atomMap: Map<String, Atom>,
        propertyName: String
    ) {
        val deletePropertyCall = functionCalls.removeAt(0)
        assertEquals(
            "deleteProperty",
            deletePropertyCall.name,
            "We need to call deleteProperty to unset $propertyName"
        )
        assertEquals(
            atomMap[propertyName],
            deletePropertyCall.parameters[1],
            "The property to delete is $propertyName"
        )
    }

    private fun checkSynchronizationRequest(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "sync",
            functionCalls.removeAt(0).name,
            "startup should sync after select input to get notified for other WMs"
        )
    }

    private fun checkRequestOfNextEvent(functionCalls: MutableList<FunctionCall>) {
        assertEquals("nextEvent", functionCalls.removeAt(0).name, "The window manager should react to events")
    }

    private fun checkFreeingOfColors(functionCalls: MutableList<FunctionCall>) {
        assertEquals("freeColors", functionCalls.removeAt(0).name, "the acquired colors need to be freed on shutdown")
    }

    private fun checkFreeingOfColorMap(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "freeColormap",
            functionCalls.removeAt(0).name,
            "the acquired color map needs to be freed on shutdown"
        )
    }

    private fun checkThatSupportWindowWasDestroyed(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "destroyWindow",
            functionCalls.removeAt(0).name,
            "net wm support window needs to be destroyed"
        )
    }

    private fun checkThatTheDisplayWasClosed(functionCalls: MutableList<FunctionCall>) {
        assertEquals(
            "closeDisplay",
            functionCalls.removeAt(0).name,
            "the display needs to be closed when shutting down due to no default screen"
        )
    }

    private fun checkThatThereIsNoUnexpectedInteraction(functionCalls: MutableList<FunctionCall>) {
        assertTrue(
            functionCalls.isEmpty(),
            "There should be no more calls to the system, when we can't get a display"
        )
    }
}