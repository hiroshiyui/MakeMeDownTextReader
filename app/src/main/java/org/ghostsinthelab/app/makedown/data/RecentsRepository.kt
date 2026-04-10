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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class RecentsRepository private constructor(context: Context) {

    private val file: File = File(context.applicationContext.filesDir, "recents.json")
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<List<RecentFile>>(emptyList())
    val state: StateFlow<List<RecentFile>> = _state.asStateFlow()

    init {
        scope.launch { reload() }
    }

    private suspend fun reload() = mutex.withLock {
        val list: List<RecentFile> = withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists()) emptyList()
                else json.decodeFromString<List<RecentFile>>(file.readText())
            }.getOrElse { emptyList() }
        }
        _state.value = list
    }

    suspend fun add(item: RecentFile) {
        mutex.withLock {
            val updated = (listOf(item) + _state.value.filterNot { it.uri == item.uri })
                .take(MAX_RECENTS)
            _state.value = updated
            persist(updated)
        }
    }

    suspend fun remove(uri: String) {
        mutex.withLock {
            val updated = _state.value.filterNot { it.uri == uri }
            _state.value = updated
            persist(updated)
        }
    }

    private suspend fun persist(list: List<RecentFile>) = withContext(Dispatchers.IO) {
        runCatching {
            file.writeText(json.encodeToString(list))
        }
    }

    companion object {
        private const val MAX_RECENTS = 32
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        @Volatile
        private var INSTANCE: RecentsRepository? = null

        fun get(context: Context): RecentsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecentsRepository(context).also { INSTANCE = it }
            }
    }
}
