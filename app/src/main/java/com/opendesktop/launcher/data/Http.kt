package com.opendesktop.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class HttpException(val code: Int, val body: String) : IOException("HTTP $code")

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // Także przekierowania (followRedirects) przechodzą przez ten strażnik
        .addNetworkInterceptor(Interceptor { chain ->
            val url = chain.request().url
            if (!url.isHttps && !isLocalHost(url)) {
                throw IOException("Połączenie bez szyfrowania (http) jest dozwolone tylko w sieci lokalnej – użyj adresu https://")
            }
            chain.proceed(chain.request())
        })
        .build()

    /**
     * Adres w sieci lokalnej: localhost, *.local, *.lan, IP prywatne (10/8, 172.16/12, 192.168/16),
     * pętla zwrotna, link-local i IPv6 ULA (fc00::/7). Nazwy domen nie są tu rozwiązywane przez DNS –
     * liczy się tylko to, co wpisano w adresie.
     */
    fun isLocalHost(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home.arpa")) return true
        val literal = host.all { it.isDigit() || it == '.' } || host.contains(':')
        if (!literal) return false
        val addr = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return false
        if (addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress) return true
        val b = addr.address
        return b.size == 16 && (b[0].toInt() and 0xFE) == 0xFC
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", "OpenDesktop-Launcher")
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        client.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw HttpException(r.code, body)
            body
        }
    }

    suspend fun postJson(url: String, json: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", "OpenDesktop-Launcher")
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw HttpException(r.code, body)
            body
        }
    }
}
