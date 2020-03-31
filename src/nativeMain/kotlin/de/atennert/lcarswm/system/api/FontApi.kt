package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import xlib.PangoContext

interface FontApi {
    fun xftGetContext(screen: Int): CPointer<PangoContext>?
}