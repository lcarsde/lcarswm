import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.FunctionCall
import de.atennert.lcarswm.system.LoggingSystemFacadeMock
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShutdownTest {
    @Test
    fun `shutdown when there's no display to get`() {
        val logger = LoggerMock()
        val testFacade = object : LoggingSystemFacadeMock() {
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
        val testFacade = object : LoggingSystemFacadeMock() {
            override fun defaultScreenOfDisplay(): CPointer<Screen>? {
                super.functionCalls.add(FunctionCall("defaultScreenOfDisplay"))
                return null
            }
        }
        runWindowManager(testFacade, logger)

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertEquals("defaultScreenOfDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get the default screen")
        assertEquals("closeDisplay", testFacade.functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to no default screen")

        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }

    @Test
    fun `shutdown when there is an error response for select input`() {
        val logger = LoggerMock()
        val testFacade = object : LoggingSystemFacadeMock() {
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

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("openDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get display")
        assertEquals("defaultScreenOfDisplay", testFacade.functionCalls.removeAt(0).name, "startup should try to get the default screen")
//        assertEquals("createWindow", testFacade.functionCalls.removeAt(0).name, "")
//        assertEquals("mapWindow", testFacade.functionCalls.removeAt(0).name, "")
//        assertEquals("", testFacade.functionCalls.removeAt(0).name, "")
        assertEquals("setErrorHandler", testFacade.functionCalls.removeAt(0).name, "startup should set an error handler to get notified if another WM is already active (selected the input on the root window)")
        assertEquals("selectInput", testFacade.functionCalls.removeAt(0).name, "startup should try to select the input on the root window")
        assertEquals("sync", testFacade.functionCalls.removeAt(0).name, "startup should sync after select input to get notified for other WMs")
//        assertEquals("destroyWindow", testFacade.functionCalls.removeAt(0).name, "")
        assertEquals("closeDisplay", testFacade.functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to another active WM")

        assertTrue(testFacade.functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }

    @Test
    fun `shutdown after sending shutdown key combo`() {
        val logger = LoggerMock()
        val testFacade = object : LoggingSystemFacadeMock() {
            val modifiers = byteArrayOf(0, 1, 2, 4, 8, 16, 32, 64)

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
                keymap.modifiermap = modifiers.toUByteArray().pin().addressOf(0)
                return keymap.ptr
            }

            override fun keysymToKeycode(keySym: KeySym): KeyCode {
                return KEY_SYMS[keySym.convert()]?.convert() ?: error("keySym not found")
            }
        }

        runWindowManager(testFacade, logger)

        val functionCalls = testFacade.functionCalls.dropWhile { it.name != "nextEvent" }.drop(1).toMutableList()

        assertTrue(logger.closed, "The logger needs to be closed")
        assertEquals("freeColors", functionCalls.removeAt(0).name, "the acquired colors need to be freed on shutdown")
        assertEquals("freeColormap", functionCalls.removeAt(0).name, "the acquired color map needs to be freed on shutdown")
        assertEquals("closeDisplay", functionCalls.removeAt(0).name, "the display needs to be closed when shutting down due to another active WM")

        assertTrue(functionCalls.isEmpty(), "There should be no more calls to the system after the display is closed")
    }
}