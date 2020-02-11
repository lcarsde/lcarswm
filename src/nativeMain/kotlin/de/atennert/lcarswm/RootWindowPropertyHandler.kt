package de.atennert.lcarswm

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms.*
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

/**
 * Class for handling property (atom) related actions.
 */
class RootWindowPropertyHandler(
    private val system: SystemApi,
    private val rootWindow: Window,
    private val atomLibrary: AtomLibrary,
    rootVisual: CPointer<Visual>?
) {
    val ewmhSupportWindow: Window

    init {
        val windowAttributes = nativeHeap.alloc<XSetWindowAttributes>()
        windowAttributes.override_redirect = X_TRUE
        windowAttributes.event_mask = PropertyChangeMask

        this.ewmhSupportWindow = system.createWindow(
            rootWindow,
            listOf(-100, -100, 1, 1),
            CopyFromParent.convert(),
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

        val timeStamp = CurrentTime // TODO get event based timestamp
        system.setSelectionOwner(wmSn, ewmhSupportWindow, timeStamp.convert())

        if (system.getSelectionOwner(wmSn) != ewmhSupportWindow) {
            return false
        }

        sendWmNotification(wmSn, timeStamp)

        return true
    }

    private fun sendWmNotification(wmSn: Atom, timeStamp: Long) {
        val event = nativeHeap.alloc<XEvent>()
        event.xclient.type = ClientMessage
        event.xclient.message_type = atomLibrary[MANAGER]
        event.xclient.display = system.getDisplay()
        event.xclient.window = rootWindow
        event.xclient.format = 32
        event.xclient.data.l[0] = timeStamp
        event.xclient.data.l[1] = wmSn.convert()
        event.xclient.data.l[2] = this.ewmhSupportWindow.convert()
        event.xclient.data.l[3] = 0
        event.xclient.data.l[4] = 0

        system.sendEvent(rootWindow, false, SubstructureNotifyMask, event.ptr)
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
            NET_WM_NAME,
            NET_WM_DESKTOP,
            NET_WM_STATE,
            NET_CLOSE_WINDOW,
            NET_ACTIVE_WINDOW,
            NET_WM_MOVERESIZE,
            NET_MOVERESIZE_WINDOW,
            NET_RESTACK_WINDOW
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
