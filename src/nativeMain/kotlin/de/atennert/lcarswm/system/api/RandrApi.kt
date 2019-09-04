package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.IntVar
import xlib.*

/**
 *
 */
interface RandrApi {
    fun rQueryExtension(display: CValuesRef<Display>, eventBase: CValuesRef<IntVar>, errorBase: CValuesRef<IntVar>): Int

    fun rSelectInput(display: CValuesRef<Display>, window: Window, mask: Int)

    fun rGetScreenResources(display: CValuesRef<Display>, window: Window): CPointer<XRRScreenResources>?

    fun rGetOutputPrimary(display: CValuesRef<Display>, window: Window): RROutput

    fun rGetOutputInfo(display: CValuesRef<Display>, resources: CPointer<XRRScreenResources>, output: RROutput): CPointer<XRROutputInfo>?

    fun rGetCrtcInfo(display: CValuesRef<Display>, resources: CPointer<XRRScreenResources>, crtc: RRCrtc): CPointer<XRRCrtcInfo>?
}