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

/**
 * In-memory metadata about a file in the app-private documents store.
 *
 * Not serialized — reconstructed on demand from [java.io.File] listings
 * so the authoritative state is the file system, not a JSON index.
 */
data class PrivateDocument(
    /** Raw file name on disk, including extension. Acts as the file's id. */
    val fileName: String,
    /** Display label for the UI. Currently equal to [fileName]. */
    val displayName: String,
    val type: DocumentType,
    val lastModified: Long,
    val sizeBytes: Long,
)
