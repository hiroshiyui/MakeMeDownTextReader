/*
 * MakeMeDown Text Reader
 * Copyright (C) 2026 Hui-Hong You
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
