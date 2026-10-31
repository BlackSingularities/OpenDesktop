package com.opendesktop.launcher.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Święto: [free] = ustawowo wolne od pracy, inaczej zwyczajowe (Walentynki, Dzień Matki…). */
data class Holiday(val date: LocalDate, val name: String, val free: Boolean)

/** Święta w Polsce liczone w telefonie – bez sieci, także ruchome (od Wielkanocy). */
object Holidays {
    /** Niedziela Wielkanocna (algorytm Meeusa/Jonesa/Butchera, kalendarz gregoriański). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    fun forYear(year: Int): List<Holiday> {
        val e = easter(year)
        fun d(m: Int, day: Int) = LocalDate.of(year, m, day)
        val free = listOf(
            Holiday(d(1, 1), "Nowy Rok", true),
            Holiday(d(1, 6), "Trzech Króli", true),
            Holiday(e, "Wielkanoc", true),
            Holiday(e.plusDays(1), "Poniedziałek Wielkanocny", true),
            Holiday(d(5, 1), "Święto Pracy", true),
            Holiday(d(5, 3), "Święto Konstytucji 3 Maja", true),
            Holiday(e.plusDays(49), "Zielone Świątki", true),
            Holiday(e.plusDays(60), "Boże Ciało", true),
            Holiday(d(8, 15), "Wniebowzięcie NMP", true),
            Holiday(d(11, 1), "Wszystkich Świętych", true),
            Holiday(d(11, 11), "Święto Niepodległości", true),
            Holiday(d(12, 25), "Boże Narodzenie", true),
            Holiday(d(12, 26), "Drugi dzień Świąt", true),
        ) + if (year >= 2025) listOf(Holiday(d(12, 24), "Wigilia", true)) else emptyList()
        val customary = listOf(
            Holiday(d(1, 21), "Dzień Babci", false),
            Holiday(d(1, 22), "Dzień Dziadka", false),
            Holiday(e.minusDays(52), "Tłusty czwartek", false),
            Holiday(d(2, 14), "Walentynki", false),
            Holiday(e.minusDays(46), "Środa Popielcowa", false),
            Holiday(d(3, 8), "Dzień Kobiet", false),
            Holiday(d(3, 21), "Pierwszy dzień wiosny", false),
            Holiday(e.minusDays(7), "Niedziela Palmowa", false),
            Holiday(d(4, 1), "Prima aprilis", false),
            Holiday(d(5, 26), "Dzień Matki", false),
            Holiday(d(6, 1), "Dzień Dziecka", false),
            Holiday(d(6, 23), "Dzień Ojca", false),
            Holiday(d(9, 30), "Dzień Chłopaka", false),
            Holiday(d(10, 14), "Dzień Nauczyciela", false),
            Holiday(d(11, 2), "Zaduszki", false),
            Holiday(d(11, 29), "Andrzejki", false),
            Holiday(d(12, 6), "Mikołajki", false),
            Holiday(d(12, 31), "Sylwester", false),
        ) + if (year < 2025) listOf(Holiday(d(12, 24), "Wigilia", false)) else emptyList()
        return (free + customary).sortedBy { it.date }
    }

    /** Najbliższe święta od [from] (włącznie), z [customary] także zwyczajowe. */
    fun upcoming(from: LocalDate, count: Int, customary: Boolean): List<Holiday> =
        (forYear(from.year) + forYear(from.year + 1))
            .filter { !it.date.isBefore(from) && (customary || it.free) }
            .take(count)

    /**
     * Długi weekend: święto we wtorek lub czwartek daje „mostek” (jeden dzień urlopu = 4 dni wolnego),
     * w piątek lub poniedziałek – trzy dni wolnego z weekendem.
     */
    fun bridgeHint(h: Holiday): String? {
        if (!h.free) return null
        return when (h.date.dayOfWeek) {
            DayOfWeek.TUESDAY -> "mostek w poniedziałek – 4 dni wolnego"
            DayOfWeek.THURSDAY -> "mostek w piątek – 4 dni wolnego"
            DayOfWeek.FRIDAY, DayOfWeek.MONDAY -> "długi weekend – 3 dni"
            DayOfWeek.SATURDAY -> "w sobotę – należy się dzień wolny"
            else -> null
        }
    }

    /** Pierwszy dzień tygodnia zawierającego [d] (do kropek tygodnia i odliczania). */
    fun weekStart(d: LocalDate, mondayFirst: Boolean): LocalDate =
        d.with(TemporalAdjusters.previousOrSame(if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY))
}
