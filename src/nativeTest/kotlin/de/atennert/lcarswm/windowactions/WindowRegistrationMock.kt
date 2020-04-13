package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.system.FunctionCall
import xlib.Window

/**
 * Mock for testing use of the window registration.
 */
open class WindowRegistrationMock : WindowRegistration {
    val functionCalls = mutableListOf<FunctionCall>()
    private val managedWindowIds = mutableListOf<Window>()

    override fun addWindow(windowId: Window, isSetup: Boolean) {
        functionCalls.add(FunctionCall("addWindow", windowId, isSetup))
        managedWindowIds.add(windowId)
    }

    override fun isWindowManaged(windowId: Window): Boolean = managedWindowIds.contains(windowId)

    override fun get(windowId: Window): FramedWindow? = null

    override fun removeWindow(windowId: Window) {
        functionCalls.add(FunctionCall("removeWindow", windowId))
        managedWindowIds.remove(windowId)
    }

    override fun isWindowParentedBy(windowId: Window, parentId: Window): Boolean {
        return true
    }
}