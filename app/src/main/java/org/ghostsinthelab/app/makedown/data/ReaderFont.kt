package org.ghostsinthelab.app.makedown.data

import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.Serializable

/** User-selectable font families for the reading surface. */
@Serializable
enum class ReaderFont(val displayName: String) {
    DEFAULT("System default"),
    SANS_SERIF("Sans-Serif"),
    SERIF("Serif"),
    MONOSPACE("Monospace");

    fun toFontFamily(): FontFamily = when (this) {
        DEFAULT -> FontFamily.Default
        SANS_SERIF -> FontFamily.SansSerif
        SERIF -> FontFamily.Serif
        MONOSPACE -> FontFamily.Monospace
    }
}
