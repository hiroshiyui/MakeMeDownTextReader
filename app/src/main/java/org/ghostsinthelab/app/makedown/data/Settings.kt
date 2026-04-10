package org.ghostsinthelab.app.makedown.data

import kotlinx.serialization.Serializable

/** Persisted user preferences. Grown as more settings are added. */
@Serializable
data class Settings(
    val readerFont: ReaderFont = ReaderFont.DEFAULT,
)
