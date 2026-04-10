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

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * App-private documents store.
 *
 * Backed by a single flat directory at `context.filesDir/private_documents/`.
 * This path is internal storage — no other app can read or write it without
 * root — and is explicitly excluded from Auto Backup and Device-to-Device
 * transfer via `res/xml/backup_rules.xml` and `data_extraction_rules.xml` so
 * documents never leave the device.
 *
 * Subdirectories are not supported; [sanitize] strips any path separators
 * from incoming names before touching the file system.
 */
class PrivateStore private constructor(context: Context) {

    private val dir: File = File(context.applicationContext.filesDir, DIR_NAME).apply {
        if (!exists()) mkdirs()
    }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<List<PrivateDocument>>(emptyList())
    val state: StateFlow<List<PrivateDocument>> = _state.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    /** Re-read the directory listing and update the [state] flow. */
    suspend fun refresh() {
        val docs: List<PrivateDocument> = withContext(Dispatchers.IO) {
            dir.listFiles()
                ?.filter { it.isFile }
                ?.map { f ->
                    PrivateDocument(
                        fileName = f.name,
                        displayName = f.name,
                        type = DocumentType.detect(mimeType = null, displayName = f.name),
                        lastModified = f.lastModified(),
                        sizeBytes = f.length(),
                    )
                }
                ?.sortedByDescending { it.lastModified }
                .orEmpty()
        }
        _state.value = docs
    }

    /** Resolve a file name to its absolute [File] inside the private dir. */
    fun fileOf(fileName: String): File = File(dir, sanitize(fileName))

    /**
     * Create an empty file with the given name and type. If [name] lacks a
     * recognisable extension for [type] one is appended. Returns the created
     * [File].
     */
    suspend fun create(name: String, type: DocumentType): File = mutex.withLock {
        val safeName = sanitize(ensureExtension(name, type))
        val file = File(dir, safeName)
        withContext(Dispatchers.IO) {
            if (!file.exists()) file.createNewFile()
        }
        refresh()
        file
    }

    suspend fun read(fileName: String): String = withContext(Dispatchers.IO) {
        File(dir, sanitize(fileName)).readText(Charsets.UTF_8)
    }

    suspend fun write(fileName: String, text: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                File(dir, sanitize(fileName)).writeText(text, Charsets.UTF_8)
            }
            refresh()
        }
    }

    suspend fun delete(fileName: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                File(dir, sanitize(fileName)).delete()
            }
            refresh()
        }
    }

    /** Strip directory separators and control characters from a file name. */
    private fun sanitize(name: String): String {
        return name
            .replace('/', '_')
            .replace('\\', '_')
            .replace('\u0000', '_')
            .trim()
            .ifEmpty { "untitled" }
    }

    /** If the name doesn't already carry a known extension for [type],
     *  append the canonical one. */
    private fun ensureExtension(name: String, type: DocumentType): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        val accepted = acceptedExtensions(type)
        if (ext in accepted) return name
        val canonical = accepted.firstOrNull() ?: return name
        return "$name.$canonical"
    }

    private fun acceptedExtensions(type: DocumentType): List<String> = when (type) {
        DocumentType.MARKDOWN -> listOf("md", "markdown", "mdown", "mkd", "mkdn")
        DocumentType.PLAIN_TEXT -> listOf("txt", "text", "log")
        // EPUB isn't editable and shouldn't be created in the private store.
        DocumentType.EPUB -> emptyList()
    }

    companion object {
        private const val DIR_NAME = "private_documents"

        @Volatile
        private var INSTANCE: PrivateStore? = null

        fun get(context: Context): PrivateStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrivateStore(context).also { INSTANCE = it }
            }
    }
}
