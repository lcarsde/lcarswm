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
                super.functionCalls.add(FunctionCall("openDisplay"))
                return false
            }
        }
        runWindowManager(testFacade, logger)

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system, when we can't get a display")
    }

    @Test
    fun `shutdown when there's no default screen to get`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            override fun defaultScreenOfDisplay(): CPointer<Screen>? {
                super.functionCalls.add(FunctionCall("defaultScreenOfDisplay"))
                return null
            }
        }
        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls.dropWhile { it.name != "defaultScreenOfDisplay" }.toMutableList()

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("defaultScreenOfDisplay", functionCalls.removeAt(0).name, "startup needs to request the default display for the screen")
        assertEquals("closeDisplay", functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to no default screen")

        assertTrue(functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }

    @Test
    fun `shutdown when there is already a screen owner`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            override fun defaultScreenNumber(): Int {
                return 23
            }

            override fun internAtom(name: String, onlyIfExists: Boolean): Atom {
                val defaultResponse = super.internAtom(name, onlyIfExists)
                return if (name == "WM_S${defaultScreenNumber()}") {
                    42.convert()
                } else {
                    defaultResponse
                }
            }

            override fun getSelectionOwner(atom: Atom): Window {
                val defaultResponse = super.getSelectionOwner(atom)
                return if (atom.convert<Int>() == 42) {
                    21.convert() // this screen is taken
                } else {
                    defaultResponse
                }
            }
        }

        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls.dropWhile { it.name != "getSelectionOwner" }.toMutableList()

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("getSelectionOwner", functionCalls.removeAt(0).name, "should call getSelectionOwner to check if other WM is active")
        assertEquals("destroyWindow", functionCalls.removeAt(0).name, "net wm support window needs to be destroyed")
        assertEquals("closeDisplay", functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to no default screen")

        assertTrue(functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
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

            override fun defaultScreenNumber(): Int {
                return 23
            }

            override fun internAtom(name: String, onlyIfExists: Boolean): Atom {
                val defaultResponse = super.internAtom(name, onlyIfExists)
                return if (name == "WM_S${defaultScreenNumber()}") {
                    42.convert()
                } else {
                    defaultResponse
                }
            }

            override fun getSelectionOwner(atom: Atom): Window {
                return if (atom.convert<Int>() == 42) {
                    super.functionCalls.add(FunctionCall("getSelectionOwner", atom, selectionOwnerCounter++))
                    None.convert()
                } else {
                    super.getSelectionOwner(atom)
                }
            }
        }

        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls.dropWhile { it.name != "getSelectionOwner" || it.parameters.elementAtOrNull(1) != 1 }.toMutableList()

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("getSelectionOwner", functionCalls.removeAt(0).name, "should call getSelectionOwner to check if we became the active WM")
        assertEquals("destroyWindow", functionCalls.removeAt(0).name, "net wm support window needs to be destroyed")
        assertEquals("closeDisplay", functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to no default screen")

        assertTrue(functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
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

        val functionCalls = testFacade.functionCalls.dropWhile { it.name != "setErrorHandler" }.toMutableList()

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("setErrorHandler", functionCalls.removeAt(0).name, "startup should set an error handler to get notified if another WM is already active (selected the input on the root window)")
        assertEquals("selectInput", functionCalls.removeAt(0).name, "startup should try to select the input on the root window")
        assertEquals("sync", functionCalls.removeAt(0).name, "startup should sync after select input to get notified for other WMs")
        assertEquals("destroyWindow", functionCalls.removeAt(0).name, "net wm support window needs to be destroyed")
        assertEquals("closeDisplay", functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to another active WM")

        assertTrue(functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }

    @Test
    fun `shutdown after sending shutdown key combo`() {
        val logger = LoggerMock()
        val testFacade = object : SystemFacadeMock() {
            val modifiers = UByteArray(8) {1.shl(it).convert()}

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

        val functionCalls = testFacade.functionCalls.dropWhile { it.name != "nextEvent" }.toMutableList()

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("nextEvent", functionCalls.removeAt(0).name, "The window manager should react to events")
        assertEquals("freeColors", functionCalls.removeAt(0).name, "the acquired colors need to be freed on shutdown")
        assertEquals("freeColormap", functionCalls.removeAt(0).name, "the acquired color map needs to be freed on shutdown")
        val selectInputCall = functionCalls.removeAt(0)
        assertEquals("selectInput", selectInputCall.name, "selectInput needs to be called on shutdown, to unselect the input")
        assertEquals(NoEventMask, selectInputCall.parameters[1], "selectInput must not select any input to unselect everything")
        assertEquals("destroyWindow", functionCalls.removeAt(0).name, "net wm support window needs to be destroyed")
        assertEquals("closeDisplay", functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to another active WM")

        assertTrue(functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }
}