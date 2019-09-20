package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.ClientMessage
import xlib.Window
import xlib.XEvent

fun closeWindow(
    windowId: Window,
    system: SystemApi,
    windowManagerState: WindowManagerState
) {
    val supportedProtocols = nativeHeap.allocPointerTo<ULongVar>()
    val numSupportedProtocols = IntArray(1).pin()

    val protocolsResult = system.getWMProtocols(windowId, supportedProtocols.ptr, numSupportedProtocols.addressOf(0))

    val min = supportedProtocols.pointed!!.value
    val max = min + numSupportedProtocols.get()[0].toULong()

    if (protocolsResult != 0 && (windowManagerState.wmDeleteWindow in (min .. max))) {
        val msg = nativeHeap.alloc<XEvent>()
        msg.xclient.type = ClientMessage
        msg.xclient.message_type = windowManagerState.wmProtocols
        msg.xclient.window = windowId
        msg.xclient.format = 32
        msg.xclient.data.l[0] = windowManagerState.wmDeleteWindow.convert()
        system.sendEvent(windowId, false, 0, msg.ptr)
    } else {
        system.killClient(windowId)
    }
}
