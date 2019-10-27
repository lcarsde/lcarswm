package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

class EwmhSupportWindowHandler(
    private val system: SystemApi,
    private val rootWindow: Window,
    rootVisual: CPointer<Visual>?
) {
    val ewmhSupportWindow: Window

    private val windowAtom = system.internAtom("WINDOW", false)
    private val atomAtom = system.internAtom("ATOM", false)
    private val utf8Atom = system.internAtom("UTF8_STRING", false)
    private val netWmName = system.internAtom("_NET_WM_NAME", false)
    private val netSupportedAtom = system.internAtom("_NET_SUPPORTED", false)
    private val netSupportWmCheckAtom = system.internAtom("_NET_SUPPORTING_WM_CHECK", false)

    init {
        val windowAttributes = nativeHeap.alloc<XSetWindowAttributes>()
        windowAttributes.override_redirect = X_TRUE
        windowAttributes.event_mask = PropertyChangeMask

        this.ewmhSupportWindow = system.createWindow(
            rootWindow,
            listOf(-100, -100, 1, 1),
            rootVisual,
            (CWEventMask or CWOverrideRedirect).convert(),
            windowAttributes.ptr
        )

        system.mapWindow(this.ewmhSupportWindow)
        system.lowerWindow(this.ewmhSupportWindow)
    }

    fun setSupportWindowProperties() {
        val windowVar = nativeHeap.alloc<ULongVar>()
        windowVar.value = ewmhSupportWindow
        val windowInBytes = windowVar.ptr.readBytes(8).asUByteArray()

        system.changeProperty(rootWindow, netSupportWmCheckAtom, windowAtom, windowInBytes, 32)
        system.changeProperty(ewmhSupportWindow, netSupportWmCheckAtom, windowAtom, windowInBytes, 32)

        system.changeProperty(ewmhSupportWindow, netWmName, utf8Atom, "LCARSWM".encodeToByteArray().asUByteArray(), 8);

        system.changeProperty(rootWindow, netSupportedAtom, atomAtom, ubyteArrayOf(), 32);
    }

    fun destroySupportWindow() {
        system.destroyWindow(ewmhSupportWindow)
    }

    fun unsetWindowProperties() {
        system.deleteProperty(rootWindow, netSupportedAtom)
    }
}
