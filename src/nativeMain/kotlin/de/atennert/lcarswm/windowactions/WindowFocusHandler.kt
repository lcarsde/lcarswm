package de.atennert.lcarswm.windowactions

import xlib.Window

class WindowFocusHandler {
    fun getFocusedWindow(): Window? {
        return null
    }

    fun registerObserver(observer: (Window?) -> Unit) {
        observer(null)
    }

}
