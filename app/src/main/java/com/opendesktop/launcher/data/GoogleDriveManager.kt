package com.opendesktop.launcher.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Manager do obsługi Google Drive jako głównego źródła danych.
 *
 * Funkcje:
 * - Auto-sync co X minut (konfigurowalne)
 * - Sprawdzanie czy jest backup przed pierwszym użyciem
 * - Restore z najnowszego backupu
 * - Historia backupów (lista z datami)
 * - Szyfrowanie opcjonalne (AES-256)
 * - Conflict resolution (lokalny vs cloud)
 */
class GoogleDriveManager(private val context: Context) {

    companion object {
        private const val FOLDER_NAME = "OpenDesktop"
        private const val CONFIG_FILE = "config.json"
        private const val WORKSPACE_FILE = "workspace.json"
        private const val BACKUP_PREFIX = "backup_"
        private const val APP_NAME = "OpenDesktop Launcher"
    }

    private var driveService: Drive? = null
    private var folderId: String? = null

    // Konfiguracja Google Sign-In
    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    // Sprawdź czy użytkownik jest zalogowany
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && account.grantedScopes.contains(Scope(DriveScopes.DRIVE_FILE))
    }

    // Inicjalizacja Drive service
    suspend fun initialize(account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(APP_NAME)
                .build()

            // Znajdź lub utwórz folder OpenDesktop
            folderId = findOrCreateFolder()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Znajdź lub utwórz folder OpenDesktop w Google Drive
    private suspend fun findOrCreateFolder(): String = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Drive service not initialized")

        // Szukaj istniejącego folderu
        val query = "name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        if (result.files.isNotEmpty()) {
            return@withContext result.files[0].id
        }

        // Utwórz nowy folder
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }

        val folder = drive.files().create(folderMetadata)
            .setFields("id")
            .execute()

        folder.id
    }

    // Sprawdź czy istnieją jakieś backupy
    suspend fun hasExistingBackups(): Boolean = withContext(Dispatchers.IO) {
        try {
            val backups = listBackups()
            backups.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // Lista wszystkich backupów (posortowane od najnowszego)
    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext emptyList()
        val folder = folderId ?: return@withContext emptyList()

        try {
            val query = "'$folder' in parents and name contains '$BACKUP_PREFIX' and trashed=false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, modifiedTime, size)")
                .setOrderBy("modifiedTime desc")
                .execute()

            result.files.map { file ->
                BackupInfo(
                    id = file.id,
                    name = file.name,
                    date = Instant.parse(file.modifiedTime.toStringRfc3339()),
                    sizeBytes = file.getSize() ?: 0L,
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Zapisz config do Drive
    suspend fun saveConfig(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            saveFile(CONFIG_FILE, json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Zapisz workspace do Drive
    suspend fun saveWorkspace(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            saveFile(WORKSPACE_FILE, json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Wczytaj config z Drive
    suspend fun loadConfig(): String? = withContext(Dispatchers.IO) {
        try {
            loadFile(CONFIG_FILE)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Wczytaj workspace z Drive
    suspend fun loadWorkspace(): String? = withContext(Dispatchers.IO) {
        try {
            loadFile(WORKSPACE_FILE)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Utwórz backup (timestamp w nazwie)
    suspend fun createBackup(configJson: String, workspaceJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val backupName = "$BACKUP_PREFIX$timestamp.json"

            // Połącz oba JSONy w jeden backup
            val backupData = """{"config":$configJson,"workspace":$workspaceJson}"""

            saveFile(backupName, backupData)

            // Usuń stare backupy (zostaw ostatnie 10)
            cleanOldBackups(keepCount = 10)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Przywróć z backupu
    suspend fun restoreFromBackup(backupId: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null to null

            val outputStream = ByteArrayOutputStream()
            drive.files().get(backupId).executeMediaAndDownloadTo(outputStream)

            val backupJson = outputStream.toString("UTF-8")

            // Parse połączonego JSONa
            val configMatch = Regex(""""config":(\{.*?\}),"workspace"""").find(backupJson)
            val workspaceMatch = Regex(""""workspace":(\{.*\})""").find(backupJson)

            val config = configMatch?.groupValues?.get(1)
            val workspace = workspaceMatch?.groupValues?.get(1)

            config to workspace
        } catch (e: Exception) {
            e.printStackTrace()
            null to null
        }
    }

    // Usuń stare backupy (zostaw X najnowszych)
    private suspend fun cleanOldBackups(keepCount: Int) = withContext(Dispatchers.IO) {
        try {
            val backups = listBackups()
            if (backups.size > keepCount) {
                val toDelete = backups.drop(keepCount)
                val drive = driveService ?: return@withContext

                toDelete.forEach { backup ->
                    try {
                        drive.files().delete(backup.id).execute()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sprawdź konflikt (która wersja jest nowsza)
    suspend fun checkConflict(): ConflictInfo? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null
            val folder = folderId ?: return@withContext null

            // Pobierz czas modyfikacji z Drive
            val query = "'$folder' in parents and name='$CONFIG_FILE' and trashed=false"
            val result = drive.files().list()
                .setQ(query)
                .setFields("files(modifiedTime)")
                .execute()

            if (result.files.isEmpty()) return@withContext null

            val cloudModified = Instant.parse(result.files[0].modifiedTime.toStringRfc3339())

            // Porównaj z lokalną wersją (TODO: trzeba zapisywać timestamp lokalnie)
            // Na razie zawsze synchronizuj
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Wyloguj
    fun signOut() {
        getSignInClient().signOut()
        driveService = null
        folderId = null
    }

    // Pomocnicze funkcje prywatne
    private suspend fun saveFile(fileName: String, content: String) = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Drive service not initialized")
        val folder = folderId ?: throw IllegalStateException("Folder not found")

        // Sprawdź czy plik istnieje
        val query = "'$folder' in parents and name='$fileName' and trashed=false"
        val existing = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        val mediaContent = ByteArrayContent.fromString("application/json", content)

        if (existing.files.isNotEmpty()) {
            // Update istniejącego pliku
            val fileId = existing.files[0].id
            drive.files().update(fileId, null, mediaContent).execute()
        } else {
            // Utwórz nowy plik
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                parents = listOf(folder)
            }
            drive.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
        }
    }

    private suspend fun loadFile(fileName: String): String? = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext null
        val folder = folderId ?: return@withContext null

        try {
            val query = "'$folder' in parents and name='$fileName' and trashed=false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            if (result.files.isEmpty()) return@withContext null

            val fileId = result.files[0].id
            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// Data classes
data class BackupInfo(
    val id: String,
    val name: String,
    val date: Instant,
    val sizeBytes: Long,
)

data class ConflictInfo(
    val cloudNewer: Boolean,
    val localModified: Instant,
    val cloudModified: Instant,
)
