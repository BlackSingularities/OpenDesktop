package com.opendesktop.launcher.ui.desktop

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.opendesktop.launcher.data.DeviceLocation
import com.opendesktop.launcher.data.WeatherProblem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.GadgetStyle
import com.opendesktop.launcher.data.WeatherDay
import com.opendesktop.launcher.data.WeatherHour
import com.opendesktop.launcher.data.WeatherMetric
import com.opendesktop.launcher.data.WeatherNow
import com.opendesktop.launcher.data.WeatherRepo
import com.opendesktop.launcher.data.WeatherExtras
import com.opendesktop.launcher.data.WeatherUnits
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import java.time.format.TextStyle as JTextStyle
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthEast
import com.opendesktop.launcher.domain.Moon

// ---------- Dane i pomocnicze ----------

/** Ręczne odświeżanie: zmiana [tick] wymusza nowe zapytanie we wszystkich widżetach pogody. */
internal object WeatherRefresh {
    var tick by mutableIntStateOf(0)
        private set
    /** Liczba trwających zapytań – kręci ikoną odświeżania. */
    var busy by mutableIntStateOf(0)

    private var lastClear = 0L

    /** Seria stuknięć nie zasypuje Open-Meteo: dane z ostatnich 30 s zostają w pamięci. */
    fun request() {
        val now = System.currentTimeMillis()
        if (now - lastClear >= 30_000) {
            WeatherRepo.clear()
            WeatherExtras.clear()
            DeviceLocation.clear()
            lastClear = now
        }
        tick++
    }
}

internal suspend fun loadWeather(ctx: Context, st: GadgetStyle): WeatherNow =
    if (st.useDeviceLocation) DeviceLocation.current(ctx).let { WeatherRepo.get(it.name, it.coords) } else WeatherRepo.get(st.city, st.coords)

/**
 * Stan pogody: null = ładowanie, failure = błąd sieci, nieznane miasto albo brak lokalizacji.
 * Odświeżane co 20 minut i po stuknięciu przycisku; nieudane odświeżenie nie zasłania starych danych.
 */
@Composable
internal fun rememberWeather(st: GadgetStyle): Result<WeatherNow>? {
    val ctx = LocalContext.current.applicationContext
    val tick = WeatherRefresh.tick
    var state by remember(st.city, st.coords, st.useDeviceLocation) { mutableStateOf<Result<WeatherNow>?>(null) }
    LaunchedEffect(st.city, st.coords, st.useDeviceLocation, tick) {
        while (true) {
            WeatherRefresh.busy++
            val r = try {
                runCatching { loadWeather(ctx, st) }
            } finally {
                WeatherRefresh.busy--
            }
            if (r.isSuccess || state?.isSuccess != true) state = r
            delay(if (r.isSuccess) 20 * 60_000L else 2 * 60_000L)
        }
    }
    return state
}

/** Mały okrągły przycisk odświeżania w rogu widżetu; kręci się, gdy trwa pobieranie. */
@Composable
internal fun RefreshButton(p: GadgetPaint, modifier: Modifier = Modifier) {
    val spinning = WeatherRefresh.busy > 0
    val angle = if (spinning) {
        rememberInfiniteTransition(label = "refresh").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle").value
    } else 0f
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(p.fg.copy(alpha = 0.12f))
            .clickable { WeatherRefresh.request() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Refresh, "Odśwież pogodę", tint = p.fg.copy(alpha = 0.85f), modifier = Modifier.size(15.dp).rotate(angle))
    }
}

/** Bieżący czas w miejscu prognozy (strefa miasta, nie telefonu). */
internal fun placeNow(w: WeatherNow): LocalDateTime =
    (w.localTime ?: LocalDateTime.now()).plusSeconds((System.currentTimeMillis() - w.fetchedAt) / 1000)

fun weatherIcon(code: Int, isDay: Boolean = true): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2 -> Icons.Rounded.WbCloudy
    3 -> Icons.Rounded.Cloud
    45, 48 -> Icons.Rounded.Dehaze
    in 51..57 -> Icons.Rounded.Grain
    in 61..67, in 80..82 -> Icons.Rounded.Umbrella
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}

fun weatherEmoji(code: Int, isDay: Boolean = true): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1 -> if (isDay) "🌤️" else "🌙"
    2 -> if (isDay) "⛅" else "☁️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..57 -> "🌦️"
    in 61..67 -> "🌧️"
    in 80..82 -> if (isDay) "🌦️" else "🌧️"
    in 71..77, 85, 86 -> "🌨️"
    in 95..99 -> "⛈️"
    else -> "☁️"
}

@Composable
internal fun ShadowedGlyph(icon: ImageVector, tint: Color, size: Dp, shadow: Boolean) {
    Box {
        if (shadow) Icon(icon, null, tint = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(size).padding(top = 1.5.dp))
        Icon(icon, null, tint = tint, modifier = Modifier.size(size))
    }
}

/** Ikona warunków w stylu wybranym przez użytkownika (akcent, kolorowe, kolor tekstu, emoji). */
@Composable
internal fun WxIcon(p: GadgetPaint, code: Int, isDay: Boolean, size: Dp) {
    val s = size * p.st.iconScale.coerceIn(0.5f, 2f)
    if (p.st.iconStyle == 3) {
        val fs = with(LocalDensity.current) { (s * 0.8f).toSp() }
        Box(Modifier.size(s), contentAlignment = Alignment.Center) {
            Text(weatherEmoji(code, isDay), fontSize = fs, lineHeight = fs, maxLines = 1)
        }
    } else ShadowedGlyph(weatherIcon(code, isDay), p.iconTint(code, isDay), s, p.st.textShadow)
}

@Composable
internal fun WeatherPlaceholder(p: GadgetPaint, state: Result<WeatherNow>?, size: Float) {
    Column(horizontalAlignment = p.hAlign) {
        if (state == null) T("Pobieranie pogody…", p.text(size, FontWeight.Normal, 0.8f))
        else {
            ShadowedGlyph(Icons.Rounded.CloudOff, p.fg.copy(alpha = 0.7f), (size * 1.8f).dp, p.st.textShadow)
            val e = state.exceptionOrNull()
            T(if (e is WeatherProblem) e.message.orEmpty() else "Brak połączenia", p.text(size, FontWeight.Normal, 0.8f), maxLines = 2)
        }
    }
}

/** Pogoda albo zastępczy napis; [content] dostaje gotowe dane. */
@Composable
internal fun WithWeather(p: GadgetPaint, content: @Composable (WeatherNow, LocalDateTime) -> Unit) {
    val state = rememberWeather(p.st)
    // Zmieniający się co minutę argument wymusza przeliczenie „teraz” (godziny, łuk słońca, zmiana dnia)
    val tick = rememberNow(false)
    val w = state?.getOrNull()
    if (w == null) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
            WeatherPlaceholder(p, state, (minOf(maxHeight.value, maxWidth.value) * 0.12f).coerceIn(10f, 16f))
        }
    } else content(w, tick)
}

internal fun clock(t: LocalDateTime?, st: GadgetStyle): String =
    t?.format(DateTimeFormatter.ofPattern(if (st.use24h) "HH:mm" else "h:mm a", if (st.use24h) Locale.getDefault() else Locale.ENGLISH)) ?: "—"

internal fun hourLabel(h: WeatherHour, first: Boolean, st: GadgetStyle): String = when {
    first -> "Teraz"
    st.use24h -> h.time.format(DateTimeFormatter.ofPattern("HH"))
    else -> h.time.format(DateTimeFormatter.ofPattern("h a", Locale.ENGLISH))
}

internal fun dayLabel(offset: Int, d: LocalDate, st: GadgetStyle, locale: Locale): String {
    if (st.dayNames != 2 && offset == 0) return "Dziś"
    if (st.dayNames == 1 && offset == 1) return "Jutro"
    val short = cap(d.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale), locale)
    return when (st.dayNames) {
        1 -> cap(d.dayOfWeek.getDisplayName(JTextStyle.FULL, locale), locale)
        2 -> d.format(DateTimeFormatter.ofPattern("d.MM"))
        3 -> "$short ${d.dayOfMonth}"
        else -> short
    }
}

internal fun lengthText(d: Duration): String = if (d.toHours() == 0L) "${d.toMinutes()} min" else "${d.toHours()} h ${d.toMinutes() % 60} min"

/** Dni do pokazania: od dziś (albo jutra) w strefie miejsca, z przesunięciem względem dziś. */
internal fun shownDays(w: WeatherNow, st: GadgetStyle): List<Pair<Int, WeatherDay>> {
    val today = placeNow(w).toLocalDate()
    return w.days.filter { !it.date.isBefore(today) }
        .drop(if (st.startTomorrow) 1 else 0)
        .take(st.days.coerceIn(1, 16))
        .map { ChronoUnit.DAYS.between(today, it.date).toInt() to it }
}

/** Godziny od bieżącej, co [GadgetStyle.hourStep]. */
internal fun shownHours(w: WeatherNow, st: GadgetStyle): List<WeatherHour> {
    val from = placeNow(w).withMinute(0).withSecond(0).withNano(0)
    val step = st.hourStep.coerceIn(1, 12)
    return w.hours.filter { !it.time.isBefore(from) }.take(st.hours.coerceIn(3, WeatherRepo.MAX_HOURS)).filterIndexed { i, _ -> i % step == 0 }
}

internal class Metric(val icon: ImageVector, val label: String, val value: String, val rotation: Float? = null)

/** Wartości „teraz” w kolejności [WeatherMetric.all], tylko wybrane przez użytkownika. */
internal fun currentMetrics(p: GadgetPaint, w: WeatherNow, skip: Set<String> = emptySet()): List<Metric> {
    val today = w.days.firstOrNull { it.date == placeNow(w).toLocalDate() } ?: w.today
    return WeatherMetric.all.filter { p.has(it) && it !in skip }.mapNotNull { m ->
        when (m) {
            WeatherMetric.Feels -> Metric(Icons.Rounded.Thermostat, "Odczuwalna", p.deg(w.feels))
            WeatherMetric.Wind -> Metric(Icons.Rounded.Navigation, "Wiatr", "${p.wind(w.wind)} ${WeatherUnits.compass(w.windDir)}", w.windDir + 180f)
            WeatherMetric.Humidity -> Metric(Icons.Rounded.Opacity, "Wilgotność", "${w.humidity}%")
            WeatherMetric.Pressure -> Metric(Icons.Rounded.Speed, "Ciśnienie", p.pressure(w.pressure))
            WeatherMetric.Uv -> today?.let { Metric(Icons.Rounded.WbSunny, "Indeks UV", "${it.uv.roundToInt()} · ${WeatherUnits.uvLabel(it.uv)}") }
            WeatherMetric.Precip -> today?.let { Metric(Icons.Rounded.Umbrella, "Szansa opadów", "${it.precipProb}%") }
            WeatherMetric.Rain -> today?.let { Metric(Icons.Rounded.WaterDrop, "Opady dziś", p.rain(it.precipSum)) }
            WeatherMetric.Cloud -> Metric(Icons.Rounded.Cloud, "Zachmurzenie", "${w.cloud}%")
            WeatherMetric.Sun -> today?.let { Metric(Icons.Rounded.WbTwilight, "Słońce", "${clock(it.sunrise, p.st)} – ${clock(it.sunset, p.st)}") }
            else -> null
        }
    }
}

/** Kolor procentu opadów: niebieski przy kolorowych ikonach, inaczej przygaszony tekst. */
internal fun precipColor(p: GadgetPaint): Color = if (p.st.iconStyle == 1 || p.st.tempColors) Color(0xFF6EC6FF) else p.fg.copy(alpha = 0.75f)

@Composable
internal fun MetricIcon(m: Metric, tint: Color, size: Dp) {
    Icon(m.icon, null, tint = tint, modifier = Modifier.size(size).then(if (m.rotation != null) Modifier.rotate(m.rotation) else Modifier))
}

/** Rząd małych wartości (ikona + tekst) – pod bieżącą pogodą. */
@Composable
internal fun MetricStrip(p: GadgetPaint, metrics: List<Metric>, fs: Float, modifier: Modifier = Modifier) {
    if (metrics.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((fs * 0.9f).dp), verticalAlignment = Alignment.CenterVertically) {
        metrics.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetricIcon(m, p.accent.copy(alpha = 0.9f), (fs * 1.15f * p.st.textScale).dp)
                Spacer(Modifier.width(3.dp))
                T(m.value, p.text(fs, FontWeight.Normal, 0.85f))
            }
        }
    }
}

@Composable
internal fun Separator(p: GadgetPaint, vertical: Boolean = false) {
    if (!p.st.separators) return
    if (vertical) Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(p.faint))
    else Box(Modifier.fillMaxWidth().height(1.dp).background(p.faint))
}

@Composable
internal fun UpdatedLabel(p: GadgetPaint, w: WeatherNow, fs: Float, modifier: Modifier = Modifier) {
    if (!p.st.showUpdated) return
    val t = java.time.Instant.ofEpochMilli(w.fetchedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    T("Akt. ${clock(t, p.st)}", p.text(fs, FontWeight.Normal, 0.5f), modifier)
}

/** Nagłówek „Miasto · 12° · Pochmurno” z godziną aktualizacji po prawej. */
@Composable
internal fun HeaderLine(p: GadgetPaint, w: WeatherNow, fs: Float) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T(
            "${w.place} · ${p.deg(w.temp)} · ${WeatherRepo.describe(w.code)}",
            p.text(fs, FontWeight.SemiBold, 0.9f).copy(textAlign = if (p.st.showUpdated) TextAlign.Start else p.textAlign),
            Modifier.weight(1f),
        )
        UpdatedLabel(p, w, fs * 0.8f)
        if (p.st.showRefresh) Spacer(Modifier.width(26.dp))
    }
}

/** Wartość dodatkowa dla jednej godziny: tekst, kolor i opcjonalny obrót strzałki wiatru. */
internal class HourValue(val text: String, val color: Color, val arrow: Float? = null)

/** Wartości godzinowe w stałej kolejności – tylko wybrane przez użytkownika. */
internal val hourMetrics = listOf(
    WeatherMetric.Feels, WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind,
    WeatherMetric.Humidity, WeatherMetric.Uv, WeatherMetric.Cloud, WeatherMetric.Pressure,
)

internal fun hourIcon(m: String): ImageVector = when (m) {
    WeatherMetric.Feels -> Icons.Rounded.Thermostat
    WeatherMetric.Precip -> Icons.Rounded.Umbrella
    WeatherMetric.Rain -> Icons.Rounded.WaterDrop
    WeatherMetric.Wind -> Icons.Rounded.Air
    WeatherMetric.Humidity -> Icons.Rounded.Opacity
    WeatherMetric.Uv -> Icons.Rounded.WbSunny
    WeatherMetric.Cloud -> Icons.Rounded.Cloud
    else -> Icons.Rounded.Speed
}

internal fun hourValue(p: GadgetPaint, h: WeatherHour, m: String): HourValue {
    val dim = p.fg.copy(alpha = 0.72f)
    return when (m) {
        WeatherMetric.Feels -> HourValue(p.deg(h.feels), if (p.st.tempColors) temperatureColor(h.feels) else dim)
        WeatherMetric.Precip -> HourValue(if (h.precipProb > 0) "${h.precipProb}%" else "–", precipColor(p))
        WeatherMetric.Rain -> HourValue(if (h.precip >= 0.05) "%.1f".format(if (p.st.fahrenheit) h.precip / 25.4 else h.precip) else "–", precipColor(p))
        WeatherMetric.Wind -> HourValue(
            "${WeatherUnits.wind(h.wind, p.st.windUnit).roundToInt()}" + if (h.gusts > h.wind + 15) "↑${WeatherUnits.wind(h.gusts, p.st.windUnit).roundToInt()}" else "",
            dim, h.windDir + 180f,
        )
        WeatherMetric.Humidity -> HourValue("${h.humidity}%", dim)
        WeatherMetric.Uv -> HourValue(if (h.isDay) "${h.uv.roundToInt()}" else "–", if (h.uv >= 6) Color(0xFFFFB347) else dim)
        WeatherMetric.Cloud -> HourValue("${h.cloud}%", dim)
        else -> HourValue(WeatherUnits.pressure(h.pressure, p.st.pressureUnit).let { if (p.st.pressureUnit == 2) "%.2f".format(it) else "${it.roundToInt()}" }, dim)
    }
}

/** Jedna wartość godzinowa: opcjonalna strzałka wiatru i tekst. */
@Composable
internal fun HourCell(p: GadgetPaint, v: HourValue, fs: Float, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (v.arrow != null) {
            Icon(Icons.Rounded.Navigation, null, tint = v.color, modifier = Modifier.size((fs * 0.95f * p.st.textScale).dp).rotate(v.arrow))
            Spacer(Modifier.width(1.dp))
        }
        T(v.text, p.text(fs, FontWeight.Normal, color = v.color).copy(textAlign = TextAlign.Center))
    }
}

/** Nazwa dnia nad pierwszą godziną nowej doby (na wykresie zamiast „00”). */
internal fun isMidnight(h: WeatherHour, i: Int) = i > 0 && h.time.hour == 0

/**
 * Krótkie podsumowanie najbliższych godzin: kiedy zacznie (albo przestanie) padać oraz
 * najcieplejsza i najzimniejsza godzina.
 */
internal fun hourlySummary(p: GadgetPaint, hours: List<WeatherHour>): String {
    if (hours.isEmpty()) return ""
    fun wet(h: WeatherHour) = h.precipProb >= 50 || h.precip >= 0.2
    fun at(h: WeatherHour) = clock(h.time, p.st)
    val rain = when {
        wet(hours[0]) -> hours.indexOfFirst { !wet(it) }.let { if (it < 0) "Opady przez najbliższe ${hours.size} h" else "Opady do ok. ${at(hours[it])}" }
        else -> hours.firstOrNull(::wet)?.let { "Opady od ok. ${at(it)} (${it.precipProb}%)" } ?: "Bez opadów przez ${hours.size} h"
    }
    val hot = hours.maxBy { it.temp }
    val cold = hours.minBy { it.temp }
    return "$rain · max ${p.deg(hot.temp)} o ${at(hot)} · min ${p.deg(cold.temp)} o ${at(cold)}"
}
