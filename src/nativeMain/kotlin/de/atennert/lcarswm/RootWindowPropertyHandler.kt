package de.atennert.lcarswm

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms.*
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

class RootWindowPropertyHandler(
    private val system: SystemApi,
    private val rootWindow: Window,
    private val atomLibrary: AtomLibrary,
    rootVisual: CPointer<Visual>?
) {
    private val ewmhSupportWindow: Window

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

    fun becomeScreenOwner(): Boolean {
        val wmSnName = "WM_S${system.defaultScreenNumber()}"
        val wmSn = system.internAtom(wmSnName)

        if (system.getSelectionOwner(wmSn) != None.convert<Window>()) {
            return false
        }

        system.setSelectionOwner(wmSn, ewmhSupportWindow, CurrentTime.convert())

        if (system.getSelectionOwner(wmSn) != ewmhSupportWindow) {
            return false
        }

        return true
    }

    fun setSupportWindowProperties() {
        val ewmhWindowInBytes = ewmhSupportWindow.toUByteArray()

        system.changeProperty(
            rootWindow,
            atomLibrary[NET_SUPPORTING_WM_CHECK],
            atomLibrary[WINDOW],
            ewmhWindowInBytes,
            32
        )
        system.changeProperty(
            ewmhSupportWindow,
            atomLibrary[NET_SUPPORTING_WM_CHECK],
            atomLibrary[WINDOW],
            ewmhWindowInBytes,
            32
        )

        system.changeProperty(
            ewmhSupportWindow,
            atomLibrary[NET_WM_NAME],
            atomLibrary[UTF_STRING],
            "LCARSWM".toUByteArray(),
            8
        )

        system.changeProperty(rootWindow, atomLibrary[NET_SUPPORTED], atomLibrary[ATOM], getSupportedProperties(), 32)
    }

    private fun getSupportedProperties(): UByteArray {
        val supportedProperties = arrayOf(
            NET_SUPPORTING_WM_CHECK,
            NET_WM_NAME
        )

        return supportedProperties
            .map { atomLibrary[it] }
            .map { it.toUByteArray() }
            .combine()
    }

    fun unsetWindowProperties() {
        system.deleteProperty(rootWindow, atomLibrary[NET_SUPPORTED])
    }

    fun destroySupportWindow() {
        system.destroyWindow(ewmhSupportWindow)
    }
}
