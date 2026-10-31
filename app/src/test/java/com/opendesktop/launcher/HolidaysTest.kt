package com.opendesktop.launcher

import com.opendesktop.launcher.domain.Holidays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidaysTest {
    @Test
    fun easterDates() {
        assertEquals(LocalDate.of(2024, 3, 31), Holidays.easter(2024))
        assertEquals(LocalDate.of(2025, 4, 20), Holidays.easter(2025))
        assertEquals(LocalDate.of(2026, 4, 5), Holidays.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Holidays.easter(2027))
    }

    @Test
    fun movableHolidays2026() {
        val y = Holidays.forYear(2026).associate { it.name to it.date }
        assertEquals(LocalDate.of(2026, 5, 24), y["Zielone Świątki"])
        assertEquals(LocalDate.of(2026, 6, 4), y["Boże Ciało"])
        assertEquals(LocalDate.of(2026, 2, 12), y["Tłusty czwartek"])
    }

    @Test
    fun christmasEveIsFreeFrom2025() {
        assertTrue(Holidays.forYear(2025).first { it.name == "Wigilia" }.free)
        assertTrue(!Holidays.forYear(2024).first { it.name == "Wigilia" }.free)
        assertEquals(14, Holidays.forYear(2026).count { it.free })
    }

    @Test
    fun upcomingCrossesYear() {
        val next = Holidays.upcoming(LocalDate.of(2026, 12, 27), 2, customary = false)
        assertEquals(listOf("Nowy Rok", "Trzech Króli"), next.map { it.name })
        assertEquals(2027, next.first().date.year)
    }
}
