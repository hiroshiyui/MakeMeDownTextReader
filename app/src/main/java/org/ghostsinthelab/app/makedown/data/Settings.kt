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

import kotlinx.serialization.Serializable

/** Bounds and defaults for the user-configurable base font size, in points. */
const val DEFAULT_READER_BASE_PT: Float = 12f
const val MIN_READER_BASE_PT: Float = 8f
const val MAX_READER_BASE_PT: Float = 32f
const val READER_BASE_PT_STEP: Float = 1f

/** Persisted user preferences. Grown as more settings are added. */
@Serializable
data class Settings(
    val readerFont: ReaderFont = ReaderFont.DEFAULT,
    /** Base font size for document reading, in points.
     *  Acts as both a direct setting (configured from the Settings screen)
     *  and the target of the reader bottom bar's zoom controls.
     *  All reading text sizes scale proportionally against this value. */
    val baseFontSizePt: Float = DEFAULT_READER_BASE_PT,
)
