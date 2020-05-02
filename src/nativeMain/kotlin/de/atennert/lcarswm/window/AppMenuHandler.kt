package de.atennert.lcarswm.window

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import xlib.Window
import xlib.XTextProperty

class AppMenuHandler(
    private val systemApi: SystemApi,
    private val atomLibrary: AtomLibrary,
    private val monitorManager: MonitorManager
) {

    fun isAppSelector(windowId: Window): Boolean {
        val textProperty = nativeHeap.alloc<XTextProperty>()
        val result = systemApi.getTextProperty(windowId, textProperty.ptr, atomLibrary[Atoms.LCARSWM_APP_SELECTOR])
        systemApi.free(textProperty.value)
        nativeHeap.free(textProperty)
        return result != 0
    }
}