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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.ghostsinthelab.app.makedown.R

/**
 * Vendored Roboto Slab. Used as the **interface** font for all Material 3
 * chrome — top/bottom app bars, buttons, chips, dialogs, the Settings
 * screen, snackbars, recents rows, etc.
 *
 * Reader surfaces (plain text, Markdown rendered view, EPUB body) do NOT
 * use this family. They explicitly override `fontFamily` to the user-
 * selected [org.ghostsinthelab.app.makedown.ui.LocalReaderFontFamily], so
 * changing the reading font from Settings or the reader bottom bar has no
 * effect on the app's chrome and vice versa.
 *
 * The TTF files live under `app/src/main/res/font/` and ship in the APK;
 * the Apache 2.0 license text is in `app/src/main/assets/fonts/`.
 */
val RobotoSlab = FontFamily(
    Font(R.font.roboto_slab_regular, FontWeight.Normal),
    Font(R.font.roboto_slab_medium, FontWeight.Medium),
    Font(R.font.roboto_slab_bold, FontWeight.Bold),
)

/**
 * Material 3 default [Typography] rebuilt with [RobotoSlab] as the family
 * for every role. We keep Material's default sizes, line heights, letter
 * spacing, and weight choices per role — only the family changes.
 */
val Typography: Typography = run {
    val base = Typography()
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = RobotoSlab),
        displayMedium = base.displayMedium.copy(fontFamily = RobotoSlab),
        displaySmall = base.displaySmall.copy(fontFamily = RobotoSlab),
        headlineLarge = base.headlineLarge.copy(fontFamily = RobotoSlab),
        headlineMedium = base.headlineMedium.copy(fontFamily = RobotoSlab),
        headlineSmall = base.headlineSmall.copy(fontFamily = RobotoSlab),
        titleLarge = base.titleLarge.copy(fontFamily = RobotoSlab),
        titleMedium = base.titleMedium.copy(fontFamily = RobotoSlab),
        titleSmall = base.titleSmall.copy(fontFamily = RobotoSlab),
        bodyLarge = base.bodyLarge.copy(fontFamily = RobotoSlab),
        bodyMedium = base.bodyMedium.copy(fontFamily = RobotoSlab),
        bodySmall = base.bodySmall.copy(fontFamily = RobotoSlab),
        labelLarge = base.labelLarge.copy(fontFamily = RobotoSlab),
        labelMedium = base.labelMedium.copy(fontFamily = RobotoSlab),
        labelSmall = base.labelSmall.copy(fontFamily = RobotoSlab),
    )
}
