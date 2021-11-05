package de.atennert.lcarswm.window

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.conversion.toULongArray
import de.atennert.lcarswm.system.WindowUtilApiMock
import kotlinx.cinterop.convert
import xlib.Atom
import xlib.Window
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowListAtomHandlerTest {

    private val windowUtilApi = object : WindowUtilApiMock() {
        var windows = ubyteArrayOf()
            private set

        fun reset() {
            windows = ubyteArrayOf()
        }

        override fun changeProperty(
            window: Window,
            propertyAtom: Atom,
            typeAtom: Atom,
            data: UByteArray?,
            format: Int,
            mode: Int
        ): Int {
            data?.let { windows = it }
            return 1
        }

        override fun internAtom(name: String, onlyIfExists: Boolean): Atom {
            return 2.convert()
        }
    }

    @BeforeTest
    fun setup() {
        windowUtilApi.reset()
    }

    @Test
    fun `add one window to list`() {
        val handler = WindowListAtomHandler(1.convert(), windowUtilApi, AtomLibrary(windowUtilApi))

        handler.windowAdded(FramedWindow(42.convert(), 100))

        assertEquals(42.toULong(), windowUtilApi.windows.toULongArray()[0])
    }

    @Test
    fun `add two windows to list`() {
        val handler = WindowListAtomHandler(1.convert(), windowUtilApi, AtomLibrary(windowUtilApi))

        handler.windowAdded(FramedWindow(42.convert(), 100))
        handler.windowAdded(FramedWindow(21.convert(), 100))

        assertEquals(8, windowUtilApi.windows.size)
        assertEquals(42.toULong(), windowUtilApi.windows.toULongArray()[0])
        assertEquals(21.toULong(), windowUtilApi.windows.toULongArray()[1])
    }

    @Test
    fun `remove window from list`() {
        val handler = WindowListAtomHandler(1.convert(), windowUtilApi, AtomLibrary(windowUtilApi))
        handler.windowAdded(FramedWindow(42.convert(), 100))
        handler.windowAdded(FramedWindow(21.convert(), 100))

        handler.windowRemoved(FramedWindow(42.convert(), 100))

        assertEquals(4, windowUtilApi.windows.size)
        assertEquals(21.toULong(), windowUtilApi.windows.toULongArray()[0])
    }

    @Test
    fun `remove last window from list`() {
        val handler = WindowListAtomHandler(1.convert(), windowUtilApi, AtomLibrary(windowUtilApi))
        handler.windowAdded(FramedWindow(42.convert(), 100))

        handler.windowRemoved(FramedWindow(42.convert(), 100))

        assertEquals(0, windowUtilApi.windows.size)
    }
}