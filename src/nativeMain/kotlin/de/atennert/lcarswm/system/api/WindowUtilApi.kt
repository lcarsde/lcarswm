package de.atennert.lcarswm.system.api

import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
interface WindowUtilApi {
    fun openDisplay(name: String?): CPointer<Display>?

    fun closeDisplay(display: CValuesRef<Display>): Int

    fun defaultScreenOfDisplay(display: CValuesRef<Display>): CPointer<Screen>?

    fun grabServer(display: CValuesRef<Display>): Int

    fun ungrabServer(display: CValuesRef<Display>): Int

    fun addToSaveSet(display: CValuesRef<Display>, window: Window): Int

    fun removeFromSaveSet(display: CValuesRef<Display>, window: Window): Int

    fun queryTree(display: CValuesRef<Display>, window: Window, rootReturn: CValuesRef<WindowVar>, parentReturn: CValuesRef<WindowVar>, childrenReturn: CValuesRef<CPointerVar<WindowVar>>, childrenReturnCounts: CValuesRef<UIntVar>): Int

    fun getWindowAttributes(display: CValuesRef<Display>, window: Window, attributes: CValuesRef<XWindowAttributes>): Int

    fun getWMProtocols(display: CValuesRef<Display>, window: Window, protocolsReturn: CValuesRef<CPointerVar<AtomVar>>, protocolCountReturn: CValuesRef<IntVar>): Int

    fun setErrorHandler(handler: XErrorHandler): XErrorHandler?

    fun internAtom(display: CValuesRef<Display>, name: String, onlyIfExists: Boolean): Atom

    fun killClient(display: CValuesRef<Display>, window: Window): Int
}