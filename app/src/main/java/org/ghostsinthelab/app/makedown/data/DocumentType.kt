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

@Serializable
enum class DocumentType {
    PLAIN_TEXT,
    MARKDOWN,
    EPUB;

    companion object {
        fun detect(mimeType: String?, displayName: String?): DocumentType {
            val mt = mimeType?.lowercase()?.trim()
            when (mt) {
                "application/epub+zip", "application/x-epub+zip" -> return EPUB
                "text/markdown", "text/x-markdown" -> return MARKDOWN
                "text/plain" -> {
                    // Some providers report .md as text/plain — check the name.
                    val ext = displayName?.substringAfterLast('.', "")?.lowercase()
                    if (ext in markdownExts) return MARKDOWN
                    return PLAIN_TEXT
                }
            }
            val ext = displayName?.substringAfterLast('.', "")?.lowercase().orEmpty()
            return when (ext) {
                "epub" -> EPUB
                in markdownExts -> MARKDOWN
                "txt", "log", "csv", "tsv", "ini", "conf", "yml", "yaml", "json", "xml", "" -> PLAIN_TEXT
                else -> if (mt?.startsWith("text/") == true) PLAIN_TEXT else PLAIN_TEXT
            }
        }

        private val markdownExts = setOf("md", "markdown", "mdown", "mkd", "mkdn")
    }
}
