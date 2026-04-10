package org.ghostsinthelab.app.makedown.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified

/**
 * The font family to use for document reading surfaces.
 *
 * Provided at the root of the composition from the persisted settings. Reader
 * composables should read this for prose text; code blocks and inline code
 * stay monospace regardless.
 */
val LocalReaderFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/**
 * Multiplier applied to all reading text sizes, derived from the user's
 * `baseFontSizePt` setting (1.0 at the default of 12 pt). Applied to every
 * reader Text via [scaledBy] so zoom controls and the Settings stepper both
 * feed through the same mechanism.
 */
val LocalReaderFontScale = staticCompositionLocalOf<Float> { 1f }

/** Scale a [TextStyle]'s fontSize and lineHeight by [factor]. */
fun TextStyle.scaledBy(factor: Float): TextStyle {
    if (factor == 1f) return this
    val scaledFontSize = if (fontSize.isSpecified) fontSize * factor else fontSize
    val scaledLineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight
    return copy(fontSize = scaledFontSize, lineHeight = scaledLineHeight)
}

/** Scale a raw [TextUnit] by [factor], leaving unspecified values alone. */
fun TextUnit.scaledBy(factor: Float): TextUnit =
    if (isUnspecified || factor == 1f) this else this * factor
