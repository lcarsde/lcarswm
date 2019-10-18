import de.atennert.lcarswm.log.LoggerMock
import de.atennert.lcarswm.system.LoggingSystemFacadeMock
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StartupTest {
    @Test
    fun `set DISPLAY environment variable during startup`() {
        val systemFacade = StartupFacadeMock()

        runWindowManager(systemFacade, LoggerMock())

        val startupCalls = systemFacade.functionCalls.takeWhile { it.name != "nextEvent" }

        val setenvCall = startupCalls.singleOrNull { it.name == "setenv" && it.parameters[0] == "DISPLAY" }

        assertNotNull(setenvCall, "setenv should be called to set the DISPLAY name")

        assertEquals(systemFacade.displayString, setenvCall.parameters[1], "the DISPLAY environment variable should be set to the return value of getDisplayString")
    }

    private class StartupFacadeMock : LoggingSystemFacadeMock() {
        val displayString = "displayString"

        val modifiers = UByteArray(8) {1.shl(it).convert()}

        val winModifierPosition = 6

        val keySyms = mapOf(
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
            // send closing key event to stop the window manager
            super.nextEvent(event)
            event.pointed.type = KeyRelease
            event.pointed.xkey.keycode = keySyms.getValue(XK_Q).convert()
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
            return keySyms[keySym.convert()]?.convert() ?: error("keySym not found")
        }

        override fun getDisplayString(): String {
            return this.displayString
        }
    }
}