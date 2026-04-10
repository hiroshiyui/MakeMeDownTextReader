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

package org.ghostsinthelab.app.makedown.reader.epub

import androidx.compose.ui.text.AnnotatedString

/** Block-level structure for an EPUB chapter, mirroring Markdown's MdBlock. */
sealed interface EpubBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : EpubBlock
    data class Paragraph(val text: AnnotatedString) : EpubBlock
    data class BulletList(val items: List<List<EpubBlock>>) : EpubBlock
    data class OrderedList(val items: List<List<EpubBlock>>, val start: Int = 1) : EpubBlock
    data class BlockQuote(val children: List<EpubBlock>) : EpubBlock
    data class CodeBlock(val text: String) : EpubBlock
    data object HorizontalRule : EpubBlock
    /** Resolved zip-entry path of the image relative to the OPF root. */
    data class Image(val resourcePath: String, val alt: String?) : EpubBlock
}

data class EpubChapter(
    val id: String,
    val title: String,
    val blocks: List<EpubBlock>,
)

data class EpubBook(
    val title: String,
    val author: String?,
    val chapters: List<EpubChapter>,
    /** Map from normalized zip entry path → bytes (used for inline images). */
    val resources: Map<String, ByteArray>,
)
