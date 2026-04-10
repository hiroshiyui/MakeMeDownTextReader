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
