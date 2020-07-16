package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.WINDOW_TITLE_FONT_SIZE
import de.atennert.lcarswm.lifecycle.closeWith
import de.atennert.lcarswm.settings.GeneralSetting
import de.atennert.lcarswm.system.api.FontApi
import kotlinx.cinterop.nativeHeap
import xlib.*

class FontProvider(
    private val fontApi: FontApi,
    private val generalSettings: Map<GeneralSetting, String>,
    screenId: Int
) {
    val pango = fontApi.xftGetContext(screenId)
    val layout = fontApi.newLayout(pango)
    val font = fontApi.getFontDescription()

    val ascent: Int
    val descent: Int

    init {
        val ascDesc = initializeFontObjects()
        ascent = ascDesc.first
        descent = ascDesc.second

        closeWith(FontProvider::close)
    }

    private fun initializeFontObjects(): Pair<Int, Int> {
        val lang = fontApi.getDefaultLanguage()
        fontApi.setFontDescriptionFamily(font, generalSettings.getValue(GeneralSetting.FONT))
        fontApi.setFontDescriptionWeight(font, PANGO_WEIGHT_BOLD)
        fontApi.setFontDescriptionStyle(font, PangoStyle.PANGO_STYLE_NORMAL)
        fontApi.setFontDescriptionSize(font, WINDOW_TITLE_FONT_SIZE * PANGO_SCALE)

        fontApi.setLayoutFontDescription(layout, font)
        fontApi.setLayoutWrapMode(layout, PangoWrapMode.PANGO_WRAP_WORD_CHAR)
        fontApi.setLayoutEllipsizeMode(layout, PangoEllipsizeMode.PANGO_ELLIPSIZE_END)
        fontApi.setLayoutSingleParagraphMode(layout, true)

        val metrics = fontApi.getFontMetrics(pango, font, lang)
        val ascDesc = fontApi.getFontAscentDescent(metrics)
        fontApi.freeFontMetrics(metrics)

        return ascDesc
    }

    private fun close() {
        layout?.let { nativeHeap.free(it.rawValue) }
        fontApi.freeFontDescription(font)
        pango?.let { nativeHeap.free(it.rawValue) }
    }
}