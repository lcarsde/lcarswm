package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import xlib.PangoContext
import xlib.PangoFontDescription
import xlib.PangoLanguage
import xlib.PangoLayout

/**
 * API for font and text handling
 */
interface FontApi {
    fun xftGetContext(screen: Int): CPointer<PangoContext>?

    fun newLayout(pango: CPointer<PangoContext>?): CPointer<PangoLayout>?

    fun getFontDescription(): CPointer<PangoFontDescription>?

    fun getDefaultLanguage(): CPointer<PangoLanguage>?

    fun setFontDescriptionFamily(font: CPointer<PangoFontDescription>?, family: String)
}