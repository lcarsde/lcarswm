package de.atennert.lcarswm.system.api

import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
interface WindowUtilApi {
    fun closeDisplay(): Int

    fun defaultScreenOfDisplay(): CPointer<Screen>?

    fun grabServer(): Int

    fun ungrabServer(): Int

    fun addToSaveSet(window: Window): Int

    fun removeFromSaveSet(window: Window): Int

    fun queryTree(window: Window, rootReturn: CValuesRef<WindowVar>, parentReturn: CValuesRef<WindowVar>, childrenReturn: CValuesRef<CPointerVar<WindowVar>>, childrenReturnCounts: CValuesRef<UIntVar>): Int

    fun getWindowAttributes(window: Window, attributes: CValuesRef<XWindowAttributes>): Int

    fun getWMProtocols(window: Window, protocolsReturn: CValuesRef<CPointerVar<AtomVar>>, protocolCountReturn: CValuesRef<IntVar>): Int

    fun setErrorHandler(handler: XErrorHandler): XErrorHandler?

    fun internAtom(name: String, onlyIfExists: Boolean): Atom

    fun changeProperty(window: Window, propertyAtom: Atom, typeAtom: Atom, data: UByteArray?): Int

    fun killClient(window: Window): Int

    fun createSimpleWindow(parentWindow: Window, measurements: List<Int>): Window
}