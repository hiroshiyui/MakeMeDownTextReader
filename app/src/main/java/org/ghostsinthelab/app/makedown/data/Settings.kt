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
