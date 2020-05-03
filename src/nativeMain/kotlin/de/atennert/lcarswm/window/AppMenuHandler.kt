package de.atennert.lcarswm.window

import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.monitor.MonitorObserver
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.None
import xlib.NormalState
import xlib.Window
import xlib.XTextProperty

class AppMenuHandler (
    private val systemApi: SystemApi,
    private val atomLibrary: AtomLibrary,
    private val monitorManager: MonitorManager
) : MonitorObserver {
    private val wmStateData = listOf<ULong>(NormalState.convert(), None.convert())
        .map { it.toUByteArray() }
        .combine()

    fun manageWindow(windowId: Window) {
        val monitor = monitorManager.getPrimaryMonitor()

        systemApi.moveResizeWindow(
            windowId,
            monitor.x,
            monitor.y + NORMAL_WINDOW_UPPER_OFFSET,
            SIDE_BAR_WIDTH.convert(),
            (monitor.height - NORMAL_WINDOW_NON_APP_HEIGHT).convert()
        )
        systemApi.ungrabServer()
        systemApi.mapWindow(windowId)
        systemApi.changeProperty(windowId, atomLibrary[Atoms.WM_STATE], atomLibrary[Atoms.WM_STATE], wmStateData, 32)
    }

    fun isAppSelector(windowId: Window): Boolean {
        val textProperty = nativeHeap.alloc<XTextProperty>()
        val result = systemApi.getTextProperty(windowId, textProperty.ptr, atomLibrary[Atoms.LCARSWM_APP_SELECTOR])
        systemApi.free(textProperty.value)
        nativeHeap.free(textProperty)
        return result != 0
    }

    override fun toggleScreenMode(newScreenMode: ScreenMode) {

    }
}