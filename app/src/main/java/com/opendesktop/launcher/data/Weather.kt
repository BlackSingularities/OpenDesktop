package com.opendesktop.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/** Dzień prognozy. Wartości zawsze w jednostkach metrycznych (°C, km/h, mm, hPa). */
data class WeatherDay(
    val date: LocalDate,
    val code: Int,
    val max: Double,
    val min: Double,
    /** Największe prawdopodobieństwo opadów w ciągu dnia, %. */
    val precipProb: Int = 0,
    val precipSum: Double = 0.0,
    val windMax: Double = 0.0,
    /** Przeważający kierunek wiatru w stopniach (skąd wieje). */
    val windDir: Int = 0,
    val uv: Double = 0.0,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null,
)

data class WeatherHour(
    val time: LocalDateTime,
    val temp: Double,
    val code: Int,
    val precipProb: Int,
    val precip: Double,
    val wind: Double,
    val isDay: Boolean,
    val feels: Double = temp,
    val humidity: Int = 0,
    val windDir: Int = 0,
    val gusts: Double = 0.0,
    val uv: Double = 0.0,
    val cloud: Int = 0,
    val pressure: Double = 0.0,
)

/** Miejsce z wyszukiwarki albo z lokalizacji telefonu. */
data class WeatherPlace(val name: String, val region: String, val lat: Double, val lon: Double) {
    /** Współrzędne w formacie rozumianym przez [WeatherRepo.get]. */
    val coords: String get() = "%.4f, %.4f".format(java.util.Locale.ROOT, lat, lon)
}

/** Błąd z komunikatem do pokazania wprost w widżecie. */
class WeatherProblem(message: String) : Exception(message)

data class WeatherNow(
    val place: String,
    val temp: Double,
    val feels: Double,
    val humidity: Int,
    val wind: Double,
    val code: Int,
    val isDay: Boolean,
    val days: List<WeatherDay>,
    val windDir: Int = 0,
    val gusts: Double = 0.0,
    val pressure: Double = 0.0,
    val cloud: Int = 0,
    val precip: Double = 0.0,
    /** Godziny od bieżącej (włącznie) – do 48. */
    val hours: List<WeatherHour> = emptyList(),
    /** Czas lokalny miejsca prognozy w chwili pomiaru. */
    val localTime: LocalDateTime? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    /** Przesunięcie czasu miejsca względem UTC w sekundach. */
    val utcOffset: Int = 0,
) {
    val today: WeatherDay? get() = days.firstOrNull()
}

/**
 * Pogoda z Open-Meteo (bez klucza API). Miasto zamieniane na współrzędne geokoderem
 * (albo podane wprost jako „52.23, 21.01”), wyniki trzymane w pamięci 20 minut – kilka
 * widżetów z tym samym miastem robi jedno zapytanie. Zawsze pobieramy 16 dni i 48 godzin
 * w jednostkach metrycznych; przeliczanie jednostek robi widżet.
 */
object WeatherRepo {
    private const val TTL = 20 * 60_000L
    private val cache = ConcurrentHashMap<String, Pair<Long, WeatherNow>>()
    private val places = ConcurrentHashMap<String, Triple<Double, Double, String>>()
    private val coords = Regex("""^\s*(-?\d{1,2}(?:\.\d+)?)\s*[,; ]\s*(-?\d{1,3}(?:\.\d+)?)\s*$""")

    /** Pogoda dla [city]; gdy znane są [coords] („lat, lon”), nazwa służy tylko jako podpis. */
    suspend fun get(city: String, coords: String = ""): WeatherNow {
        val name = city.trim().ifBlank { "Warszawa" }
        val key = coords.trim().ifBlank { name }.lowercase()
        cache[key]?.let { (t, w) -> if (System.currentTimeMillis() - t < TTL) return w }
        val (lat, lon, place) = places[key]
            ?: (if (coords.isNotBlank()) resolve(coords).copy(third = name) else resolve(name)).also { places[key] = it }
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m," +
            "wind_direction_10m,wind_gusts_10m,pressure_msl,cloud_cover,precipitation,is_day" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,precipitation,wind_speed_10m,is_day," +
            "apparent_temperature,relative_humidity_2m,wind_direction_10m,wind_gusts_10m,uv_index,cloud_cover,pressure_msl" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum," +
            "wind_speed_10m_max,wind_direction_10m_dominant,uv_index_max,sunrise,sunset" +
            "&timezone=auto&forecast_days=16&forecast_hours=48"
        val body = Http.get(url)
        val w = withContext(Dispatchers.Default) { parse(body, place) }
        cache[key] = System.currentTimeMillis() to w
        return w
    }

    /** Rozbiór odpowiedzi Open-Meteo; wydzielony, żeby dało się go sprawdzić testem. */
    fun parse(body: String, place: String): WeatherNow {
        val root = Json.parseToJsonElement(body).jsonObject
        val cur = root["current"]!!.jsonObject
        val daily = root["daily"]!!.jsonObject
        val hourly = root["hourly"]?.jsonObject
        fun JsonObject.arr(k: String): JsonArray? = this[k]?.jsonArray
        fun JsonArray?.num(i: Int): Double? = this?.getOrNull(i)?.jsonPrimitive?.doubleOrNull
        fun JsonArray?.time(i: Int): LocalDateTime? = this?.getOrNull(i)?.jsonPrimitive?.content?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

        val dates = daily.arr("time")!!
        val days = dates.indices.mapNotNull { i ->
            val max = daily.arr("temperature_2m_max").num(i) ?: return@mapNotNull null
            val min = daily.arr("temperature_2m_min").num(i) ?: return@mapNotNull null
            WeatherDay(
                date = LocalDate.parse(dates[i].jsonPrimitive.content),
                code = daily.arr("weather_code").num(i)?.toInt() ?: 0,
                max = max,
                min = min,
                precipProb = daily.arr("precipitation_probability_max").num(i)?.toInt() ?: 0,
                precipSum = daily.arr("precipitation_sum").num(i) ?: 0.0,
                windMax = daily.arr("wind_speed_10m_max").num(i) ?: 0.0,
                windDir = daily.arr("wind_direction_10m_dominant").num(i)?.toInt() ?: 0,
                uv = daily.arr("uv_index_max").num(i) ?: 0.0,
                sunrise = daily.arr("sunrise").time(i),
                sunset = daily.arr("sunset").time(i),
            )
        }
        val localTime = cur["time"]?.jsonPrimitive?.content?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val hours = hourly?.let { h ->
            val times = h.arr("time") ?: return@let emptyList()
            val from = localTime?.withMinute(0)
            times.indices.mapNotNull { i ->
                val t = times.time(i) ?: return@mapNotNull null
                if (from != null && t.isBefore(from)) return@mapNotNull null
                val temp = h.arr("temperature_2m").num(i) ?: return@mapNotNull null
                WeatherHour(
                    time = t,
                    temp = temp,
                    code = h.arr("weather_code").num(i)?.toInt() ?: 0,
                    precipProb = h.arr("precipitation_probability").num(i)?.toInt() ?: 0,
                    precip = h.arr("precipitation").num(i) ?: 0.0,
                    wind = h.arr("wind_speed_10m").num(i) ?: 0.0,
                    isDay = (h.arr("is_day").num(i) ?: 1.0).toInt() == 1,
                    feels = h.arr("apparent_temperature").num(i) ?: temp,
                    humidity = h.arr("relative_humidity_2m").num(i)?.toInt() ?: 0,
                    windDir = h.arr("wind_direction_10m").num(i)?.toInt() ?: 0,
                    gusts = h.arr("wind_gusts_10m").num(i) ?: 0.0,
                    uv = h.arr("uv_index").num(i) ?: 0.0,
                    cloud = h.arr("cloud_cover").num(i)?.toInt() ?: 0,
                    pressure = h.arr("pressure_msl").num(i) ?: 0.0,
                )
            }.take(48)
        } ?: emptyList()
        return WeatherNow(
            place = place,
            temp = cur.num("temperature_2m"),
            feels = cur.numOr("apparent_temperature", cur.num("temperature_2m")),
            humidity = cur.numOr("relative_humidity_2m", 0.0).toInt(),
            wind = cur.numOr("wind_speed_10m", 0.0),
            code = cur.numOr("weather_code", 0.0).toInt(),
            isDay = cur.numOr("is_day", 1.0).toInt() == 1,
            days = days,
            windDir = cur.numOr("wind_direction_10m", 0.0).toInt(),
            gusts = cur.numOr("wind_gusts_10m", 0.0),
            pressure = cur.numOr("pressure_msl", 0.0),
            cloud = cur.numOr("cloud_cover", 0.0).toInt(),
            precip = cur.numOr("precipitation", 0.0),
            hours = hours,
            localTime = localTime,
            lat = root["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            lon = root["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            utcOffset = root["utc_offset_seconds"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 0,
        )
    }

    /** Zapomina pobraną pogodę – następne zapytanie idzie do sieci (ręczne odświeżenie). */
    fun clear() = cache.clear()

    /** Podpowiedzi miejscowości do wyszukiwarki w oknie widżetu. */
    suspend fun search(query: String): List<WeatherPlace> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        coords.matchEntire(q)?.let { m ->
            val lat = m.groupValues[1].toDouble()
            val lon = m.groupValues[2].toDouble()
            if (lat in -90.0..90.0 && lon in -180.0..180.0) return listOf(WeatherPlace("%.2f, %.2f".format(java.util.Locale.ROOT, lat, lon), "Współrzędne", lat, lon))
        }
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(q, "UTF-8")}&count=8&language=pl&format=json"
        val body = Http.get(url)
        return withContext(Dispatchers.Default) { parsePlaces(body) }
    }

    fun parsePlaces(body: String): List<WeatherPlace> {
        val results = Json.parseToJsonElement(body).jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { e ->
            val r = e.jsonObject
            val lat = r["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val lon = r["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val region = listOfNotNull(r["admin1"], r["country"]).map { it.jsonPrimitive.content }.filter { it.isNotBlank() }.joinToString(", ")
            WeatherPlace(r["name"]?.jsonPrimitive?.content ?: return@mapNotNull null, region, lat, lon)
        }
    }

    private fun JsonObject.num(k: String): Double = this[k]!!.jsonPrimitive.double
    private fun JsonObject.numOr(k: String, def: Double): Double = this[k]?.jsonPrimitive?.doubleOrNull ?: def

    private suspend fun resolve(name: String): Triple<Double, Double, String> {
        coords.matchEntire(name)?.let { m ->
            val lat = m.groupValues[1].toDouble()
            val lon = m.groupValues[2].toDouble()
            if (lat in -90.0..90.0 && lon in -180.0..180.0) return Triple(lat, lon, "%.2f, %.2f".format(java.util.Locale.ROOT, lat, lon))
        }
        val q = URLEncoder.encode(name, "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=pl&format=json"
        val root = Json.parseToJsonElement(Http.get(url)).jsonObject
        val r = root["results"]?.jsonArray?.firstOrNull()?.jsonObject ?: throw WeatherProblem("Nie znam: $name")
        return Triple(r.num("latitude"), r.num("longitude"), r["name"]?.jsonPrimitive?.content ?: name)
    }

    /** Opis warunków po polsku dla kodu WMO. */
    fun describe(code: Int): String = when (code) {
        0 -> "Bezchmurnie"
        1 -> "Przeważnie pogodnie"
        2 -> "Częściowe zachmurzenie"
        3 -> "Pochmurno"
        45, 48 -> "Mgła"
        in 51..57 -> "Mżawka"
        in 61..67 -> "Deszcz"
        in 71..77 -> "Śnieg"
        in 80..82 -> "Przelotne opady"
        85, 86 -> "Przelotny śnieg"
        in 95..99 -> "Burza"
        else -> "—"
    }

    /** Krótki opis do wąskich kolumn. */
    fun describeShort(code: Int): String = when (code) {
        0 -> "Bezchmurnie"
        1 -> "Pogodnie"
        2 -> "Chmury"
        3 -> "Pochmurno"
        45, 48 -> "Mgła"
        in 51..57 -> "Mżawka"
        in 61..67 -> "Deszcz"
        in 71..77, 85, 86 -> "Śnieg"
        in 80..82 -> "Przelotny"
        in 95..99 -> "Burza"
        else -> "—"
    }
}

/** Jednostki wyświetlania; dane zawsze przychodzą metryczne. */
object WeatherUnits {
    val windNames = listOf("km/h", "m/s", "mph", "kn")
    val pressureNames = listOf("hPa", "mmHg", "inHg")

    fun temp(c: Double, fahrenheit: Boolean): Double = if (fahrenheit) c * 9.0 / 5.0 + 32.0 else c

    fun wind(kmh: Double, unit: Int): Double = when (unit) {
        1 -> kmh / 3.6
        2 -> kmh / 1.609344
        3 -> kmh / 1.852
        else -> kmh
    }

    fun pressure(hpa: Double, unit: Int): Double = when (unit) {
        1 -> hpa * 0.750062
        2 -> hpa * 0.02953
        else -> hpa
    }

    /** Kierunek, z którego wieje, jako skrót róży wiatrów. */
    fun compass(deg: Int): String {
        val names = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return names[(((deg % 360 + 360) % 360 + 22) / 45) % 8]
    }

    fun uvLabel(uv: Double): String = when {
        uv < 3 -> "niski"
        uv < 6 -> "umiarkowany"
        uv < 8 -> "wysoki"
        uv < 11 -> "bardzo wysoki"
        else -> "ekstremalny"
    }
}
