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

package org.ghostsinthelab.app.makedown.reader.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

/** Block-level structure produced from a parsed Markdown document. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : MdBlock
    data class Paragraph(val text: AnnotatedString) : MdBlock
    data class BulletList(val items: List<List<MdBlock>>) : MdBlock
    data class OrderedList(val items: List<List<MdBlock>>, val start: Int = 1) : MdBlock
    data class BlockQuote(val children: List<MdBlock>) : MdBlock
    data class CodeBlock(val language: String?, val code: String) : MdBlock
    data object HorizontalRule : MdBlock
}

object MarkdownToBlocks {

    fun convert(source: String): List<MdBlock> {
        val root = MarkdownAst.parse(source)
        return blocksFromChildren(root, source)
    }

    private fun blocksFromChildren(parent: ASTNode, src: String): List<MdBlock> {
        val out = mutableListOf<MdBlock>()
        for (child in parent.children) {
            val block = nodeToBlock(child, src) ?: continue
            out += block
        }
        return out
    }

    private fun nodeToBlock(node: ASTNode, src: String): MdBlock? {
        return when (node.type) {
            MarkdownElementTypes.ATX_1 -> heading(1, node, src)
            MarkdownElementTypes.ATX_2 -> heading(2, node, src)
            MarkdownElementTypes.ATX_3 -> heading(3, node, src)
            MarkdownElementTypes.ATX_4 -> heading(4, node, src)
            MarkdownElementTypes.ATX_5 -> heading(5, node, src)
            MarkdownElementTypes.ATX_6 -> heading(6, node, src)
            MarkdownElementTypes.SETEXT_1 -> heading(1, node, src)
            MarkdownElementTypes.SETEXT_2 -> heading(2, node, src)
            MarkdownElementTypes.PARAGRAPH -> MdBlock.Paragraph(inline(node, src))
            MarkdownElementTypes.UNORDERED_LIST -> MdBlock.BulletList(listItems(node, src))
            MarkdownElementTypes.ORDERED_LIST -> MdBlock.OrderedList(listItems(node, src))
            MarkdownElementTypes.BLOCK_QUOTE -> MdBlock.BlockQuote(blocksFromChildren(node, src))
            MarkdownElementTypes.CODE_BLOCK -> {
                val text = node.getTextInNode(src).toString()
                    .lines()
                    .joinToString("\n") { it.removePrefix("    ") }
                    .trimEnd()
                MdBlock.CodeBlock(null, text)
            }
            MarkdownElementTypes.CODE_FENCE -> codeFence(node, src)
            MarkdownTokenTypes.HORIZONTAL_RULE -> MdBlock.HorizontalRule
            // Skip whitespace, EOL, link reference definitions, etc. at the block level.
            else -> null
        }
    }

    private fun heading(level: Int, node: ASTNode, src: String): MdBlock.Heading {
        // Drop the leading #s and trailing #s by harvesting only ATX_CONTENT/text children.
        val builder = AnnotatedString.Builder()
        for (child in node.children) {
            when (child.type) {
                MarkdownTokenTypes.ATX_CONTENT, MarkdownElementTypes.PARAGRAPH -> {
                    appendInline(builder, child, src)
                }
                MarkdownTokenTypes.SETEXT_CONTENT -> {
                    appendInline(builder, child, src)
                }
                MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> {
                    appendInline(builder, child, src)
                }
                else -> if (child.children.isEmpty() &&
                    child.type != MarkdownTokenTypes.ATX_HEADER &&
                    child.type != MarkdownTokenTypes.WHITE_SPACE &&
                    child.type != MarkdownTokenTypes.EOL
                ) {
                    // ignore
                }
            }
        }
        // Trim surrounding whitespace from headings.
        val text = builder.toAnnotatedString().let { it.trimAnnotated() }
        return MdBlock.Heading(level, text)
    }

    private fun listItems(listNode: ASTNode, src: String): List<List<MdBlock>> {
        val items = mutableListOf<List<MdBlock>>()
        for (child in listNode.children) {
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                items += blocksFromChildren(child, src)
            }
        }
        return items
    }

    private fun codeFence(node: ASTNode, src: String): MdBlock.CodeBlock {
        var lang: String? = null
        val sb = StringBuilder()
        for (child in node.children) {
            when (child.type) {
                MarkdownTokenTypes.FENCE_LANG -> lang = child.getTextInNode(src).toString().trim().ifEmpty { null }
                MarkdownTokenTypes.CODE_FENCE_CONTENT -> sb.append(child.getTextInNode(src))
                MarkdownTokenTypes.EOL -> if (sb.isNotEmpty()) sb.append('\n')
            }
        }
        return MdBlock.CodeBlock(lang, sb.toString().trimEnd())
    }

    private fun inline(node: ASTNode, src: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        appendInline(builder, node, src)
        return builder.toAnnotatedString().trimAnnotated()
    }

    private fun appendInline(builder: AnnotatedString.Builder, node: ASTNode, src: String) {
        when (node.type) {
            MarkdownTokenTypes.TEXT,
            MarkdownTokenTypes.WHITE_SPACE,
            MarkdownTokenTypes.SINGLE_QUOTE,
            MarkdownTokenTypes.DOUBLE_QUOTE,
            MarkdownTokenTypes.LPAREN,
            MarkdownTokenTypes.RPAREN,
            MarkdownTokenTypes.LBRACKET,
            MarkdownTokenTypes.RBRACKET,
            MarkdownTokenTypes.COLON,
            MarkdownTokenTypes.EXCLAMATION_MARK,
            MarkdownTokenTypes.EMPH -> builder.append(node.getTextInNode(src).toString())

            MarkdownTokenTypes.EOL -> builder.append(' ')
            MarkdownTokenTypes.HARD_LINE_BREAK -> builder.append('\n')

            MarkdownElementTypes.STRONG -> builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                // Skip the surrounding * / _ marker tokens, mirroring how the
                // CODE_SPAN branch below skips its BACKTICK markers. Recurse
                // into anything else so nested emphasis (e.g. ***foo***)
                // keeps working — the inner EMPH *element* has type
                // MarkdownElementTypes.EMPH, which is distinct from the
                // MarkdownTokenTypes.EMPH marker token we're filtering out.
                for (c in node.children) {
                    if (c.type != MarkdownTokenTypes.EMPH) appendInline(this, c, src)
                }
            }
            MarkdownElementTypes.EMPH -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                for (c in node.children) {
                    if (c.type != MarkdownTokenTypes.EMPH) appendInline(this, c, src)
                }
            }
            GFMElementTypes.STRIKETHROUGH -> builder.withStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough)
            ) {
                // Skip the surrounding ~ marker tokens.
                for (c in node.children) {
                    if (c.type != GFMTokenTypes.TILDE) appendInline(this, c, src)
                }
            }
            MarkdownElementTypes.CODE_SPAN -> builder.withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080))
            ) {
                // Skip the surrounding ` tokens.
                node.children.forEach { c ->
                    if (c.type != MarkdownTokenTypes.BACKTICK) {
                        builder.append(c.getTextInNode(src).toString())
                    }
                }
            }
            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                val url = findChild(node, MarkdownElementTypes.LINK_DESTINATION)
                    ?.getTextInNode(src)?.toString()?.trim('<', '>')
                val labelNode = findChild(node, MarkdownElementTypes.LINK_TEXT) ?: node
                if (url.isNullOrBlank()) {
                    inlineChildren(builder, labelNode, src)
                } else {
                    builder.withLink(LinkAnnotation.Url(url)) {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF1A73E8),
                                textDecoration = TextDecoration.Underline,
                            )
                        ) {
                            // LINK_TEXT contains the [ and ] tokens — skip those.
                            for (c in labelNode.children) {
                                if (c.type == MarkdownTokenTypes.LBRACKET || c.type == MarkdownTokenTypes.RBRACKET) continue
                                appendInline(this, c, src)
                            }
                        }
                    }
                }
            }
            MarkdownElementTypes.AUTOLINK,
            GFMTokenTypes.GFM_AUTOLINK -> {
                val raw = node.getTextInNode(src).toString().trim('<', '>')
                builder.withLink(LinkAnnotation.Url(raw)) {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF1A73E8),
                            textDecoration = TextDecoration.Underline,
                        )
                    ) { append(raw) }
                }
            }
            MarkdownElementTypes.IMAGE -> {
                // Inline images aren't rendered yet — fall back to alt text.
                val alt = findChild(node, MarkdownElementTypes.LINK_TEXT)
                    ?.getTextInNode(src)?.toString()?.trim('[', ']') ?: ""
                if (alt.isNotEmpty()) builder.append("[image: $alt]")
            }
            else -> {
                // Recurse for unknown nodes if they have children, otherwise emit raw text.
                if (node.children.isNotEmpty()) {
                    inlineChildren(builder, node, src)
                } else {
                    builder.append(node.getTextInNode(src).toString())
                }
            }
        }
    }

    private fun inlineChildren(builder: AnnotatedString.Builder, node: ASTNode, src: String) {
        for (child in node.children) appendInline(builder, child, src)
    }

    private fun findChild(node: ASTNode, type: org.intellij.markdown.IElementType): ASTNode? {
        for (c in node.children) if (c.type == type) return c
        return null
    }

    /** Trim leading/trailing ASCII whitespace while preserving span styles. */
    private fun AnnotatedString.trimAnnotated(): AnnotatedString {
        val s = this.text
        var start = 0
        var end = s.length
        while (start < end && s[start].isWhitespace()) start++
        while (end > start && s[end - 1].isWhitespace()) end--
        return if (start == 0 && end == s.length) this else this.subSequence(start, end)
    }
}
