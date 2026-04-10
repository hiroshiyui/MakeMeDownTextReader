package org.ghostsinthelab.app.makedown.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/**
 * The font family to use for document reading surfaces.
 *
 * Provided at the root of the composition from the persisted settings. Reader
 * composables should read this for prose text; code blocks and inline code
 * stay monospace regardless.
 */
val LocalReaderFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }
