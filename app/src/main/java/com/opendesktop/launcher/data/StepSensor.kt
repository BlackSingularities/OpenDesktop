package com.opendesktop.launcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.time.LocalDate

/**
 * Kroki liczone przez sam telefon (czujnik `TYPE_STEP_COUNTER`), bez Health Connect i bez żadnej
 * aplikacji zdrowotnej. Działa też tam, gdzie producent nie udostępnia danych systemowi –
 * na przykład w telefonach Huawei, gdzie „Zdrowie Huawei” nie zapisuje kroków do Health Connect.
 *
 * Czujnik podaje liczbę kroków od ostatniego uruchomienia telefonu, więc dzisiejszy wynik liczymy
 * od zapamiętanego punktu odniesienia, a skończone dni trzymamy w pamięci podręcznej na tydzień.
 */
object StepSensor {

    const val PERMISSION = "android.permission.ACTIVITY_RECOGNITION"

    private const val PREFS = "steps_sensor"
    private const val KEY_DAY = "day"
    private const val KEY_BASE = "base"
    private const val KEY_CARRY = "carry"
    private const val KEY_TODAY = "today"
    private const val KEY_HISTORY = "history"

    /** Przelicznik długości kroku i spalania – wyniki poglądowe, bez wzrostu i wagi. */
    private const val METERS_PER_STEP = 0.72
    private const val KCAL_PER_STEP = 0.04

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun sensorManager(ctx: Context) = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun sensor(ctx: Context): Sensor? = sensorManager(ctx)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun available(ctx: Context): Boolean = sensor(ctx) != null

    /** Zgoda na rozpoznawanie aktywności; na Androidzie 9 i starszym nie jest potrzebna. */
    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(ctx, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * Zapisuje świeży odczyt czujnika ([counter] – kroki od uruchomienia telefonu) i zwraca
     * dzisiejszy wynik. Zmiana dnia zamyka poprzedni w historii, a restart telefonu
     * (licznik mniejszy niż punkt odniesienia) przenosi dotychczasowe kroki do „carry”.
     */
    fun record(ctx: Context, counter: Long): Health.Summary {
        val p = prefs(ctx)
        val today = LocalDate.now()
        val day = p.getString(KEY_DAY, null)
        var base = p.getLong(KEY_BASE, counter)
        var carry = p.getLong(KEY_CARRY, 0L)
        var history = parse(p.getString(KEY_HISTORY, "") ?: "")
        when {
            day != today.toString() -> {
                day?.let { history = history + (it to p.getLong(KEY_TODAY, 0L)) }
                base = counter
                carry = 0L
            }
            // Telefon był wyłączany: licznik czujnika zaczął od zera
            counter < base -> {
                carry = p.getLong(KEY_TODAY, 0L)
                base = counter
            }
        }
        val steps = (carry + counter - base).coerceAtLeast(0L)
        history = history.filterKeys { runCatching { LocalDate.parse(it) }.getOrNull()?.isAfter(today.minusDays(7)) == true }
        p.edit()
            .putString(KEY_DAY, today.toString())
            .putLong(KEY_BASE, base)
            .putLong(KEY_CARRY, carry)
            .putLong(KEY_TODAY, steps)
            .putString(KEY_HISTORY, serialize(history))
            .apply()
        return summary(today, steps, history)
    }

    /** Ostatni zapisany wynik – zanim czujnik zdąży zgłosić pierwszy odczyt. */
    fun last(ctx: Context): Health.Summary {
        val p = prefs(ctx)
        val today = LocalDate.now()
        val steps = if (p.getString(KEY_DAY, null) == today.toString()) p.getLong(KEY_TODAY, 0L) else 0L
        return summary(today, steps, parse(p.getString(KEY_HISTORY, "") ?: ""))
    }

    private fun summary(today: LocalDate, steps: Long, history: Map<String, Long>): Health.Summary {
        val first = today.minusDays(6)
        val week = (0L..6L).map { i ->
            val date = first.plusDays(i)
            day(date, if (date == today) steps else history[date.toString()] ?: 0L)
        }
        return Health.Summary(week.last(), week, hasCalories = true, hasDistance = true, estimated = true)
    }

    private fun day(date: LocalDate, steps: Long) = Health.Day(
        date = date,
        steps = steps,
        activeKcal = steps * KCAL_PER_STEP,
        totalKcal = steps * KCAL_PER_STEP,
        meters = steps * METERS_PER_STEP,
    )

    private fun parse(raw: String): Map<String, Long> = raw.split(';')
        .mapNotNull { entry ->
            val (date, value) = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            value.toLongOrNull()?.let { date to it }
        }
        .toMap()

    private fun serialize(history: Map<String, Long>) = history.entries.joinToString(";") { "${it.key}=${it.value}" }
}
