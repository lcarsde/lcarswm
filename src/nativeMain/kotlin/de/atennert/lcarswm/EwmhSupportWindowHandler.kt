package de.atennert.lcarswm

import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

class EwmhSupportWindowHandler(
    private val system: SystemApi,
    private val rootWindow: Window,
    rootVisual: CPointer<Visual>?
) {
    val ewmhSupportWindow: Window

    private val longSizeInBytes = 4

    private val windowAtom = system.internAtom("WINDOW")
    private val atomAtom = system.internAtom("ATOM")
    private val utf8Atom = system.internAtom("UTF8_STRING")
    private val netWmName = system.internAtom("_NET_WM_NAME")
    private val netSupportedAtom = system.internAtom("_NET_SUPPORTED")
    private val netSupportWmCheckAtom = system.internAtom("_NET_SUPPORTING_WM_CHECK")

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
        val ewmhWindowInBytes = ewmhSupportWindow.toUByteArray()

        system.changeProperty(rootWindow, netSupportWmCheckAtom, windowAtom, ewmhWindowInBytes, 32)
        system.changeProperty(ewmhSupportWindow, netSupportWmCheckAtom, windowAtom, ewmhWindowInBytes, 32)

        system.changeProperty(ewmhSupportWindow, netWmName, utf8Atom, "LCARSWM".toUByteArray(), 8)

        system.changeProperty(rootWindow, netSupportedAtom, atomAtom, getSupportedProperties(), 32)
    }

    private fun getSupportedProperties(): UByteArray {
        val supportedProperties = ulongArrayOf(netSupportWmCheckAtom,
            netWmName)

        val byteCount = supportedProperties.size * longSizeInBytes
        val propertyBytes = supportedProperties.map { it.toUByteArray() }

        return UByteArray(byteCount) {propertyBytes[it.div(longSizeInBytes)][it.rem(longSizeInBytes)]}
    }

    fun unsetWindowProperties() {
        system.deleteProperty(rootWindow, netSupportedAtom)
    }

    fun destroySupportWindow() {
        system.destroyWindow(ewmhSupportWindow)
    }
}
