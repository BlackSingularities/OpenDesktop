package com.opendesktop.launcher.domain

import java.time.LocalDate
import kotlin.math.PI
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
}
