package com.opendesktop.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.core.content.FileProvider
import com.opendesktop.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URLEncoder

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val pageUrl: String,
)

data class RepoHit(val fullName: String, val description: String, val stars: Int, val url: String)

/**
 * Aktualizacje z GitHub Releases. Tag ma postać v1.0.<N>, gdzie N = versionCode
 * (numer przebiegu workflow), więc porównujemy liczby, nie napisy.
 */
object Updater {
    private val json = Json { ignoreUnknownKeys = true }

    private fun s(o: Map<String, kotlinx.serialization.json.JsonElement>, k: String) =
        (o[k] as? JsonPrimitive)?.contentOrNull.orEmpty()

    /** Zwraca najnowsze wydanie, gdy jest nowsze niż zainstalowane. */
    suspend fun check(repo: String): UpdateInfo? {
        val body = try {
            Http.get("https://api.github.com/repos/${repo.trim()}/releases/latest", mapOf("Accept" to "application/vnd.github+json"))
        } catch (e: HttpException) {
            if (e.code == 404) throw IOException("Repozytorium $repo nie ma jeszcze wydań")
            throw IOException("GitHub: HTTP ${e.code}")
        }
        val o = json.parseToJsonElement(body).jsonObject
        val tag = s(o, "tag_name").removePrefix("v")
        val code = tag.substringAfterLast('.').toIntOrNull() ?: return null
        if (code <= BuildConfig.VERSION_CODE) return null
        val apk = o["assets"]?.jsonArray?.map { it.jsonObject }
            ?.firstOrNull { s(it, "name").endsWith(".apk") } ?: return null
        return UpdateInfo(
            versionName = tag,
            versionCode = code,
            notes = s(o, "body"),
            apkUrl = s(apk, "browser_download_url"),
            sizeBytes = (apk["size"] as? JsonPrimitive)?.longOrNull ?: 0L,
            pageUrl = s(o, "html_url"),
        )
    }

    /** Wyszukiwanie repozytorium z wydaniami OpenDesktop na GitHubie. */
    suspend fun searchRepos(query: String): List<RepoHit> {
        val q = URLEncoder.encode("${query.trim()} in:name,description", "UTF-8")
        val body = Http.get("https://api.github.com/search/repositories?q=$q&per_page=20&sort=stars")
        return json.parseToJsonElement(body).jsonObject["items"]?.jsonArray.orEmpty().map { it.jsonObject }.map {
            RepoHit(
                fullName = s(it, "full_name"),
                description = s(it, "description"),
                stars = (it["stargazers_count"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0,
                url = s(it, "html_url"),
            )
        }
    }

    /** Pobiera APK do cache aplikacji, raportując postęp 0..1. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "OpenDesktop-${info.versionName}.apk")
        val req = Request.Builder().url(info.apkUrl).build()
        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("Pusta odpowiedź")
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
            body.byteStream().use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        target
    }

    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
