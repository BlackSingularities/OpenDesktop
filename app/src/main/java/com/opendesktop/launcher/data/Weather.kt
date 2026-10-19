package com.opendesktop.launcher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

data class WeatherDay(val date: LocalDate, val code: Int, val max: Double, val min: Double)

data class WeatherNow(
    val place: String,
    val temp: Double,
    val feels: Double,
    val humidity: Int,
    val wind: Double,
    val code: Int,
    val isDay: Boolean,
    val days: List<WeatherDay>,
)

/**
 * Pogoda z Open-Meteo (bez klucza API). Miasto zamieniane na współrzędne geokoderem,
 * wyniki trzymane w pamięci 20 minut – kilka widżetów z tym samym miastem robi jedno zapytanie.
 */
object WeatherRepo {
    private const val TTL = 20 * 60_000L
    private val cache = ConcurrentHashMap<String, Pair<Long, WeatherNow>>()
    private val places = ConcurrentHashMap<String, Triple<Double, Double, String>>()

    suspend fun get(city: String, fahrenheit: Boolean): WeatherNow {
        val name = city.trim().ifBlank { "Warszawa" }
        val key = "${name.lowercase()}|$fahrenheit"
        cache[key]?.let { (t, w) -> if (System.currentTimeMillis() - t < TTL) return w }
        val (lat, lon, place) = places[name.lowercase()] ?: geocode(name).also { places[name.lowercase()] = it }
        val units = if (fahrenheit) "&temperature_unit=fahrenheit&wind_speed_unit=mph" else ""
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=7$units"
        val root = Json.parseToJsonElement(Http.get(url)).jsonObject
        val cur = root["current"]!!.jsonObject
        val daily = root["daily"]!!.jsonObject
        fun arr(k: String) = daily[k]!!.jsonArray
        val dates = arr("time")
        val days = dates.indices.map { i ->
            WeatherDay(
                LocalDate.parse(dates[i].jsonPrimitive.content),
                arr("weather_code")[i].jsonPrimitive.int,
                arr("temperature_2m_max")[i].jsonPrimitive.double,
                arr("temperature_2m_min")[i].jsonPrimitive.double,
            )
        }
        val w = WeatherNow(
            place = place,
            temp = cur.num("temperature_2m"),
            feels = cur.num("apparent_temperature"),
            humidity = cur.num("relative_humidity_2m").toInt(),
            wind = cur.num("wind_speed_10m"),
            code = cur.num("weather_code").toInt(),
            isDay = cur.num("is_day").toInt() == 1,
            days = days,
        )
        cache[key] = System.currentTimeMillis() to w
        return w
    }

    private fun JsonObject.num(k: String): Double = this[k]!!.jsonPrimitive.double

    private suspend fun geocode(name: String): Triple<Double, Double, String> {
        val q = URLEncoder.encode(name, "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=pl&format=json"
        val root = Json.parseToJsonElement(Http.get(url)).jsonObject
        val r = root["results"]?.jsonArray?.firstOrNull()?.jsonObject ?: error("Nie znaleziono miejscowości: $name")
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
}
