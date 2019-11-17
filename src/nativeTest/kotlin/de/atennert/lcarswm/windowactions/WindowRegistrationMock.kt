package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.system.FunctionCall
import xlib.Window

/**
 *
 */
open class WindowRegistrationMock : WindowRegistrationApi {
    val functionCalls = mutableListOf<FunctionCall>()
    private val managedWindowIds = mutableListOf<Window>()

    override fun addWindow(windowId: Window, isSetup: Boolean) {
        functionCalls.add(FunctionCall("addWindow", windowId, isSetup))
        managedWindowIds.add(windowId)
    }

    override fun isWindowManaged(windowId: Window): Boolean = managedWindowIds.contains(windowId)

    override fun removeWindow(windowId: Window) {
        functionCalls.add(FunctionCall("removeWindow", windowId))
        managedWindowIds.remove(windowId)
    }
}