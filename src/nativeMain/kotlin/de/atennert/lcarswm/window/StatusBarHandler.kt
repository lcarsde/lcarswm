package de.atennert.lcarswm.window

import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.events.sendConfigureNotify
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorObserver
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.None
import xlib.NormalState
import xlib.Window
import xlib.XTextProperty

class StatusBarHandler(
        private val systemApi: SystemApi,
        private val atomLibrary: AtomLibrary,
        private val monitorManager: MonitorManager,
        private val rootWindowId: Window
) : MonitorObserver {
    private val wmStateData = listOf<ULong>(NormalState.convert(), None.convert())
        .map { it.toUByteArray() }
        .combine()

    private var window: FramedWindow? = null

    fun manageWindow(window: FramedWindow) {
        this.window = window
        val measurements = getWindowMeasurements()

        window.frame = systemApi.createSimpleWindow(
            rootWindowId,
            listOf(measurements.x, measurements.y, measurements.width, measurements.frameHeight)
        )

        systemApi.reparentWindow(window.id, window.frame, 0, 0)
        systemApi.resizeWindow(window.id, measurements.width.convert(), measurements.height.convert())

        systemApi.ungrabServer()
        if (monitorManager.getScreenMode() == ScreenMode.NORMAL) {
            showStatusBar(window)
        }
        systemApi.changeProperty(window.id, atomLibrary[Atoms.WM_STATE], atomLibrary[Atoms.WM_STATE], wmStateData, 32)
    }

    fun isStatusBar(windowId: Window): Boolean {
        val textProperty = nativeHeap.alloc<XTextProperty>()
        val result = systemApi.getTextProperty(windowId, textProperty.ptr, atomLibrary[Atoms.LCARSWM_STATUS_BAR])
        systemApi.xFree(textProperty.value)
        nativeHeap.free(textProperty)
        return result != 0
    }

    override fun toggleScreenMode(newScreenMode: ScreenMode) {
        window?.let { window ->
            if (newScreenMode == ScreenMode.NORMAL) {
                showStatusBar(window)
            } else {
                hideStatusBar(window)
            }
        }
    }

    private fun showStatusBar(window: FramedWindow) {
        systemApi.mapWindow(window.frame)
        systemApi.mapWindow(window.id)
    }

    private fun hideStatusBar(window: FramedWindow) {
        systemApi.unmapWindow(window.id)
        systemApi.unmapWindow(window.frame)
    }

    override fun updateMonitors() {
        window?.let { window ->
            resizeWindow(window)
        }
    }

    private fun resizeWindow(window: FramedWindow) {
        val measurements = getWindowMeasurements()

        systemApi.moveResizeWindow(
            window.frame,
            measurements.x,
            measurements.y,
            measurements.width.convert(),
            measurements.frameHeight.convert()
        )

        systemApi.resizeWindow(
            window.id,
            measurements.width.convert(),
            measurements.height.convert()
        )

        sendConfigureNotify(systemApi, window.id, measurements)
    }

    fun isKnownStatusBar(windowId: Window): Boolean {
        return window?.id == windowId
    }

    fun getWindowMeasurements(): WindowMeasurements {
        val monitor = monitorManager.getPrimaryMonitor()

        return WindowMeasurements(
            monitor.x + SIDE_BAR_WIDTH + BAR_GAP_SIZE,
            monitor.y + BAR_HEIGHT + BAR_GAP_SIZE,
            (monitor.width - SIDE_BAR_WIDTH - 2 * BAR_END_WIDTH - 2 * BAR_GAP_SIZE).convert(),
            (DATA_AREA_HEIGHT).convert(),
            (DATA_AREA_HEIGHT).convert()
        )
    }

    fun removeWindow() {
        window?.let { window ->
            systemApi.unmapWindow(window.frame)
            systemApi.flush()

            systemApi.setWindowBorderWidth(window.id, window.borderWidth.convert())

            systemApi.reparentWindow(window.id, rootWindowId, 0, 0)
            systemApi.destroyWindow(window.frame)
        }
        window = null
    }
}