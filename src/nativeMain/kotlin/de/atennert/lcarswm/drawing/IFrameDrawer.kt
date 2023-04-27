package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.window.WindowMeasurements
import xlib.Colormap
import xlib.Window

interface IFrameDrawer {
    val colorMap: Colormap
    fun drawFrame(measurements: WindowMeasurements, screenMode: ScreenMode, isFocused: Boolean, title: String, titleBar: Window)
}