package org.ghostsinthelab.app.makedown.reader.epub

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
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream

/**
 * Recursive-descent walker that converts XHTML into [EpubBlock]s.
 *
 * Lenient about input — namespaces are ignored, unknown tags are skipped at the
 * block level and treated as transparent at the inline level.
 */
internal object XhtmlToBlocks {

    data class Result(val title: String?, val blocks: List<EpubBlock>)

    fun parse(bytes: ByteArray, chapterDir: String): Result {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
        }.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        // Skip until <body>.
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("body", ignoreCase = true)) {
                break
            }
            event = parser.next()
        }
        if (event == XmlPullParser.END_DOCUMENT) return Result(null, emptyList())

        val blocks = mutableListOf<EpubBlock>()
        parseBlockChildren(parser, "body", chapterDir, blocks)

        // Pull a title from the first heading we find.
        val title = blocks.firstNotNullOfOrNull { b ->
            (b as? EpubBlock.Heading)?.text?.text?.trim()
        }
        return Result(title, blocks)
    }

    private class InlineBuf {
        var builder: AnnotatedString.Builder = AnnotatedString.Builder()
        fun isBlank(): Boolean = builder.toAnnotatedString().text.isBlank()
        fun take(): AnnotatedString {
            val s = builder.toAnnotatedString()
            builder = AnnotatedString.Builder()
            return s
        }
    }

    /**
     * Parses block-level children of [containerTag] until its END_TAG. Inline
     * runs are accumulated in [InlineBuf] and flushed as paragraphs at block
     * boundaries.
     */
    private fun parseBlockChildren(
        parser: XmlPullParser,
        containerTag: String,
        chapterDir: String,
        out: MutableList<EpubBlock>,
    ) {
        val buf = InlineBuf()

        fun flush() {
            if (!buf.isBlank()) {
                out += EpubBlock.Paragraph(buf.take().collapseWhitespace())
            } else {
                buf.take() // discard whitespace-only content
            }
        }

        while (true) {
            val event = parser.next()
            when (event) {
                XmlPullParser.END_DOCUMENT -> {
                    flush(); return
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals(containerTag, ignoreCase = true)) {
                        flush(); return
                    }
                }
                XmlPullParser.TEXT -> {
                    buf.builder.append(parser.text ?: "")
                }
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    when (tag) {
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            flush()
                            val level = tag.removePrefix("h").toInt()
                            val text = collectInline(parser, tag, chapterDir)
                            out += EpubBlock.Heading(level, text.collapseWhitespace())
                        }
                        "p", "div", "section", "article", "header", "footer", "nav", "aside" -> {
                            flush()
                            val sub = mutableListOf<EpubBlock>()
                            // div-like containers may have either inline text or nested blocks.
                            // Treat them as block containers — recurse.
                            if (tag == "p") {
                                val text = collectInline(parser, tag, chapterDir)
                                if (text.text.isNotBlank()) {
                                    out += EpubBlock.Paragraph(text.collapseWhitespace())
                                }
                            } else {
                                parseBlockChildren(parser, tag, chapterDir, sub)
                                out += sub
                            }
                        }
                        "br" -> {
                            buf.builder.append('\n')
                        }
                        "hr" -> {
                            flush()
                            out += EpubBlock.HorizontalRule
                        }
                        "ul" -> {
                            flush()
                            out += EpubBlock.BulletList(parseListItems(parser, tag, chapterDir))
                        }
                        "ol" -> {
                            flush()
                            val start = parser.getAttributeValue(null, "start")?.toIntOrNull() ?: 1
                            out += EpubBlock.OrderedList(parseListItems(parser, tag, chapterDir), start)
                        }
                        "blockquote" -> {
                            flush()
                            val children = mutableListOf<EpubBlock>()
                            parseBlockChildren(parser, tag, chapterDir, children)
                            out += EpubBlock.BlockQuote(children)
                        }
                        "pre" -> {
                            flush()
                            out += EpubBlock.CodeBlock(collectRawText(parser, tag))
                        }
                        "img" -> {
                            val src = parser.getAttributeValue(null, "src")
                            val alt = parser.getAttributeValue(null, "alt")
                            if (!src.isNullOrBlank()) {
                                flush()
                                val resolved = EpubParser.resolvePath(chapterDir, src)
                                out += EpubBlock.Image(resolved, alt)
                            }
                        }
                        "table" -> {
                            flush()
                            skipTo(parser, tag)
                            out += EpubBlock.Paragraph(AnnotatedString("(table omitted)"))
                        }
                        // Inline tags that may appear at block level — fold their content
                        // into the current inline buffer rather than starting a new paragraph.
                        "em", "i", "strong", "b", "u", "code", "a", "span", "small", "sub", "sup" -> {
                            collectStyledInline(parser, tag, buf.builder, chapterDir)
                        }
                        else -> {
                            // Unknown — treat as transparent block container.
                            val sub = mutableListOf<EpubBlock>()
                            parseBlockChildren(parser, tag, chapterDir, sub)
                            out += sub
                        }
                    }
                }
            }
        }
    }

    private fun parseListItems(
        parser: XmlPullParser,
        listTag: String,
        chapterDir: String,
    ): List<List<EpubBlock>> {
        val items = mutableListOf<List<EpubBlock>>()
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) return items
            if (event == XmlPullParser.END_TAG && parser.name.equals(listTag, ignoreCase = true)) {
                return items
            }
            if (event == XmlPullParser.START_TAG && parser.name.equals("li", ignoreCase = true)) {
                val children = mutableListOf<EpubBlock>()
                parseBlockChildren(parser, "li", chapterDir, children)
                items += children
            }
        }
    }

    /** Collect all text inside [containerTag] verbatim, preserving newlines. */
    private fun collectRawText(parser: XmlPullParser, containerTag: String): String {
        val sb = StringBuilder()
        var depth = 1
        while (true) {
            val event = parser.next()
            when (event) {
                XmlPullParser.END_DOCUMENT -> return sb.toString()
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals(containerTag, ignoreCase = true)) depth++
                    if (parser.name.equals("br", ignoreCase = true)) sb.append('\n')
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals(containerTag, ignoreCase = true)) {
                        depth--
                        if (depth == 0) return sb.toString().trimEnd()
                    }
                }
                XmlPullParser.TEXT -> sb.append(parser.text ?: "")
            }
        }
    }

    /** Collect inline content of [containerTag] into a fresh AnnotatedString. */
    private fun collectInline(parser: XmlPullParser, containerTag: String, chapterDir: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        collectInlineInto(parser, containerTag, builder, chapterDir)
        return builder.toAnnotatedString()
    }

    private fun collectInlineInto(
        parser: XmlPullParser,
        containerTag: String,
        builder: AnnotatedString.Builder,
        chapterDir: String,
    ) {
        while (true) {
            val event = parser.next()
            when (event) {
                XmlPullParser.END_DOCUMENT -> return
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals(containerTag, ignoreCase = true)) return
                }
                XmlPullParser.TEXT -> builder.append(parser.text ?: "")
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    when (tag) {
                        "br" -> builder.append('\n')
                        else -> collectStyledInline(parser, tag, builder, chapterDir)
                    }
                }
            }
        }
    }

    /**
     * Read an inline tag's content with its style applied. Always consumes the
     * matching END_TAG of [tag] before returning.
     */
    private fun collectStyledInline(
        parser: XmlPullParser,
        tag: String,
        builder: AnnotatedString.Builder,
        chapterDir: String,
    ) {
        when (tag) {
            "em", "i" -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                collectInlineInto(parser, tag, this, chapterDir)
            }
            "strong", "b" -> builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                collectInlineInto(parser, tag, this, chapterDir)
            }
            "u" -> builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                collectInlineInto(parser, tag, this, chapterDir)
            }
            "code" -> builder.withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080))
            ) {
                collectInlineInto(parser, tag, this, chapterDir)
            }
            "a" -> {
                val href = parser.getAttributeValue(null, "href")
                if (!href.isNullOrBlank() &&
                    (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("mailto:"))
                ) {
                    builder.withLink(LinkAnnotation.Url(href)) {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF1A73E8),
                                textDecoration = TextDecoration.Underline,
                            )
                        ) { collectInlineInto(parser, tag, this, chapterDir) }
                    }
                } else {
                    collectInlineInto(parser, tag, builder, chapterDir)
                }
            }
            else -> collectInlineInto(parser, tag, builder, chapterDir)
        }
    }

    private fun skipTo(parser: XmlPullParser, containerTag: String) {
        var depth = 1
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) return
            if (event == XmlPullParser.START_TAG && parser.name.equals(containerTag, ignoreCase = true)) depth++
            if (event == XmlPullParser.END_TAG && parser.name.equals(containerTag, ignoreCase = true)) {
                depth--
                if (depth == 0) return
            }
        }
    }

    /** Collapse runs of XML/HTML whitespace to single spaces, like a browser. */
    private fun AnnotatedString.collapseWhitespace(): AnnotatedString {
        val s = text
        if (s.isEmpty()) return this
        val sb = StringBuilder(s.length)
        val map = IntArray(s.length + 1)
        var prevSpace = true
        for (i in s.indices) {
            val c = s[i]
            map[i] = sb.length
            val isWs = c == ' ' || c == '\t' || c == '\r' || c == '\n'
            if (isWs) {
                if (!prevSpace) sb.append(' ')
                prevSpace = true
            } else {
                sb.append(c)
                prevSpace = false
            }
        }
        map[s.length] = sb.length
        var newText = sb.toString()
        if (newText.endsWith(' ')) {
            newText = newText.dropLast(1)
            for (i in map.indices) if (map[i] > newText.length) map[i] = newText.length
        }
        val rebuilt = AnnotatedString.Builder(newText)
        for (range in spanStyles) {
            val start = map[range.start.coerceIn(0, s.length)]
            val end = map[range.end.coerceIn(0, s.length)]
            if (end > start) rebuilt.addStyle(range.item, start, end)
        }
        for (range in paragraphStyles) {
            val start = map[range.start.coerceIn(0, s.length)]
            val end = map[range.end.coerceIn(0, s.length)]
            if (end > start) rebuilt.addStyle(range.item, start, end)
        }
        for (link in getLinkAnnotations(0, s.length)) {
            val start = map[link.start.coerceIn(0, s.length)]
            val end = map[link.end.coerceIn(0, s.length)]
            if (end > start) {
                when (val item = link.item) {
                    is LinkAnnotation.Url -> rebuilt.addLink(item, start, end)
                    is LinkAnnotation.Clickable -> rebuilt.addLink(item, start, end)
                }
            }
        }
        return rebuilt.toAnnotatedString()
    }
}
