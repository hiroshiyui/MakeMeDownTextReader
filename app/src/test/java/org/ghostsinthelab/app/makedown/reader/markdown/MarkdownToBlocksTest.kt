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

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for inline Markdown formatting.
 *
 * Before the v1.0.2 fix, the STRONG / EMPH / STRIKETHROUGH branches in
 * [MarkdownToBlocks.appendInline] recursed into their children via
 * `inlineChildren`, and the children included the literal `*` / `_` /
 * `~` marker tokens (`MarkdownTokenTypes.EMPH`, `GFMTokenTypes.TILDE`).
 * Those marker tokens fell through to the catch-all literal-text branch
 * and were appended verbatim, so `**bold**` rendered as the visible
 * string `**bold**` (with the bold weight wrapping the asterisks too)
 * rather than `bold`.
 *
 * These tests assert on the visible *text* of the resulting
 * `AnnotatedString`, which is the cheapest behaviour to lock in.
 */
class MarkdownToBlocksTest {

    private fun paragraphText(source: String): String {
        val blocks = MarkdownToBlocks.convert(source)
        val paragraph = blocks.filterIsInstance<MdBlock.Paragraph>().single()
        return paragraph.text.text
    }

    private fun firstParagraph(source: String): MdBlock.Paragraph =
        MarkdownToBlocks.convert(source).filterIsInstance<MdBlock.Paragraph>().single()

    @Test
    fun `bold marker characters are not part of the rendered text`() {
        assertEquals("bold", paragraphText("**bold**"))
    }

    @Test
    fun `italic marker characters are not part of the rendered text`() {
        assertEquals("italic", paragraphText("*italic*"))
        assertEquals("italic", paragraphText("_italic_"))
    }

    @Test
    fun `strikethrough marker characters are not part of the rendered text`() {
        assertEquals("strike", paragraphText("~~strike~~"))
    }

    @Test
    fun `bold and surrounding text are joined without leftover asterisks`() {
        assertEquals("before bold after", paragraphText("before **bold** after"))
    }

    @Test
    fun `nested bold and italic both leave clean text`() {
        // ***foo*** parses as STRONG containing EMPH containing TEXT, or
        // EMPH containing STRONG — either nesting must produce the bare
        // word with no marker characters left over.
        assertEquals("foo", paragraphText("***foo***"))
    }

    @Test
    fun `stray asterisks outside an emphasis run remain literal`() {
        // The bug fix must NOT silently swallow `*` characters that
        // weren't part of a successful emphasis match — `2 * 3` is plain
        // arithmetic, not a broken bold marker.
        assertEquals("2 * 3 = 6", paragraphText("2 * 3 = 6"))
    }

    @Test
    fun `bold span has FontWeight Bold over the bold word`() {
        val paragraph = firstParagraph("**bold**")
        val text = paragraph.text
        // Some style somewhere on the AnnotatedString must contribute
        // FontWeight.Bold to the "bold" word.
        val boldRanges = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue(
            "expected at least one Bold span, got: ${text.spanStyles}",
            boldRanges.isNotEmpty(),
        )
        // The Bold span should cover the full visible text "bold".
        val anyBoldOverWholeWord = boldRanges.any { it.start == 0 && it.end == text.length }
        assertTrue(
            "expected a Bold span covering the whole [0, ${text.length}) range, got: $boldRanges",
            anyBoldOverWholeWord,
        )
    }

    @Test
    fun `italic span has FontStyle Italic over the italic word`() {
        val paragraph = firstParagraph("*italic*")
        val text = paragraph.text
        val italicRanges = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue(italicRanges.isNotEmpty())
        assertTrue(italicRanges.any { it.start == 0 && it.end == text.length })
    }

    @Test
    fun `strikethrough span has LineThrough decoration over the struck word`() {
        val paragraph = firstParagraph("~~strike~~")
        val text = paragraph.text
        val strikeRanges = text.spanStyles.filter {
            it.item.textDecoration == TextDecoration.LineThrough
        }
        assertTrue(strikeRanges.isNotEmpty())
        assertTrue(strikeRanges.any { it.start == 0 && it.end == text.length })
    }
}
