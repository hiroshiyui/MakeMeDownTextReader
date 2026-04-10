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

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Solarized mapped onto Material 3 color roles.
 *
 * The goal: respect Material 3's semantics (primary is the main brand
 * accent, error is red, surface is a neutral ground, every `onX` pair has
 * usable contrast against `X`, etc.) while pulling every hue from the
 * Solarized palette so the app actually looks Solarized.
 *
 * Mapping rationale:
 *
 * - **Neutrals** — Solarized is built around an eight-step monotone ramp
 *   (base03..base3). Light mode uses `base3` as background/surface and
 *   `base00` as default text; dark mode flips to `base03` / `base0`. The
 *   surfaceVariant role takes the next step inward (`base2` / `base02`),
 *   and the `onSurfaceVariant` text role uses the emphasized-content hue
 *   (`base01` / `base1`). This mirrors Solarized's own recommendations.
 *
 * - **Primary = blue.** Solarized's blue is the canonical accent for UI
 *   chrome (selection, caret, focus) and gives the best contrast against
 *   both the light and dark surfaces. It's also what `surfaceTint` should
 *   be in M3 terms — the color that gets mixed into elevated surfaces.
 *
 * - **Secondary = cyan, tertiary = violet** — picked so that the three
 *   accent roles are visually distinct on both backgrounds.
 *
 * - **Container roles** — Solarized has no intermediate shades between its
 *   base tones and its accents, so `*Container` uses `base2`/`base02` (the
 *   nearest neutral step) and `on*Container` uses the corresponding accent
 *   hue. This keeps tonal buttons, chips, and filled cards feeling
 *   "Solarized" — a muted paper/ink ground with a colored label — instead
 *   of introducing off-palette tints.
 *
 * - **Error = red, errorContainer = base2/base02, onErrorContainer = red.**
 *   Semantics preserved; the red still reads as "something went wrong."
 *
 * - **Outline / outlineVariant** use `base1`/`base01` and `base2`/`base02`
 *   so dividers and component borders sit at a plausible contrast step.
 *
 * - **Inverse roles** swap the background/text across the light/dark
 *   boundary, which is exactly how M3 expects them to work (Snackbars in
 *   a light-themed app show on a dark inverse surface, and vice versa).
 */
private val SolarizedLightColors = lightColorScheme(
    primary = SolarizedBlue,
    onPrimary = SolarizedBase3,
    primaryContainer = SolarizedBase2,
    onPrimaryContainer = SolarizedBlue,

    secondary = SolarizedCyan,
    onSecondary = SolarizedBase3,
    secondaryContainer = SolarizedBase2,
    onSecondaryContainer = SolarizedCyan,

    tertiary = SolarizedViolet,
    onTertiary = SolarizedBase3,
    tertiaryContainer = SolarizedBase2,
    onTertiaryContainer = SolarizedViolet,

    background = SolarizedBase3,
    onBackground = SolarizedBase00,
    surface = SolarizedBase3,
    onSurface = SolarizedBase00,
    surfaceVariant = SolarizedBase2,
    onSurfaceVariant = SolarizedBase01,
    surfaceTint = SolarizedBlue,

    // Surface container roles (M3 2024+) — the palette only has two neutral
    // steps so lower containers share base3 and higher containers share base2.
    // Flat by design; Solarized doesn't use tonal elevation.
    surfaceContainerLowest = SolarizedBase3,
    surfaceContainerLow = SolarizedBase3,
    surfaceContainer = SolarizedBase2,
    surfaceContainerHigh = SolarizedBase2,
    surfaceContainerHighest = SolarizedBase2,
    surfaceBright = SolarizedBase3,
    surfaceDim = SolarizedBase2,

    error = SolarizedRed,
    onError = SolarizedBase3,
    errorContainer = SolarizedBase2,
    onErrorContainer = SolarizedRed,

    outline = SolarizedBase1,
    outlineVariant = SolarizedBase2,

    // Inverse surfaces are used by Snackbars and a few tonal containers.
    // Flip to the dark ramp so they contrast the light background.
    inverseSurface = SolarizedBase02,
    inverseOnSurface = SolarizedBase2,
    inversePrimary = SolarizedBlue,

    scrim = Color.Black,
)

private val SolarizedDarkColors = darkColorScheme(
    primary = SolarizedBlue,
    onPrimary = SolarizedBase03,
    primaryContainer = SolarizedBase02,
    onPrimaryContainer = SolarizedBlue,

    secondary = SolarizedCyan,
    onSecondary = SolarizedBase03,
    secondaryContainer = SolarizedBase02,
    onSecondaryContainer = SolarizedCyan,

    tertiary = SolarizedViolet,
    onTertiary = SolarizedBase03,
    tertiaryContainer = SolarizedBase02,
    onTertiaryContainer = SolarizedViolet,

    background = SolarizedBase03,
    onBackground = SolarizedBase0,
    surface = SolarizedBase03,
    onSurface = SolarizedBase0,
    surfaceVariant = SolarizedBase02,
    onSurfaceVariant = SolarizedBase1,
    surfaceTint = SolarizedBlue,

    surfaceContainerLowest = SolarizedBase03,
    surfaceContainerLow = SolarizedBase03,
    surfaceContainer = SolarizedBase02,
    surfaceContainerHigh = SolarizedBase02,
    surfaceContainerHighest = SolarizedBase02,
    surfaceBright = SolarizedBase02,
    surfaceDim = SolarizedBase03,

    error = SolarizedRed,
    onError = SolarizedBase03,
    errorContainer = SolarizedBase02,
    onErrorContainer = SolarizedRed,

    outline = SolarizedBase01,
    outlineVariant = SolarizedBase02,

    inverseSurface = SolarizedBase2,
    inverseOnSurface = SolarizedBase02,
    inversePrimary = SolarizedBlue,

    scrim = Color.Black,
)

@Composable
fun MakeMeDownTextReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // The whole point of this theme is to ship Solarized, so Material You
    // dynamic color is OFF by default. Callers that explicitly want the
    // system palette can opt in.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SolarizedDarkColors
        else -> SolarizedLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
