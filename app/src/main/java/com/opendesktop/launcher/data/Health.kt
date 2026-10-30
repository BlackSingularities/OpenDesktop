package com.opendesktop.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.Period

/**
 * Kroki, kalorie i dystans z Health Connect (tam zapisują je Google Fit, Samsung Health, opaski…).
 * Tylko odczyt; kroki są wymagane, reszta pokazuje się, gdy użytkownik na nią pozwolił.
 */
object Health {
    private const val PROVIDER = "com.google.android.apps.healthdata"

    private val steps = HealthPermission.getReadPermission(StepsRecord::class)
    private val active = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val total = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val distance = HealthPermission.getReadPermission(DistanceRecord::class)

    val permissions = setOf(steps, active, total, distance)

    data class Day(val date: LocalDate, val steps: Long, val activeKcal: Double, val totalKcal: Double, val meters: Double)

    /** Dzisiejszy dzień i 7 ostatnich (z dzisiejszym na końcu). */
    data class Summary(
        val today: Day,
        val week: List<Day>,
        val hasCalories: Boolean,
        val hasDistance: Boolean,
        /** Kalorie i dystans policzone z kroków (czujnik telefonu), nie zmierzone. */
        val estimated: Boolean = false,
    )

    enum class Status { Ready, NeedsUpdate, Missing }

    fun status(ctx: Context): Status = when (HealthConnectClient.getSdkStatus(ctx, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE -> Status.Ready
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Status.NeedsUpdate
        else -> Status.Missing
    }

    /** Sklep z Health Connect (instalacja albo aktualizacja). */
    fun storeIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun granted(ctx: Context): Set<String> =
        HealthConnectClient.getOrCreate(ctx).permissionController.getGrantedPermissions()

    fun canRead(granted: Set<String>) = steps in granted

    suspend fun read(ctx: Context): Summary {
        val client = HealthConnectClient.getOrCreate(ctx)
        val granted = granted(ctx)
        val metrics = buildSet<AggregateMetric<*>> {
            add(StepsRecord.COUNT_TOTAL)
            if (active in granted) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (total in granted) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (distance in granted) add(DistanceRecord.DISTANCE_TOTAL)
        }
        val today = LocalDate.now()
        val first = today.minusDays(6)
        val groups = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(first.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            )
        )
        val week = (0L..6L).map { i ->
            val date = first.plusDays(i)
            val r = groups.firstOrNull { it.startTime.toLocalDate() == date }?.result
            Day(
                date = date,
                steps = r?.get(StepsRecord.COUNT_TOTAL) ?: 0L,
                activeKcal = r?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories ?: 0.0,
                totalKcal = r?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories ?: 0.0,
                meters = r?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters ?: 0.0,
            )
        }
        return Summary(week.last(), week, active in granted || total in granted, distance in granted)
    }
}
