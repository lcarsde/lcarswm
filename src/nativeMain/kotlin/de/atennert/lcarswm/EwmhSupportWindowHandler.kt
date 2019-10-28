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
        val ewmhWindowInBytes = ewmhSupportWindow.toUByteArray()

        system.changeProperty(rootWindow, netSupportWmCheckAtom, windowAtom, ewmhWindowInBytes, 32)
        system.changeProperty(ewmhSupportWindow, netSupportWmCheckAtom, windowAtom, ewmhWindowInBytes, 32)

        system.changeProperty(ewmhSupportWindow, netWmName, utf8Atom, "LCARSWM".toUByteArray(), 8)

        system.changeProperty(rootWindow, netSupportedAtom, atomAtom, getSupportedProperties(), 32)
    }

    private fun getSupportedProperties(): UByteArray {
        val supportedProperties = ulongArrayOf(netSupportWmCheckAtom,
            netWmName)

        val propertyCount = supportedProperties.size
        val propertyBytes = supportedProperties.map { it.toUByteArray() }

        return UByteArray(propertyCount * longSizeInBytes) {propertyBytes[it.div(longSizeInBytes)][it.rem(longSizeInBytes)]}
    }

    fun unsetWindowProperties() {
        system.deleteProperty(rootWindow, netSupportedAtom)
    }

    fun destroySupportWindow() {
        system.destroyWindow(ewmhSupportWindow)
    }
}
