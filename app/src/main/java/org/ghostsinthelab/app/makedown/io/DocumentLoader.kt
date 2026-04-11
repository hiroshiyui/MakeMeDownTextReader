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

package org.ghostsinthelab.app.makedown.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ghostsinthelab.app.makedown.data.DocumentType
import org.ghostsinthelab.app.makedown.data.PrivateStore
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

    /**
     * Load a document from the app-private [PrivateStore] by file name.
     * EPUB is not supported in the private store — only plain text and
     * Markdown can be created or edited there.
     */
    suspend fun loadPrivate(
        context: Context,
        fileName: String,
        type: DocumentType,
        displayName: String,
    ): LoadedDocument = withContext(Dispatchers.IO) {
        val store = PrivateStore.get(context)
        val text = store.read(fileName)
        when (type) {
            DocumentType.PLAIN_TEXT -> LoadedDocument.PlainText(displayName, text)
            DocumentType.MARKDOWN -> LoadedDocument.Markdown(displayName, text)
            DocumentType.EPUB -> error("EPUB is not supported in the private store")
        }
    }

    /**
     * Load a document bundled inside the APK as an asset (under
     * `app/src/main/assets/`). Used by the home-screen "sample
     * documents" affordance, and by anything else that wants to ship a
     * read-only document with the app. Supports all three document
     * types (plain text, Markdown, EPUB).
     *
     * Samples are read-only by contract: there is no `saveSample(...)`,
     * and the reader hides its Edit button for any URI that resolves
     * to a sample.
     */
    suspend fun loadSample(
        context: Context,
        assetPath: String,
        type: DocumentType,
        displayName: String,
    ): LoadedDocument = withContext(Dispatchers.IO) {
        when (type) {
            DocumentType.PLAIN_TEXT -> LoadedDocument.PlainText(
                displayName,
                readAssetText(context, assetPath),
            )
            DocumentType.MARKDOWN -> LoadedDocument.Markdown(
                displayName,
                readAssetText(context, assetPath),
            )
            DocumentType.EPUB -> {
                val book = context.assets.open(assetPath).use { input ->
                    EpubParser.parse(input)
                }
                LoadedDocument.Epub(displayName, book)
            }
        }
    }

    private fun readAssetText(context: Context, assetPath: String): String {
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        // Strip UTF-8 BOM if present, mirroring readText().
        val start = if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) 3 else 0
        return String(bytes, start, bytes.size - start, Charsets.UTF_8)
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
