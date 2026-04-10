package org.ghostsinthelab.app.makedown.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ghostsinthelab.app.makedown.data.DocumentType
import org.ghostsinthelab.app.makedown.reader.epub.EpubBook
import org.ghostsinthelab.app.makedown.reader.epub.EpubParser

sealed interface LoadedDocument {
    val displayName: String

    data class PlainText(override val displayName: String, val text: String) : LoadedDocument
    data class Markdown(override val displayName: String, val source: String) : LoadedDocument
    data class Epub(override val displayName: String, val book: EpubBook) : LoadedDocument
}

object DocumentLoader {

    suspend fun load(
        context: Context,
        uri: Uri,
        type: DocumentType,
        displayName: String,
    ): LoadedDocument = withContext(Dispatchers.IO) {
        when (type) {
            DocumentType.PLAIN_TEXT -> LoadedDocument.PlainText(displayName, readText(context, uri))
            DocumentType.MARKDOWN -> LoadedDocument.Markdown(displayName, readText(context, uri))
            DocumentType.EPUB -> {
                val book = context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Cannot open EPUB stream" }
                    EpubParser.parse(input)
                }
                LoadedDocument.Epub(displayName, book)
            }
        }
    }

    private fun readText(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open document stream" }
            input.readBytes()
        }
        // Strip UTF-8 BOM if present.
        val start = if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) 3 else 0
        return String(bytes, start, bytes.size - start, Charsets.UTF_8)
    }
}
