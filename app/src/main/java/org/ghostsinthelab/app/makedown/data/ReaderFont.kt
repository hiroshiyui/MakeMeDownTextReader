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
