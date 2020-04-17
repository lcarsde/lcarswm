package de.atennert.lcarswm.system.api

import kotlinx.cinterop.CPointer
import xlib.*

/**
 * API for font and text handling
 */
interface FontApi {
    fun xftGetContext(screen: Int): CPointer<PangoContext>?

    fun newLayout(pango: CPointer<PangoContext>?): CPointer<PangoLayout>?

    fun getFontDescription(): CPointer<PangoFontDescription>?

    fun getDefaultLanguage(): CPointer<PangoLanguage>?

    fun setFontDescriptionFamily(font: CPointer<PangoFontDescription>?, family: String)

    fun setFontDescriptionWeight(font: CPointer<PangoFontDescription>?, weight: PangoWeight)

    fun setFontDescriptionStyle(font: CPointer<PangoFontDescription>?, style: PangoStyle)

    fun setFontDescriptionSize(font: CPointer<PangoFontDescription>?, size: Int)

    fun freeFontDescription(font: CPointer<PangoFontDescription>?)

    fun setLayoutFontDescription(layout: CPointer<PangoLayout>?, fontDescription: CPointer<PangoFontDescription>?)

    fun setLayoutWrapMode(layout: CPointer<PangoLayout>?, wrapMode: PangoWrapMode)

    fun setLayoutText(layout: CPointer<PangoLayout>?, text: String)

    fun setLayoutWidth(layout: CPointer<PangoLayout>?, width: Int)

    fun setLayoutEllipsizeMode(layout: CPointer<PangoLayout>?, ellipsizeMode: PangoEllipsizeMode)

    fun setLayoutSingleParagraphMode(layout: CPointer<PangoLayout>?, setting: Boolean)

    fun getLayoutPixelExtents(layout: CPointer<PangoLayout>?, logicalRectangle: CPointer<PangoRectangle>?)

    fun getFontMetrics(context: CPointer<PangoContext>?, font: CPointer<PangoFontDescription>?, language: CPointer<PangoLanguage>?): CPointer<PangoFontMetrics>?

    fun getFontAscentDescent(metrics: CPointer<PangoFontMetrics>?): Pair<Int, Int>

    fun freeFontMetrics(metrics: CPointer<PangoFontMetrics>?)
}