package com.opendesktop.launcher

import com.opendesktop.launcher.data.WeatherRepo
import com.opendesktop.launcher.data.WeatherUnits
import com.opendesktop.launcher.domain.Moon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeatherTest {
    private val sample = """
        {
          "current": {"time": "2026-10-29T15:45", "temperature_2m": 9.4, "apparent_temperature": 6.8,
            "relative_humidity_2m": 81, "weather_code": 3, "wind_speed_10m": 14.2, "wind_direction_10m": 250,
            "wind_gusts_10m": 30.1, "pressure_msl": 1012.6, "cloud_cover": 96, "precipitation": 0.0, "is_day": 1},
          "hourly": {
            "time": ["2026-10-29T14:00", "2026-10-29T15:00", "2026-10-29T16:00", "2026-10-29T17:00"],
            "temperature_2m": [9.9, 9.5, 8.7, null],
            "weather_code": [3, 3, 61, 61],
            "precipitation_probability": [5, 10, 55, 70],
            "precipitation": [0.0, 0.0, 0.4, 1.1],
            "wind_speed_10m": [12.0, 14.0, 15.5, 16.0],
            "is_day": [1, 1, 0, 0]
          },
          "daily": {
            "time": ["2026-10-29", "2026-10-30"],
            "weather_code": [61, 2],
            "temperature_2m_max": [11.2, 12.8],
            "temperature_2m_min": [5.1, 4.0],
            "precipitation_probability_max": [70, null],
            "precipitation_sum": [3.4, 0.0],
            "wind_speed_10m_max": [22.0, 15.0],
            "wind_direction_10m_dominant": [245, 180],
            "uv_index_max": [1.2, 2.5],
            "sunrise": ["2026-10-29T06:35", "2026-10-30T06:37"],
            "sunset": ["2026-10-29T16:22", "2026-10-30T16:20"]
          }
        }
    """.trimIndent()

    @Test
    fun parsesCurrentDaysAndHours() {
        val w = WeatherRepo.parse(sample, "Warszawa")
        assertEquals("Warszawa", w.place)
        assertEquals(9.4, w.temp, 1e-9)
        assertEquals(250, w.windDir)
        assertEquals(1012.6, w.pressure, 1e-9)
        assertEquals(LocalDateTime.of(2026, 10, 29, 15, 45), w.localTime)

        assertEquals(2, w.days.size)
        assertEquals(70, w.days[0].precipProb)
        assertEquals(0, w.days[1].precipProb) // null w danych
        assertEquals(LocalDateTime.of(2026, 10, 29, 16, 22), w.days[0].sunset)

        // Godziny od bieżącej (15:00), bez punktu bez temperatury
        assertEquals(listOf(15, 16), w.hours.map { it.time.hour })
        assertEquals(55, w.hours[1].precipProb)
        assertTrue(!w.hours[1].isDay)
    }

    @Test
    fun parsesWithoutHourly() {
        val w = WeatherRepo.parse(sample.replace(Regex(""""hourly": \{[^}]*},"""), ""), "X")
        assertTrue(w.hours.isEmpty())
    }

    @Test
    fun unitConversions() {
        assertEquals(50.0, WeatherUnits.temp(10.0, fahrenheit = true), 1e-9)
        assertEquals(10.0, WeatherUnits.wind(36.0, 1), 1e-9)
        assertEquals(750.062, WeatherUnits.pressure(1000.0, 1), 1e-6)
        assertEquals("N", WeatherUnits.compass(0))
        assertEquals("N", WeatherUnits.compass(350))
        assertEquals("E", WeatherUnits.compass(90))
        assertEquals("SW", WeatherUnits.compass(225))
        assertEquals("W", WeatherUnits.compass(-90))
    }

    @Test
    fun moonPhases() {
        // Pełnia 25.01.2024, nów 11.01.2024, pełnia 17.10.2024
        assertEquals("Pełnia", Moon.name(Moon.phase(LocalDate.of(2024, 1, 25))))
        assertEquals("Nów", Moon.name(Moon.phase(LocalDate.of(2024, 1, 11))))
        assertEquals("Pełnia", Moon.name(Moon.phase(LocalDate.of(2024, 10, 17))))
        assertTrue(Moon.illumination(Moon.phase(LocalDate.of(2024, 1, 25))) > 97)
        assertEquals(0, Moon.daysUntil(Moon.phase(LocalDate.of(2024, 1, 25)), 0.5))
        assertTrue(Moon.daysUntil(Moon.phase(LocalDate.of(2024, 1, 11)), 0.5) in 14..15)
        assertEquals(0, Moon.daysUntil(Moon.phase(LocalDate.of(2024, 1, 11)), 0.0))
    }
}
