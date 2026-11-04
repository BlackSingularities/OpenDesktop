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
 * Cały stan launchera w dwóch plikach JSON. Klucze API nie trafiają do config.json – trzyma je
 * zaszyfrowany [SecretStore] (secrets.bin, wyłączony z kopii zapasowej Androida).
 *
 * Zapis jest odraczany (debounce), bo przeciąganie ikon generuje serię zmian w krótkim czasie.
 */
class LauncherStore(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val dir = context.filesDir
    private val configFile = File(dir, "config.json")
    private val workspaceFile = File(dir, "workspace.json")
    private val secrets = SecretStore(File(dir, "secrets.bin"))
    /** Klucze ostatnio zapisane w [secrets] – żeby nie szyfrować pliku przy każdej zmianie układu. */
    private var savedSecrets: Map<String, String>? = null

    private val _config = MutableStateFlow(loadConfig())
    private val _workspace = MutableStateFlow(read(workspaceFile, Workspace.serializer()) ?: Workspace())
    val config: StateFlow<Config> = _config
    val workspace: StateFlow<Workspace> = _workspace

    private var saveJob: Job? = null
    private val lock = Any()

    /**
     * Konfiguracja z kluczami odszyfrowanymi z [secrets]. Klucze zapisane jawnie przez starsze wersje
     * (config.json) są przenoszone do szyfrowanego pliku i usuwane z config.json przy pierwszym zapisie.
     */
    private fun loadConfig(): Config {
        val plain = read(configFile, Config.serializer()) ?: Config()
        val stored = secrets.read()
        val legacy = plain.secretMap()
        // Jawne klucze w config.json są nowsze (zostają tam tylko, gdy szyfrowanie zawiodło)
        val merged = stored + legacy
        val cfg = plain.withSecrets(merged)
        if (legacy.isEmpty()) {
            savedSecrets = merged
        } else if (runCatching { secrets.write(merged) }.isSuccess) {
            savedSecrets = merged
            runCatching { write(configFile, json.encodeToString(Config.serializer(), cfg.withoutSecrets())) }
        }
        return cfg
    }

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
        val cfg = _config.value
        val map = cfg.secretMap()
        // Zapis szyfrowanego pliku tylko przy zmianie kluczy. Gdyby Keystore na danym telefonie
        // zawiódł, klucze zostają w config.json – lepiej niż zgubić je po restarcie.
        val secured = map == savedSecrets || runCatching { secrets.write(map) }.isSuccess
        if (secured) savedSecrets = map
        write(configFile, json.encodeToString(Config.serializer(), if (secured) cfg.withoutSecrets() else cfg))
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
        Backup(config = _config.value.withoutSecrets(), workspace = _workspace.value),
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

/** Klucz w szyfrowanym pliku dla dawnego pola OpenRoutera. */
private const val LEGACY_OPENROUTER = "_openRouterKey"

private fun Config.secretMap(): Map<String, String> =
    aiKeys + (if (openRouterKey.isNotBlank()) mapOf(LEGACY_OPENROUTER to openRouterKey) else emptyMap())

private fun Config.withSecrets(m: Map<String, String>): Config =
    copy(aiKeys = m - LEGACY_OPENROUTER, openRouterKey = m[LEGACY_OPENROUTER].orEmpty())

internal fun Config.withoutSecrets(): Config = copy(aiKeys = emptyMap(), openRouterKey = "")
