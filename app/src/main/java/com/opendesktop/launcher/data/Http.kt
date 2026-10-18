package com.opendesktop.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpException(val code: Int, val body: String) : IOException("HTTP $code")

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

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
