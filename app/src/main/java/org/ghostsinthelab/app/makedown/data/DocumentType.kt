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
