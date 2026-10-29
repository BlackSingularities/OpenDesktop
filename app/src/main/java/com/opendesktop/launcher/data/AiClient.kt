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
    /** Właściciel modelu (OpenAI, Groq itp. podają listę bez nazw i cen). */
    @SerialName("owned_by") val ownedBy: String = "",
    /** Nazwa wyświetlana z API Anthropic. */
    @SerialName("display_name") val displayName: String = "",
) {
    val title: String get() = name.ifBlank { displayName.ifBlank { id } }
    val hasPricing: Boolean get() = pricing != null

    /** Cena za milion tokenów wejścia (USD) lub null, gdy nieznana. */
    val promptPerMillion: Double? get() = pricing?.prompt?.toDoubleOrNull()?.takeIf { it >= 0 }?.times(1_000_000)
    val completionPerMillion: Double? get() = pricing?.completion?.toDoubleOrNull()?.takeIf { it >= 0 }?.times(1_000_000)
    val isFree: Boolean get() = id.endsWith(":free") || (pricing != null && promptPerMillion == 0.0 && completionPerMillion == 0.0)
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

/** Rodzaj API dostawcy: większość mówi dialektem OpenAI, Anthropic ma własny. */
enum class AiApi { OpenAi, Anthropic }

/** Obsługiwani dostawcy AI. Lista modeli pobierana jest zawsze z API wybranego dostawcy. */
@Serializable
enum class AiProvider(
    val label: String,
    val baseUrl: String,
    val keysUrl: String,
    val keyHint: String,
    val api: AiApi = AiApi.OpenAi,
    val needsKey: Boolean = true,
) {
    OpenRouter("OpenRouter", "https://openrouter.ai/api/v1", "https://openrouter.ai/keys", "sk-or-v1-…"),
    OpenAI("OpenAI", "https://api.openai.com/v1", "https://platform.openai.com/api-keys", "sk-…"),
    Anthropic("Anthropic (Claude)", "https://api.anthropic.com/v1", "https://console.anthropic.com/settings/keys", "sk-ant-…", AiApi.Anthropic),
    Gemini("Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "https://aistudio.google.com/apikey", "AIza…"),
    Groq("Groq", "https://api.groq.com/openai/v1", "https://console.groq.com/keys", "gsk_…"),
    Mistral("Mistral", "https://api.mistral.ai/v1", "https://console.mistral.ai/api-keys", "klucz Mistral"),
    DeepSeek("DeepSeek", "https://api.deepseek.com/v1", "https://platform.deepseek.com/api_keys", "sk-…"),
    XAi("xAI (Grok)", "https://api.x.ai/v1", "https://console.x.ai", "xai-…"),
    Custom("Własny serwer (zgodny z OpenAI)", "", "", "opcjonalny", needsKey = false),
}

fun Config.aiKey(p: AiProvider = aiProvider): String =
    aiKeys[p.name] ?: if (p == AiProvider.OpenRouter) openRouterKey else ""

fun Config.aiModelId(p: AiProvider = aiProvider): String =
    aiModels[p.name] ?: if (p == AiProvider.OpenRouter) aiModel else ""

fun Config.aiModelLabel(p: AiProvider = aiProvider): String =
    aiModelNames[p.name] ?: if (p == AiProvider.OpenRouter) aiModelName.ifBlank { aiModel } else aiModelId(p)

fun Config.withAiKey(key: String, p: AiProvider = aiProvider): Config = copy(aiKeys = aiKeys + (p.name to key))

fun Config.withAiModel(id: String, name: String, p: AiProvider = aiProvider): Config =
    copy(aiModels = aiModels + (p.name to id), aiModelNames = aiModelNames + (p.name to name))

/** Wszystko, czego potrzeba do zapytania: dostawca, klucz, model i (dla własnego serwera) adres. */
data class AiEndpoint(val provider: AiProvider, val key: String, val model: String, val customUrl: String = "") {
    val baseUrl: String get() = (if (provider == AiProvider.Custom) customUrl else provider.baseUrl).trim().trimEnd('/')

    companion object {
        fun of(cfg: Config, p: AiProvider = cfg.aiProvider) = AiEndpoint(p, cfg.aiKey(p).trim(), cfg.aiModelId(p), cfg.aiBaseUrl)
    }
}

/** Klient AI: lista modeli wybranego dostawcy i proste zapytania czatu. */
object AiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Modele, które nie służą do czatu (OpenAI, Mistral, Groq i inni zwracają wszystko naraz)
    private val nonChat = listOf(
        "embed", "whisper", "tts", "dall-e", "moderation", "image", "audio", "realtime",
        "transcribe", "davinci", "babbage", "sora", "guard", "ocr",
    )

    // Modele „rozumujące” OpenAI (o1, o3, gpt-5…) przyjmują tylko domyślną temperaturę
    private val fixedTemperature = Regex("^(o\\d|gpt-5)")

    private fun headers(e: AiEndpoint): Map<String, String> = when (e.provider.api) {
        AiApi.Anthropic -> mapOf("x-api-key" to e.key, "anthropic-version" to "2023-06-01")
        AiApi.OpenAi -> buildMap {
            if (e.key.isNotBlank()) put("Authorization", "Bearer ${e.key}")
            if (e.provider == AiProvider.OpenRouter) {
                put("HTTP-Referer", "https://redzikowski.com/portfolio/opendesktop")
                put("X-Title", "OpenDesktop Launcher")
            }
        }
    }

    private fun requireUrl(e: AiEndpoint) {
        if (e.baseUrl.isBlank()) throw IOException("Podaj adres serwera, np. http://192.168.1.10:11434/v1")
    }

    /** Modele dostępne u dostawcy (OpenRouter podaje listę bez klucza, pozostali wymagają klucza). */
    suspend fun models(e: AiEndpoint): List<AiModel> {
        requireUrl(e)
        if (e.provider.needsKey && e.provider != AiProvider.OpenRouter && e.key.isBlank()) {
            throw IOException("Wpisz klucz API ${e.provider.label}, aby pobrać listę modeli")
        }
        val url = "${e.baseUrl}/models" + if (e.provider == AiProvider.Anthropic) "?limit=1000" else ""
        val body = try {
            Http.get(url, headers(e))
        } catch (ex: HttpException) {
            throw IOException(errorMessage(e, ex))
        }
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"] ?: root["models"] ?: return emptyList()
        val list = json.decodeFromJsonElement(ListSerializer(AiModel.serializer()), data)
            // Gemini zwraca „models/gemini-…” – w zapytaniach wystarczy sama nazwa
            .map { if (it.id.startsWith("models/")) it.copy(id = it.id.removePrefix("models/")) else it }
        val chat = if (e.provider == AiProvider.OpenRouter || e.provider == AiProvider.Custom) list
        else list.filter { m -> nonChat.none { m.id.contains(it, ignoreCase = true) } }
        return chat.distinctBy { it.id }.sortedBy { it.title.lowercase() }
    }

    /** Sprawdza klucz – zwraca krótki opis albo rzuca wyjątek. */
    suspend fun checkKey(e: AiEndpoint): String {
        if (e.provider != AiProvider.OpenRouter) {
            val n = models(e).size
            return "Połączenie działa · dostępnych modeli: $n"
        }
        val body = try {
            Http.get("${e.baseUrl}/key", headers(e))
        } catch (ex: HttpException) {
            throw IOException(errorMessage(e, ex))
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

    suspend fun chat(e: AiEndpoint, system: String, user: String, temperature: Double = 0.2): String {
        requireUrl(e)
        return when (e.provider.api) {
            AiApi.OpenAi -> chatOpenAi(e, system, user, temperature)
            AiApi.Anthropic -> chatAnthropic(e, system, user, temperature)
        }
    }

    private suspend fun chatOpenAi(e: AiEndpoint, system: String, user: String, temperature: Double): String {
        val payload = buildJsonObject {
            put("model", e.model)
            if (!(e.provider == AiProvider.OpenAI && fixedTemperature.containsMatchIn(e.model))) put("temperature", temperature)
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", system) }
                addJsonObject { put("role", "user"); put("content", user) }
            }
        }
        val body = try {
            Http.postJson("${e.baseUrl}/chat/completions", payload.toString(), headers(e))
        } catch (ex: HttpException) {
            throw IOException(errorMessage(e, ex))
        }
        val o = json.parseToJsonElement(body).jsonObject
        errorText(o)?.let { throw IOException(it) }
        val choice = o["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw IOException("Model nie zwrócił odpowiedzi")
        val content = choice["message"]?.jsonObject?.get("content")
        return (content as? JsonPrimitive)?.contentOrNull?.trim()
            ?: throw IOException("Pusta odpowiedź modelu")
    }

    private suspend fun chatAnthropic(e: AiEndpoint, system: String, user: String, temperature: Double): String {
        val payload = buildJsonObject {
            put("model", e.model)
            put("max_tokens", 4096)
            put("temperature", temperature)
            put("system", system)
            putJsonArray("messages") { addJsonObject { put("role", "user"); put("content", user) } }
        }
        val body = try {
            Http.postJson("${e.baseUrl}/messages", payload.toString(), headers(e))
        } catch (ex: HttpException) {
            throw IOException(errorMessage(e, ex))
        }
        val o = json.parseToJsonElement(body).jsonObject
        errorText(o)?.let { throw IOException(it) }
        val text = o["content"]?.jsonArray
            ?.mapNotNull { (it as? JsonObject)?.get("text") as? JsonPrimitive }
            ?.joinToString("") { it.contentOrNull.orEmpty() }
            ?.trim()
        if (text.isNullOrEmpty()) throw IOException("Pusta odpowiedź modelu")
        return text
    }

    private fun errorText(o: JsonObject): String? = when (val err = o["error"]) {
        null -> null
        is JsonObject -> (err["message"] as? JsonPrimitive)?.contentOrNull ?: "Błąd dostawcy AI"
        is JsonPrimitive -> err.contentOrNull
        else -> "Błąd dostawcy AI"
    }

    private fun errorMessage(e: AiEndpoint, ex: HttpException): String {
        // Gemini zwraca błąd jako tablicę z jednym obiektem
        val msg = runCatching {
            val p = json.parseToJsonElement(ex.body)
            errorText((p as? JsonObject) ?: p.jsonArray.first().jsonObject)
        }.getOrNull()
        return when (ex.code) {
            401, 403 -> "Nieprawidłowy klucz API ${e.provider.label} (${ex.code})"
            402 -> "Brak środków na koncie ${e.provider.label} (402)"
            404 -> msg ?: "Nie znaleziono – sprawdź adres serwera i model (404)"
            429 -> "Za dużo zapytań lub wyczerpany limit – spróbuj za chwilę (429)"
            else -> msg ?: "Błąd HTTP ${ex.code}"
        }
    }

    /** Buduje kategorie aplikacji na podstawie listy i wskazówek użytkownika. */
    suspend fun categorize(
        e: AiEndpoint,
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
        val text = chat(e, system, user)
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) throw IOException("Model nie zwrócił JSON-a")
        return json.decodeFromString(AiCategories.serializer(), text.substring(start, end + 1)).categories
            .filter { it.name.isNotBlank() }
    }

    /** Wybiera aplikację pasującą do opisu („czym zapłacę za parking?”). */
    suspend fun findApp(e: AiEndpoint, apps: List<AiAppRef>, query: String): List<String> {
        val text = chat(
            e,
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
