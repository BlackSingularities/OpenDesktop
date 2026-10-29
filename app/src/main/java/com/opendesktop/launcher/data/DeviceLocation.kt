package com.opendesktop.launcher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Przybliżone położenie telefonu dla widżetów pogody („Moja lokalizacja”). Wystarcza zgrubna
 * lokalizacja z sieci; nazwa miejscowości z systemowego geokodera, a gdy go brak – współrzędne.
 * Wynik trzymany 15 minut, żeby kilka widżetów nie pytało systemu osobno.
 */
object DeviceLocation {
    private const val TTL = 15 * 60_000L
    private var cached: Pair<Long, WeatherPlace>? = null

    fun granted(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun clear() {
        cached = null
    }

    suspend fun current(ctx: Context): WeatherPlace {
        cached?.let { (t, p) -> if (System.currentTimeMillis() - t < TTL) return p }
        if (!granted(ctx)) throw WeatherProblem("Brak dostępu do lokalizacji")
        val lm = ctx.getSystemService(LocationManager::class.java) ?: throw WeatherProblem("Lokalizacja niedostępna")
        val loc = fresh(ctx, lm) ?: lastKnown(lm) ?: throw WeatherProblem("Nie znam położenia – włącz lokalizację")
        val place = withContext(Dispatchers.IO) { describe(ctx, loc) }
        cached = System.currentTimeMillis() to place
        return place
    }

    @Suppress("MissingPermission")
    private fun lastKnown(lm: LocationManager): Location? = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }

    /** Świeży odczyt z sieci (Android 11+), najwyżej 8 sekund. */
    @Suppress("MissingPermission")
    private suspend fun fresh(ctx: Context, lm: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        return withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(ctx)) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun describe(ctx: Context, loc: Location): WeatherPlace {
        val fallback = "%.2f, %.2f".format(Locale.ROOT, loc.latitude, loc.longitude)
        val a = if (Geocoder.isPresent()) runCatching { Geocoder(ctx, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull() }.getOrNull() else null
        val name = a?.locality ?: a?.subAdminArea ?: a?.adminArea ?: fallback
        return WeatherPlace(name, listOfNotNull(a?.adminArea, a?.countryName).joinToString(", "), loc.latitude, loc.longitude)
    }
}
