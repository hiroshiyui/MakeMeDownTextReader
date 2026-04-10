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

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Minimal EPUB 3 parser. Reads the entire archive into memory (most ebooks are
 * small enough that this is fine), locates the OPF rootfile via container.xml,
 * walks the spine, and parses each XHTML chapter into [EpubBlock]s.
 *
 * No CSS application — Compose typography is used for rendering. Images
 * referenced from XHTML are kept in [EpubBook.resources] for later decoding.
 */
object EpubParser {

    fun parse(input: InputStream): EpubBook {
        val entries = readZip(input)

        val containerXml = entries["META-INF/container.xml"]
            ?: error("EPUB missing META-INF/container.xml")
        val opfPath = findOpfPath(containerXml)
            ?: error("EPUB container.xml has no rootfile")

        val opfBytes = entries[opfPath] ?: error("EPUB OPF not found at $opfPath")
        val opf = parseOpf(opfBytes)

        val opfDir = opfPath.substringBeforeLast('/', "")

        // Resolve manifest hrefs to full zip paths.
        val manifestPaths: Map<String, ManifestItem> = opf.manifest.mapValues { (_, item) ->
            item.copy(href = resolvePath(opfDir, item.href))
        }

        val chapters = mutableListOf<EpubChapter>()
        for ((index, idref) in opf.spine.withIndex()) {
            val item = manifestPaths[idref] ?: continue
            val bytes = entries[item.href] ?: continue
            val chapterDir = item.href.substringBeforeLast('/', "")
            val parsed = XhtmlToBlocks.parse(bytes, chapterDir)
            val title = parsed.title?.takeIf { it.isNotBlank() }
                ?: idref
                ?: "Chapter ${index + 1}"
            chapters += EpubChapter(id = idref, title = title, blocks = parsed.blocks)
        }

        return EpubBook(
            title = opf.title.ifBlank { "Untitled" },
            author = opf.author,
            chapters = chapters,
            resources = entries,
        )
    }

    // ---------- ZIP ----------

    private fun readZip(input: InputStream): Map<String, ByteArray> {
        val out = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zin ->
            var entry = zin.nextEntry
            val buf = ByteArray(8 * 1024)
            while (entry != null) {
                if (!entry.isDirectory) {
                    val baos = java.io.ByteArrayOutputStream()
                    while (true) {
                        val n = zin.read(buf)
                        if (n <= 0) break
                        baos.write(buf, 0, n)
                    }
                    out[entry.name] = baos.toByteArray()
                }
                zin.closeEntry()
                entry = zin.nextEntry
            }
        }
        return out
    }

    // ---------- container.xml ----------

    private fun findOpfPath(bytes: ByteArray): String? {
        val parser = newPullParser(bytes)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
            }
            event = parser.next()
        }
        return null
    }

    // ---------- OPF ----------

    private data class Opf(
        val title: String,
        val author: String?,
        val manifest: Map<String, ManifestItem>,
        val spine: List<String>,
    )

    private fun parseOpf(bytes: ByteArray): Opf {
        val parser = newPullParser(bytes)
        var title = ""
        var author: String? = null
        val manifest = LinkedHashMap<String, ManifestItem>()
        val spine = mutableListOf<String>()

        var event = parser.eventType
        var inMetadata = false
        var collectingTitle = false
        var collectingAuthor = false

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    when (name) {
                        "metadata" -> inMetadata = true
                        "title" -> if (inMetadata) collectingTitle = true
                        "creator" -> if (inMetadata) collectingAuthor = true
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                            if (id.isNotEmpty() && href.isNotEmpty()) {
                                manifest[id] = ManifestItem(id, href, mediaType)
                            }
                        }
                        "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref") ?: ""
                            val linear = parser.getAttributeValue(null, "linear")
                            if (idref.isNotEmpty() && linear != "no") spine += idref
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (collectingTitle) title += parser.text
                    if (collectingAuthor) author = (author ?: "") + parser.text
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "metadata" -> inMetadata = false
                        "title" -> collectingTitle = false
                        "creator" -> collectingAuthor = false
                    }
                }
            }
            event = parser.next()
        }
        return Opf(title.trim(), author?.trim()?.ifBlank { null }, manifest, spine)
    }

    internal data class ManifestItem(val id: String, val href: String, val mediaType: String)

    // ---------- helpers ----------

    private fun newPullParser(bytes: ByteArray): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        return parser
    }

    /** Resolve [relative] against [baseDir], collapsing `./` and `../` segments. */
    internal fun resolvePath(baseDir: String, relative: String): String {
        val href = relative.substringBefore('#')
        if (href.startsWith('/')) return href.trimStart('/')
        val base: MutableList<String> =
            if (baseDir.isEmpty()) mutableListOf() else baseDir.split('/').toMutableList()
        for (segment in href.split('/')) {
            when (segment) {
                "", "." -> continue
                ".." -> if (base.isNotEmpty()) base.removeAt(base.size - 1)
                else -> base.add(segment)
            }
        }
        return base.joinToString("/")
    }
}
