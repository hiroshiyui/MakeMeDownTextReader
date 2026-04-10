package org.ghostsinthelab.app.makedown.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Solarized palette by Ethan Schoonover.
 * https://ethanschoonover.com/solarized/
 *
 * The base0x values form an eight-step monotone ramp used for background
 * and text, flipped depending on whether the theme is light or dark. The
 * accent colors are the same across both modes.
 */

// Monotone ramp — light → dark
val SolarizedBase3  = Color(0xFFFDF6E3) // lightest  (light bg)
val SolarizedBase2  = Color(0xFFEEE8D5) // light bg highlights
val SolarizedBase1  = Color(0xFF93A1A1) // optional emphasized content (light bg)
val SolarizedBase0  = Color(0xFF839496) // body text on dark bg
val SolarizedBase00 = Color(0xFF657B83) // body text on light bg
val SolarizedBase01 = Color(0xFF586E75) // optional emphasized content (dark bg)
val SolarizedBase02 = Color(0xFF073642) // dark bg highlights
val SolarizedBase03 = Color(0xFF002B36) // darkest   (dark bg)

// Accent colors
val SolarizedYellow  = Color(0xFFB58900)
val SolarizedOrange  = Color(0xFFCB4B16)
val SolarizedRed     = Color(0xFFDC322F)
val SolarizedMagenta = Color(0xFFD33682)
val SolarizedViolet  = Color(0xFF6C71C4)
val SolarizedBlue    = Color(0xFF268BD2)
val SolarizedCyan    = Color(0xFF2AA198)
val SolarizedGreen   = Color(0xFF859900)
