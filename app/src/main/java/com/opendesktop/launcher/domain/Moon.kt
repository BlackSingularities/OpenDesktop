package com.opendesktop.launcher.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

/** Faza Księżyca liczona lokalnie, bez sieci – dokładność rzędu kilku godzin wystarcza do widżetu. */
object Moon {
    /** Nów odniesienia: 6 stycznia 2000, 18:14 UTC (w dniach epoki). */
    private const val NEW_MOON_EPOCH = 10962.7597

    /** Miesiąc synodyczny w dniach. */
    const val SYNODIC = 29.530588853

    /** Faza w danym dniu (w południe): 0 = nów, 0,5 = pełnia, rośnie do 1. */
    fun phase(date: LocalDate): Double {
        val x = (date.toEpochDay() + 0.5 - NEW_MOON_EPOCH) / SYNODIC
        return x - floor(x)
    }

    /** Oświetlona część tarczy w procentach. */
    fun illumination(phase: Double): Int = ((1 - cos(2 * PI * phase)) / 2 * 100).roundToInt()

    fun name(phase: Double): String = when {
        phase < 0.0339 || phase > 0.9661 -> "Nów"
        phase < 0.2161 -> "Przybywający sierp"
        phase < 0.2839 -> "Pierwsza kwadra"
        phase < 0.4661 -> "Przybywający garb"
        phase < 0.5339 -> "Pełnia"
        phase < 0.7161 -> "Ubywający garb"
        phase < 0.7839 -> "Ostatnia kwadra"
        else -> "Ubywający sierp"
    }

    /**
     * Dni do fazy [target] (0 = nów, 0,5 = pełnia); 0, gdy wypada w ciągu ¾ doby w przód lub w tył
     * (średni ruch Księżyca myli się do kilkunastu godzin).
     */
    fun daysUntil(phase: Double, target: Double): Int {
        val d = ((target - phase + 1.0) % 1.0) * SYNODIC
        return if (d < 0.75 || d > SYNODIC - 0.75) 0 else d.roundToInt()
    }

    // ----- Wschód i zachód (uproszczona teoria ruchu Księżyca, dokładność kilku minut) -----

    private const val RAD = PI / 180
    private const val OBLIQUITY = RAD * 23.4397

    private fun moonAltitude(epochMs: Double, lat: Double, lon: Double): Double {
        val d = epochMs / 86_400_000.0 - 0.5 + 2440588 - 2451545
        val l0 = RAD * (218.316 + 13.176396 * d)
        val m = RAD * (134.963 + 13.064993 * d)
        val f = RAD * (93.272 + 13.229350 * d)
        val l = l0 + RAD * 6.289 * sin(m)
        val b = RAD * 5.128 * sin(f)
        val ra = atan2(sin(l) * cos(OBLIQUITY) - tan(b) * sin(OBLIQUITY), cos(l))
        val dec = asin(sin(b) * cos(OBLIQUITY) + cos(b) * sin(OBLIQUITY) * sin(l))
        val hourAngle = RAD * (280.16 + 360.9856235 * d) + RAD * lon - ra
        val phi = RAD * lat
        var h = asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(hourAngle))
        // Refrakcja atmosferyczna
        val hr = if (h < 0) 0.0 else h
        h += 0.0002967 / tan(hr + 0.00312536 / (hr + 0.08901179))
        return h
    }

    /**
     * Wschód i zachód Księżyca w dniu [date] czasu lokalnego miejsca (przesunięcie [utcOffsetSec]);
     * `null`, gdy danego zdarzenia tego dnia nie ma (zdarza się raz w miesiącu).
     */
    fun riseSet(date: LocalDate, lat: Double, lon: Double, utcOffsetSec: Int): Pair<LocalDateTime?, LocalDateTime?> {
        val start = date.atStartOfDay()
        val t0 = start.toEpochSecond(ZoneOffset.ofTotalSeconds(utcOffsetSec)) * 1000.0
        val hc = 0.133 * RAD
        fun alt(hours: Double) = moonAltitude(t0 + hours * 3_600_000.0, lat, lon) - hc
        var rise: Double? = null
        var set: Double? = null
        var h0 = alt(0.0)
        var i = 1
        while (i <= 24) {
            val h1 = alt(i.toDouble())
            val h2 = alt(i + 1.0)
            val a = (h0 + h2) / 2 - h1
            val b = (h2 - h0) / 2
            val xe = -b / (2 * a)
            val ye = (a * xe + b) * xe + h1
            val disc = b * b - 4 * a * h1
            var roots = 0
            var x1 = 0.0
            var x2 = 0.0
            if (disc >= 0) {
                val dx = sqrt(disc) / (abs(a) * 2)
                x1 = xe - dx
                x2 = xe + dx
                if (abs(x1) <= 1) roots++
                if (abs(x2) <= 1) roots++
                if (x1 < -1) x1 = x2
            }
            if (roots == 1) {
                if (h0 < 0) rise = i + x1 else set = i + x1
            } else if (roots == 2) {
                rise = i + if (ye < 0) x2 else x1
                set = i + if (ye < 0) x1 else x2
            }
            if (rise != null && set != null) break
            h0 = h2
            i += 2
        }
        fun at(h: Double?) = h?.takeIf { it in 0.0..24.0 }?.let { start.plusSeconds((it * 3600).toLong()) }
        return at(rise) to at(set)
    }
}
