package com.opendesktop.launcher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Cały stan launchera w dwóch plikach JSON. Zapis jest odraczany (debounce),
 * bo przeciąganie ikon generuje serię zmian w krótkim czasie.
 */
class LauncherStore(context: Context, private val scope: CoroutineScope) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val dir = context.filesDir
    private val configFile = File(dir, "config.json")
    private val workspaceFile = File(dir, "workspace.json")

    private val _config = MutableStateFlow(read(configFile, Config.serializer()) ?: Config())
    private val _workspace = MutableStateFlow(read(workspaceFile, Workspace.serializer()) ?: Workspace())
    val config: StateFlow<Config> = _config
    val workspace: StateFlow<Workspace> = _workspace

    private var saveJob: Job? = null
    private val lock = Any()

    private fun <T> read(f: File, s: kotlinx.serialization.KSerializer<T>): T? =
        runCatching { if (f.exists()) json.decodeFromString(s, f.readText()) else null }.getOrNull()

    fun updateConfig(f: (Config) -> Config) {
        _config.update(f)
        scheduleSave()
    }

    fun updateWorkspace(f: (Workspace) -> Workspace) {
        _workspace.update(f)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(350)
            saveNow()
        }
    }

    fun saveNow() = synchronized(lock) {
        write(configFile, json.encodeToString(Config.serializer(), _config.value))
        write(workspaceFile, json.encodeToString(Workspace.serializer(), _workspace.value))
    }

    private fun write(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) {
            f.writeText(text)
            tmp.delete()
        }
    }

    /** Kopia zapasowa bez klucza API i bez widżetów systemowych (ich identyfikatory nie przenoszą się). */
    fun exportBackup(): String = json.encodeToString(
        Backup.serializer(),
        Backup(config = _config.value.copy(openRouterKey = ""), workspace = _workspace.value),
    )

    fun importBackup(text: String) {
        val b = json.decodeFromString(Backup.serializer(), text)
        val key = _config.value.openRouterKey
        _config.value = b.config.copy(openRouterKey = key.ifBlank { b.config.openRouterKey })
        _workspace.value = b.workspace.copy(
            pages = b.workspace.pages.map { p -> p.copy(items = p.items.filter { it.type != ItemType.Widget }) },
            initialized = true,
        )
        scheduleSave()
    }
}
