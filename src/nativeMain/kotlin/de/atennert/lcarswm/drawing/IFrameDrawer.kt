package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.FramedWindow
import de.atennert.lcarswm.monitor.Monitor
import xlib.Colormap

interface IFrameDrawer {
    val colorMap: Colormap
    fun drawFrame(window: FramedWindow, monitor: Monitor)
}