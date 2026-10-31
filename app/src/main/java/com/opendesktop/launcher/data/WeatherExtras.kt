package com.opendesktop.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Jakość powietrza teraz (µg/m³, indeks europejski 0–100+) i prognoza indeksu na kolejne godziny. */
data class AirNow(
    val aqi: Int,
    val pm25: Double?,
    val pm10: Double?,
    val no2: Double?,
    val o3: Double?,
    val so2: Double?,
    val co: Double?,
    /** Pyłki (ziarna/m³) – tylko Europa i tylko w sezonie; nazwa → wartość. */
    val pollen: Map<String, Double>,
    /** Indeks na kolejne godziny (od bieżącej). */
    val hours: List<Pair<LocalDateTime, Int>>,
    val fetchedAt: Long = System.currentTimeMillis(),
)

/** Opady co 15 minut na najbliższe godziny (mm na kwadrans). */
data class RainSlot(val time: LocalDateTime, val mm: Double, val code: Int)

/**
 * Dodatkowe dane z Open-Meteo dla współrzędnych miejsca pogody: jakość powietrza i opady co 15 minut.
 * Każde w osobnym zapytaniu – kłopot jednego nie psuje zwykłej prognozy.
 */
object WeatherExtras {
    private const val TTL = 20 * 60_000L
    private val air = ConcurrentHashMap<String, Pair<Long, AirNow>>()
    private val rain = ConcurrentHashMap<String, Pair<Long, List<RainSlot>>>()

    private fun key(lat: Double, lon: Double) = "%.3f,%.3f".format(Locale.ROOT, lat, lon)

    fun clear() {
        air.clear()
        rain.clear()
    }

    suspend fun air(lat: Double, lon: Double): AirNow {
        val k = key(lat, lon)
        air[k]?.let { (t, a) -> if (System.currentTimeMillis() - t < TTL) return a }
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon" +
            "&current=european_aqi,pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide," +
            "alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen" +
            "&hourly=european_aqi&forecast_days=2&timezone=auto"
        val body = Http.get(url)
        val a = withContext(Dispatchers.Default) { parseAir(body) }
        air[k] = System.currentTimeMillis() to a
        return a
    }

    suspend fun rain(lat: Double, lon: Double): List<RainSlot> {
        val k = key(lat, lon)
        rain[k]?.let { (t, r) -> if (System.currentTimeMillis() - t < TTL / 2) return r }
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&minutely_15=precipitation,weather_code&forecast_minutely_15=24&timezone=auto"
        val body = Http.get(url)
        val r = withContext(Dispatchers.Default) { parseRain(body) }
        rain[k] = System.currentTimeMillis() to r
        return r
    }

    private fun JsonArray?.num(i: Int): Double? = this?.getOrNull(i)?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.arr(k: String): JsonArray? = this[k]?.jsonArray
    private fun JsonObject.numOrNull(k: String): Double? = this[k]?.jsonPrimitive?.doubleOrNull
    private fun JsonArray?.time(i: Int): LocalDateTime? =
        this?.getOrNull(i)?.jsonPrimitive?.content?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    /** Polskie nazwy pyłków w kolejności pokazywania. */
    val pollenNames = listOf(
        "alder_pollen" to "Olsza",
        "birch_pollen" to "Brzoza",
        "grass_pollen" to "Trawy",
        "mugwort_pollen" to "Bylica",
        "ragweed_pollen" to "Ambrozja",
        "olive_pollen" to "Oliwka",
    )

    fun parseAir(body: String): AirNow {
        val root = Json.parseToJsonElement(body).jsonObject
        val cur = root["current"]?.jsonObject ?: throw WeatherProblem("Brak danych o powietrzu")
        val now = cur["time"]?.jsonPrimitive?.content?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val hourly = root["hourly"]?.jsonObject
        val hours = hourly?.let { h ->
            val times = h.arr("time") ?: return@let emptyList()
            val from = now?.withMinute(0)
            times.indices.mapNotNull { i ->
                val t = times.time(i) ?: return@mapNotNull null
                if (from != null && t.isBefore(from)) return@mapNotNull null
                val v = h.arr("european_aqi").num(i) ?: return@mapNotNull null
                t to v.toInt()
            }.take(24)
        } ?: emptyList()
        return AirNow(
            aqi = cur.numOrNull("european_aqi")?.toInt() ?: hours.firstOrNull()?.second ?: throw WeatherProblem("Brak indeksu jakości powietrza"),
            pm25 = cur.numOrNull("pm2_5"),
            pm10 = cur.numOrNull("pm10"),
            no2 = cur.numOrNull("nitrogen_dioxide"),
            o3 = cur.numOrNull("ozone"),
            so2 = cur.numOrNull("sulphur_dioxide"),
            co = cur.numOrNull("carbon_monoxide"),
            pollen = pollenNames.mapNotNull { (k, _) -> cur.numOrNull(k)?.let { k to it } }.toMap(),
            hours = hours,
        )
    }

    fun parseRain(body: String): List<RainSlot> {
        val root = Json.parseToJsonElement(body).jsonObject
        val m = root["minutely_15"]?.jsonObject ?: throw WeatherProblem("Brak prognozy opadów")
        val times = m.arr("time") ?: return emptyList()
        return times.indices.mapNotNull { i ->
            val t = times.time(i) ?: return@mapNotNull null
            RainSlot(t, m.arr("precipitation").num(i) ?: 0.0, m.arr("weather_code").num(i)?.toInt() ?: 0)
        }
    }

    /** Opis indeksu europejskiego (EAQI) i jego kolor (ARGB). */
    fun aqiLevel(aqi: Int): Pair<String, Long> = when {
        aqi <= 20 -> "Bardzo dobra" to 0xFF50F0E6
        aqi <= 40 -> "Dobra" to 0xFF50CCAA
        aqi <= 60 -> "Umiarkowana" to 0xFFF0E641
        aqi <= 80 -> "Słaba" to 0xFFFF5050
        aqi <= 100 -> "Zła" to 0xFF960032
        else -> "Bardzo zła" to 0xFF7D2181
    }

    /** Poziom pyłków: 0 = brak, 1 = niski, 2 = średni, 3 = wysoki (progi wspólne dla drzew i traw). */
    fun pollenLevel(v: Double): Int = when {
        v < 1 -> 0
        v < 20 -> 1
        v < 100 -> 2
        else -> 3
    }
}
