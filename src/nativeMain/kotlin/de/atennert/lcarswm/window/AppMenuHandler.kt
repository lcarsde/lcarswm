package de.atennert.lcarswm.window

import de.atennert.lcarswm.*
import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.conversion.combine
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.events.sendConfigureNotify
import de.atennert.lcarswm.monitor.MonitorManager
import de.atennert.lcarswm.monitor.MonitorObserver
import de.atennert.lcarswm.monitor.WindowMeasurements
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.None
import xlib.NormalState
import xlib.Window
import xlib.XTextProperty

class AppMenuHandler(
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
            showAppMenu(window)
        }
        systemApi.changeProperty(window.id, atomLibrary[Atoms.WM_STATE], atomLibrary[Atoms.WM_STATE], wmStateData, 32)
    }

    fun isAppSelector(windowId: Window): Boolean {
        val textProperty = nativeHeap.alloc<XTextProperty>()
        val result = systemApi.getTextProperty(windowId, textProperty.ptr, atomLibrary[Atoms.LCARSWM_APP_SELECTOR])
        systemApi.free(textProperty.value)
        nativeHeap.free(textProperty)
        return result != 0
    }

    override fun toggleScreenMode(newScreenMode: ScreenMode) {
        window?.let { window ->
            if (newScreenMode == ScreenMode.NORMAL) {
                showAppMenu(window)
            } else {
                hideAppMenu(window)
            }
        }
    }

    private fun showAppMenu(window: FramedWindow) {
        systemApi.mapWindow(window.frame)
        systemApi.mapWindow(window.id)
    }

    private fun hideAppMenu(window: FramedWindow) {
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

    fun isKnownAppMenu(windowId: Window): Boolean {
        return window?.id == windowId
    }

    fun getWindowMeasurements(): WindowMeasurements {
        val monitor = monitorManager.getPrimaryMonitor()

        return WindowMeasurements(
            monitor.x,
            monitor.y + NORMAL_WINDOW_UPPER_OFFSET,
            SIDE_BAR_WIDTH.convert(),
            (monitor.height - NORMAL_WINDOW_NON_APP_HEIGHT).convert(),
            (monitor.height - NORMAL_WINDOW_NON_APP_HEIGHT).convert()
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

    /*###########################################*
     * Communicating window list updates
     *###########################################*/
    val windowListObserver = object : WindowList.Observer {
        override fun windowAdded(window: FramedWindow) {
        }

        override fun windowRemoved(window: FramedWindow) {
        }

        override fun windowUpdated(window: FramedWindow) {
        }
    }
}