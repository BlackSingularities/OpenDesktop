package com.opendesktop.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
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
import java.security.MessageDigest

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val pageUrl: String,
    /** SHA-256 pliku APK (z pola `digest` GitHuba albo z linii „SHA-256:” w opisie wydania). */
    val sha256: String? = null,
)

/**
 * Aktualizacje przez redzikowski.com. Pobrany APK jest sprawdzany przed instalacją: suma SHA-256
 * musi zgadzać się z opublikowaną, a podpis – z podpisem zainstalowanej aplikacji. Tag ma postać v<główna>.<poboczna>.<poprawka>; wersje
 * porównujemy część po części jako liczby (1.2.0 > 1.0.37), nie jako napisy.
 */
object Updater {
    /** Porównanie wersji „a.b.c” część po części; brakujące części = 0. */
    fun compareVersions(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val HEX64 = Regex("^[0-9a-fA-F]{64}$")
    private val SHA_LINE = Regex("SHA-256[^0-9a-fA-F]{0,20}([0-9a-fA-F]{64})")

    private fun s(o: Map<String, kotlinx.serialization.json.JsonElement>, k: String) =
        (o[k] as? JsonPrimitive)?.contentOrNull.orEmpty()

    /** Zwraca najnowsze wydanie, gdy jest nowsze niż zainstalowane. */
    suspend fun check(): UpdateInfo? {
        val body = try {
            Http.get("https://redzikowski.com/api/github/repos/${Config.DEFAULT_REPO}/releases/latest", mapOf("Accept" to "application/json"))
        } catch (e: HttpException) {
            if (e.code == 404) throw IOException("Brak wydań do pobrania")
            throw IOException("redzikowski.com: HTTP ${e.code}")
        }
        val o = json.parseToJsonElement(body).jsonObject
        val tag = s(o, "tag_name").removePrefix("v")
        if (compareVersions(tag, BuildConfig.VERSION_NAME) <= 0) return null
        // Identyfikator do „Pomiń tę wersję”: unikalny dla całej wersji, nie tylko poprawki
        val code = tag.split('.').map { it.toIntOrNull() ?: 0 }.let { p -> p.getOrElse(0) { 0 } * 1_000_000 + p.getOrElse(1) { 0 } * 1_000 + p.getOrElse(2) { 0 } }
        val apk = o["assets"]?.jsonArray?.map { it.jsonObject }
            ?.firstOrNull { s(it, "name").endsWith(".apk") } ?: return null
        return UpdateInfo(
            versionName = tag,
            versionCode = code,
            notes = "",
            apkUrl = s(apk, "browser_download_url"),
            sizeBytes = (apk["size"] as? JsonPrimitive)?.longOrNull ?: 0L,
            pageUrl = "",
            sha256 = s(apk, "digest").removePrefix("sha256:").takeIf { HEX64.matches(it) }
                ?: SHA_LINE.find(s(o, "body"))?.groupValues?.get(1),
        )
    }


    /** Pobiera APK do cache aplikacji, raportując postęp 0..1. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "OpenDesktop-${info.versionName}.apk")
        val req = Request.Builder().url(info.apkUrl).build()
        val sha = MessageDigest.getInstance("SHA-256")
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
                        sha.update(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        try {
            val actual = sha.digest().joinToString("") { "%02x".format(it) }
            if (info.sha256 != null && !actual.equals(info.sha256, ignoreCase = true)) {
                throw IOException("Pobrany plik jest uszkodzony lub podmieniony (suma SHA-256 się nie zgadza)")
            }
            verifySigner(context, target)
        } catch (e: IOException) {
            target.delete()
            throw e
        }
        target
    }

    /**
     * APK musi być tą samą aplikacją, podpisaną tym samym kluczem co zainstalowana wersja. Android
     * i tak odmówiłby instalacji, ale tu odrzucamy obcy plik od razu, zanim użytkownik go zobaczy.
     * Gdy systemowi nie uda się odczytać podpisów z archiwum, decyzję zostawiamy instalatorowi.
     */
    private fun verifySigner(context: Context, apk: File) {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.path, signatureFlags())
            ?: throw IOException("Pobrany plik nie jest poprawnym APK")
        if (archive.packageName != context.packageName) throw IOException("Pobrany plik to inna aplikacja (${archive.packageName})")
        // Nowy plik może mieć klucz po rotacji – wystarczy, że w jego historii jest nasz obecny podpis
        val theirLineage = signers(archive, lineage = true) ?: return
        val ourCurrent = signers(pm.getPackageInfo(context.packageName, signatureFlags()), lineage = false) ?: return
        if (!theirLineage.containsAll(ourCurrent)) throw IOException("Pobrany plik ma inny podpis niż zainstalowana aplikacja – instalacja wstrzymana")
    }

    @Suppress("DEPRECATION")
    private fun signatureFlags() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    /** Podpisy jako tekst: obecne albo (gdy [lineage]) cała historia rotacji klucza. */
    @Suppress("DEPRECATION")
    private fun signers(info: android.content.pm.PackageInfo, lineage: Boolean): Set<String>? {
        val sigs: List<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { si ->
                when {
                    si.hasMultipleSigners() -> si.apkContentsSigners?.toList()
                    lineage -> si.signingCertificateHistory?.toList()
                    else -> si.signingCertificateHistory?.lastOrNull()?.let(::listOf)
                }
            }
        } else info.signatures?.toList()
        return sigs?.takeIf { it.isNotEmpty() }?.map { it.toCharsString() }?.toSet()
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
