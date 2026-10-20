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
 * Cały stan launchera w dwóch plikach JSON.
 *
 * Tryby działania:
 * 1. LOCAL - tylko lokalnie (domyślnie)
 * 2. CLOUD - Google Drive jako główne źródło (auto-sync)
 * 3. HYBRID - lokalnie + backup do Drive co X minut
 *
 * Zapis jest odraczany (debounce), bo przeciąganie ikon generuje serię zmian w krótkim czasie.
 */
class LauncherStore(
    private val context: Context,
    private val scope: CoroutineScope,
    private val driveManager: GoogleDriveManager? = null,
) {
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

    // Google Drive sync
    private var cloudSyncJob: Job? = null
    var cloudSyncEnabled = false
        set(value) {
            field = value
            if (value) startCloudSync() else stopCloudSync()
        }
    var cloudSyncInterval = 300000L // 5 minut domyślnie
    var lastSyncTimestamp = 0L

    init {
        // Synchronizuj przy uruchomieniu jeśli Drive jest dostępny
        if (driveManager?.isSignedIn() == true) {
            scope.launch {
                delay(2000) // Poczekaj 2s na inicjalizację
                syncOnStartup()
            }
        }
    }

    // Synchronizacja przy starcie aplikacji
    private suspend fun syncOnStartup() {
        val manager = driveManager ?: return

        try {
            // Sprawdź czy są dane w Drive
            val hasCloudData = manager.loadConfig() != null || manager.loadWorkspace() != null

            if (hasCloudData) {
                // Sprawdź czy są backupy
                val hasBackups = manager.hasExistingBackups()

                if (hasBackups) {
                    // TODO: Pokaż dialog użytkownikowi
                    // "Znaleziono dane w Google Drive. Przywrócić?"
                    // Na razie auto-merge
                    loadFromCloud()
                } else {
                    // Są dane ale nie ma backupów - merge
                    loadFromCloud()
                }
            } else {
                // Brak danych w Drive - wyślij lokalne
                syncToCloud()
            }

            lastSyncTimestamp = System.currentTimeMillis()

            // Włącz auto-sync jeśli skonfigurowany
            if (_config.value.cloudAutoSync) {
                cloudSyncEnabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun <T> read(f: File, s: kotlinx.serialization.KSerializer<T>): T? =
        runCatching { if (f.exists()) json.decodeFromString(s, f.readText()) else null }.getOrNull()

    // Wczytaj z Drive jeśli dostępne, inaczej lokalnie
    suspend fun loadFromCloud(): Boolean {
        val manager = driveManager ?: return false
        if (!manager.isSignedIn()) return false

        return try {
            val configJson = manager.loadConfig()
            val workspaceJson = manager.loadWorkspace()

            if (configJson != null) {
                _config.value = json.decodeFromString(Config.serializer(), configJson)
            }
            if (workspaceJson != null) {
                _workspace.value = json.decodeFromString(Workspace.serializer(), workspaceJson)
            }

            configJson != null || workspaceJson != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Rozpocznij auto-sync z Drive
    private fun startCloudSync() {
        cloudSyncJob?.cancel()
        cloudSyncJob = scope.launch {
            while (cloudSyncEnabled) {
                delay(cloudSyncInterval)
                syncToCloud()
            }
        }
    }

    private fun stopCloudSync() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
    }

    // Synchronizuj do Drive
    suspend fun syncToCloud(): Boolean {
        val manager = driveManager ?: return false
        if (!manager.isSignedIn()) return false

        return try {
            val configJson = json.encodeToString(Config.serializer(), _config.value)
            val workspaceJson = json.encodeToString(Workspace.serializer(), _workspace.value)

            manager.saveConfig(configJson) && manager.saveWorkspace(workspaceJson)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Utwórz backup w Drive
    suspend fun createCloudBackup(): Boolean {
        val manager = driveManager ?: return false

        return try {
            val configJson = json.encodeToString(Config.serializer(), _config.value)
            val workspaceJson = json.encodeToString(Workspace.serializer(), _workspace.value)

            manager.createBackup(configJson, workspaceJson)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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

    /** Kopia zapasowa bez kluczy API i bez widżetów systemowych (ich identyfikatory nie przenoszą się). */
    fun exportBackup(): String = json.encodeToString(
        Backup.serializer(),
        Backup(config = _config.value.copy(openRouterKey = "", aiKeys = emptyMap()), workspace = _workspace.value),
    )

    fun importBackup(text: String) {
        val b = json.decodeFromString(Backup.serializer(), text)
        // Klucze API zostają z tego urządzenia (kopia ich nie zawiera)
        val cur = _config.value
        _config.value = b.config.copy(
            openRouterKey = cur.openRouterKey.ifBlank { b.config.openRouterKey },
            aiKeys = b.config.aiKeys + cur.aiKeys.filterValues { it.isNotBlank() },
        )
        _workspace.value = b.workspace.copy(
            pages = b.workspace.pages.map { p -> p.copy(items = p.items.filter { it.type != ItemType.Widget }) },
            initialized = true,
        )
        scheduleSave()
    }
}
