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

/**
 * File-backed settings store, mirroring [RecentsRepository]'s approach so the
 * persistence layer stays consistent across the app.
 */
class SettingsRepository private constructor(context: Context) {

    private val file: File = File(context.applicationContext.filesDir, "settings.json")
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    init {
        scope.launch { reload() }
    }

    private suspend fun reload() = mutex.withLock {
        val loaded: Settings = withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists()) Settings()
                else json.decodeFromString<Settings>(file.readText())
            }.getOrElse { Settings() }
        }
        _state.value = loaded
    }

    /** Merge a partial update into the current settings and persist. */
    suspend fun update(transform: (Settings) -> Settings) {
        mutex.withLock {
            val updated = transform(_state.value)
            if (updated == _state.value) return@withLock
            _state.value = updated
            withContext(Dispatchers.IO) {
                runCatching { file.writeText(json.encodeToString(updated)) }
            }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
    }
}
