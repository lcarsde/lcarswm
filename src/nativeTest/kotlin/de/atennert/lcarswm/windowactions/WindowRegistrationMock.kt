package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.system.FunctionCall
import xlib.Window

/**
 *
 */
class WindowRegistrationMock : WindowRegistrationApi {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun addWindow(windowId: Window, isSetup: Boolean) {
        functionCalls.add(FunctionCall("addWindow", windowId, isSetup))
    }

    override fun isWindowManaged(windowId: Window): Boolean = false
}