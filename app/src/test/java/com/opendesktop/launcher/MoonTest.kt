package com.opendesktop.launcher

import com.opendesktop.launcher.domain.Moon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class MoonTest {
    // Warszawa, czas letni (UTC+2)
    private val lat = 52.23
    private val lon = 21.01

    @Test
    fun riseAndSetFallInsideTheDay() {
        val date = LocalDate.of(2024, 6, 1)
        val (rise, set) = Moon.riseSet(date, lat, lon, 7200)
        assertNotNull(rise)
        assertNotNull(set)
        listOf(rise!!, set!!).forEach { assertEquals(date, it.toLocalDate()) }
    }

    @Test
    fun crescentIsUpForHalfADay() {
        // Ubywający sierp: wschodzi nad ranem, zachodzi po południu
        val (rise, set) = Moon.riseSet(LocalDate.of(2024, 6, 1), lat, lon, 7200)
        assertTrue(rise!!.hour in 0..4)
        val up = Duration.between(rise, set!!).toHours()
        assertTrue("$up h", up in 9..16)
    }
}
