package com.opendesktop.launcher

import com.opendesktop.launcher.data.WeatherExtras
import com.opendesktop.launcher.ui.desktop.rainSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WeatherExtrasTest {
    @Test
    fun parsesAirQuality() {
        val body = """
            {"current":{"time":"2026-10-31T12:00","european_aqi":34,"pm2_5":8.4,"pm10":15.1,"nitrogen_dioxide":12.0,
             "ozone":40.0,"sulphur_dioxide":1.2,"carbon_monoxide":180.0,"birch_pollen":0.0,"grass_pollen":3.5},
             "hourly":{"time":["2026-10-31T11:00","2026-10-31T12:00","2026-10-31T13:00"],"european_aqi":[30,34,null]}}
        """.trimIndent()
        val a = WeatherExtras.parseAir(body)
        assertEquals(34, a.aqi)
        assertEquals(8.4, a.pm25!!, 0.001)
        // Godziny od bieżącej; brakująca wartość odpada
        assertEquals(1, a.hours.size)
        assertEquals(3.5, a.pollen["grass_pollen"]!!, 0.001)
        assertEquals("Dobra", WeatherExtras.aqiLevel(a.aqi).first)
    }

    @Test
    fun rainSummaryFindsStart() {
        val t0 = LocalDateTime.of(2026, 10, 31, 12, 0)
        val body = """{"minutely_15":{"time":["2026-10-31T12:00","2026-10-31T12:15","2026-10-31T12:30","2026-10-31T12:45"],
            "precipitation":[0.0,0.0,0.4,0.6],"weather_code":[3,3,61,61]}}"""
        val slots = WeatherExtras.parseRain(body)
        assertEquals(4, slots.size)
        val s = rainSummary(slots, t0, use24h = true)
        assertTrue(s, s.startsWith("Opady za 30 min"))
        assertTrue(rainSummary(slots.drop(2), t0.plusMinutes(30), true).startsWith("Pada"))
    }
}
