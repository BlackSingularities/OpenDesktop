package com.opendesktop.launcher.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.IOException

@Serializable
data class AiPricing(
    val prompt: String = "0",
    val completion: String = "0",
)

@Serializable
data class AiModel(
    val id: String,
    val name: String = "",
    val description: String = "",
    @SerialName("context_length") val contextLength: Long? = null,
    val pricing: AiPricing? = null,
) {
    /** Cena za milion tokenów wejścia (USD) lub null, gdy nieznana. */
    val promptPerMillion: Double? get() = pricing?.prompt?.toDoubleOrNull()?.takeIf { it >= 0 }?.times(1_000_000)
    val completionPerMillion: Double? get() = pricing?.completion?.toDoubleOrNull()?.takeIf { it >= 0 }?.times(1_000_000)
    val isFree: Boolean get() = id.endsWith(":free") || (promptPerMillion == 0.0 && completionPerMillion == 0.0)
}

@Serializable
data class AiCategory(
    val name: String,
    val emoji: String = "",
    val apps: List<String> = emptyList(),
)

@Serializable
private data class AiCategories(val categories: List<AiCategory> = emptyList())

data class AiAppRef(val packageName: String, val label: String)

/** Klient OpenRouter: lista wszystkich modeli i proste zapytania czatu. */
object OpenRouter {
    private const val BASE = "https://openrouter.ai/api/v1"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private fun headers(key: String) = mapOf(
        "Authorization" to "Bearer ${key.trim()}",
        "HTTP-Referer" to "https://github.com/${Config.DEFAULT_REPO}",
        "X-Title" to "OpenDesktop Launcher",
    )

    suspend fun models(): List<AiModel> {
        val body = Http.get("$BASE/models")
        val data = json.parseToJsonElement(body).jsonObject["data"] ?: return emptyList()
        return json.decodeFromJsonElement(ListSerializer(AiModel.serializer()), data)
            .sortedBy { it.name.ifBlank { it.id }.lowercase() }
    }

    /** Sprawdza klucz — zwraca opis limitu/wykorzystania albo rzuca wyjątek. */
    suspend fun checkKey(key: String): String {
        val body = try {
            Http.get("$BASE/key", headers(key))
        } catch (e: HttpException) {
            throw IOException(errorMessage(e))
        }
        val d = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject
        val label = (d?.get("label") as? JsonPrimitive)?.contentOrNull
        val usage = (d?.get("usage") as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
        val limit = (d?.get("limit") as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
        return buildString {
            append("Klucz działa")
            if (!label.isNullOrBlank()) append(" ($label)")
            if (usage != null) append(" · zużyto $" + "%.4f".format(usage))
            if (limit != null) append(" z $" + "%.2f".format(limit))
        }
    }

    suspend fun chat(key: String, model: String, system: String, user: String, temperature: Double = 0.2): String {
        val payload = buildJsonObject {
            put("model", model)
            put("temperature", temperature)
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", system) }
                addJsonObject { put("role", "user"); put("content", user) }
            }
        }
        val body = try {
            Http.postJson("$BASE/chat/completions", payload.toString(), headers(key))
        } catch (e: HttpException) {
            throw IOException(errorMessage(e))
        }
        val o = json.parseToJsonElement(body).jsonObject
        (o["error"] as? JsonObject)?.let { err ->
            throw IOException((err["message"] as? JsonPrimitive)?.contentOrNull ?: "Błąd OpenRouter")
        }
        val choice = o["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw IOException("Model nie zwrócił odpowiedzi")
        val content = choice["message"]?.jsonObject?.get("content")
        return (content as? JsonPrimitive)?.contentOrNull?.trim()
            ?: throw IOException("Pusta odpowiedź modelu")
    }

    private fun errorMessage(e: HttpException): String {
        val msg = runCatching {
            val err = json.parseToJsonElement(e.body).jsonObject["error"]
            ((err as? JsonObject)?.get("message") as? JsonPrimitive)?.contentOrNull
        }.getOrNull()
        return when (e.code) {
            401 -> "Nieprawidłowy klucz API (401)"
            402 -> "Brak środków na koncie OpenRouter (402)"
            429 -> "Za dużo zapytań — spróbuj za chwilę (429)"
            else -> msg ?: "Błąd HTTP ${e.code}"
        }
    }

    /** Buduje kategorie aplikacji na podstawie listy i wskazówek użytkownika. */
    suspend fun categorize(
        key: String,
        model: String,
        apps: List<AiAppRef>,
        instructions: String,
        maxCategories: Int,
        multi: Boolean,
        existing: List<String>,
    ): List<AiCategory> {
        val system = """
            Jesteś asystentem, który porządkuje aplikacje w launcherze Androida.
            Odpowiadasz WYŁĄCZNIE poprawnym obiektem JSON bez komentarzy i bez bloków kodu:
            {"categories":[{"name":"Nazwa","emoji":"🙂","apps":["nazwa.pakietu", ...]}]}
            Zasady:
            - używaj wyłącznie nazw pakietów z listy użytkownika,
            - nazwy kategorii krótkie (1–2 słowa), w języku wskazówek użytkownika (domyślnie po polsku),
            - jedna trafna emoji na kategorię,
            - najwyżej $maxCategories kategorii,
            - ${if (multi) "aplikacja może należeć do kilku kategorii, jeśli to naprawdę pasuje" else "każda aplikacja dokładnie w jednej kategorii"},
            - każda aplikacja z listy musi trafić do przynajmniej jednej kategorii.
        """.trimIndent()
        val user = buildString {
            if (instructions.isNotBlank()) appendLine("Wskazówki użytkownika: ${instructions.trim()}")
            if (existing.isNotEmpty()) appendLine("Istniejące kategorie (możesz użyć, jeśli pasują): ${existing.joinToString()}")
            appendLine("Aplikacje (pakiet | nazwa):")
            apps.forEach { appendLine("${it.packageName} | ${it.label}") }
        }
        val text = chat(key, model, system, user)
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) throw IOException("Model nie zwrócił JSON-a")
        return json.decodeFromString(AiCategories.serializer(), text.substring(start, end + 1)).categories
            .filter { it.name.isNotBlank() }
    }

    suspend fun folderName(key: String, model: String, labels: List<String>): String {
        val text = chat(
            key, model,
            "Wymyślasz krótkie (1–2 słowa) nazwy folderów na pulpicie telefonu. Odpowiedz samą nazwą, bez cudzysłowów.",
            "Aplikacje w folderze: ${labels.joinToString()}",
            temperature = 0.5,
        )
        return text.lineSequence().first().trim().trim('"', '\'', '.', '*').take(24)
    }

    /** Wybiera aplikację pasującą do opisu („czym zapłacę za parking?”). */
    suspend fun findApp(key: String, model: String, apps: List<AiAppRef>, query: String): List<String> {
        val text = chat(
            key, model,
            "Pomagasz znaleźć aplikację w telefonie. Odpowiedz WYŁĄCZNIE tablicą JSON z maksymalnie 5 nazwami pakietów z listy, od najlepiej pasującej. Np. [\"com.a\",\"com.b\"]",
            "Pytanie: $query\nAplikacje (pakiet | nazwa):\n" + apps.joinToString("\n") { "${it.packageName} | ${it.label}" },
        )
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return runCatching {
            json.parseToJsonElement(text.substring(start, end + 1)).jsonArray
                .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        }.getOrDefault(emptyList())
    }
}
