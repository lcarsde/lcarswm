package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.monitor.Monitor
import kotlinx.cinterop.convert
import xlib.Colormap

class FrameDrawerMock : IFrameDrawer {
    override val colorMap: Colormap = 0.convert()

    override fun drawFrame(window: FramedWindow, monitor: Monitor) {
    }
}