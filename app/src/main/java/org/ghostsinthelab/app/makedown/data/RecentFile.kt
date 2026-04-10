package org.ghostsinthelab.app.makedown.data

import kotlinx.serialization.Serializable

@Serializable
data class RecentFile(
    val uri: String,
    val displayName: String,
    val type: DocumentType,
    val lastOpenedAt: Long,
)
