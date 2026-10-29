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

// ---------- Dane i pomocnicze ----------

/** Ręczne odświeżanie: zmiana [tick] wymusza nowe zapytanie we wszystkich widżetach pogody. */
internal object WeatherRefresh {
    var tick by mutableIntStateOf(0)
        private set
    /** Liczba trwających zapytań – kręci ikoną odświeżania. */
    var busy by mutableIntStateOf(0)

    fun request() {
        WeatherRepo.clear()
        DeviceLocation.clear()
        tick++
    }
}

private suspend fun loadWeather(ctx: Context, st: GadgetStyle): WeatherNow =
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
private fun WeatherPlaceholder(p: GadgetPaint, state: Result<WeatherNow>?, size: Float) {
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
private fun WithWeather(p: GadgetPaint, content: @Composable (WeatherNow, LocalDateTime) -> Unit) {
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

private fun clock(t: LocalDateTime?, st: GadgetStyle): String =
    t?.format(DateTimeFormatter.ofPattern(if (st.use24h) "HH:mm" else "h:mm a", if (st.use24h) Locale.getDefault() else Locale.ENGLISH)) ?: "—"

private fun hourLabel(h: WeatherHour, first: Boolean, st: GadgetStyle): String = when {
    first -> "Teraz"
    st.use24h -> h.time.format(DateTimeFormatter.ofPattern("HH"))
    else -> h.time.format(DateTimeFormatter.ofPattern("h a", Locale.ENGLISH))
}

private fun dayLabel(offset: Int, d: LocalDate, st: GadgetStyle, locale: Locale): String {
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

private fun lengthText(d: Duration): String = if (d.toHours() == 0L) "${d.toMinutes()} min" else "${d.toHours()} h ${d.toMinutes() % 60} min"

/** Dni do pokazania: od dziś (albo jutra) w strefie miejsca, z przesunięciem względem dziś. */
private fun shownDays(w: WeatherNow, st: GadgetStyle): List<Pair<Int, WeatherDay>> {
    val today = placeNow(w).toLocalDate()
    return w.days.filter { !it.date.isBefore(today) }
        .drop(if (st.startTomorrow) 1 else 0)
        .take(st.days.coerceIn(1, 16))
        .map { ChronoUnit.DAYS.between(today, it.date).toInt() to it }
}

/** Godziny od bieżącej, co [GadgetStyle.hourStep]. */
private fun shownHours(w: WeatherNow, st: GadgetStyle): List<WeatherHour> {
    val from = placeNow(w).withMinute(0).withSecond(0).withNano(0)
    val step = st.hourStep.coerceIn(1, 6)
    return w.hours.filter { !it.time.isBefore(from) }.take(st.hours.coerceIn(3, 48)).filterIndexed { i, _ -> i % step == 0 }
}

private class Metric(val icon: ImageVector, val label: String, val value: String, val rotation: Float? = null)

/** Wartości „teraz” w kolejności [WeatherMetric.all], tylko wybrane przez użytkownika. */
private fun currentMetrics(p: GadgetPaint, w: WeatherNow, skip: Set<String> = emptySet()): List<Metric> {
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
private fun precipColor(p: GadgetPaint): Color = if (p.st.iconStyle == 1 || p.st.tempColors) Color(0xFF6EC6FF) else p.fg.copy(alpha = 0.75f)

@Composable
private fun MetricIcon(m: Metric, tint: Color, size: Dp) {
    Icon(m.icon, null, tint = tint, modifier = Modifier.size(size).then(if (m.rotation != null) Modifier.rotate(m.rotation) else Modifier))
}

/** Rząd małych wartości (ikona + tekst) – pod bieżącą pogodą. */
@Composable
private fun MetricStrip(p: GadgetPaint, metrics: List<Metric>, fs: Float, modifier: Modifier = Modifier) {
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
private fun Separator(p: GadgetPaint, vertical: Boolean = false) {
    if (!p.st.separators) return
    if (vertical) Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(p.faint))
    else Box(Modifier.fillMaxWidth().height(1.dp).background(p.faint))
}

@Composable
private fun UpdatedLabel(p: GadgetPaint, w: WeatherNow, fs: Float, modifier: Modifier = Modifier) {
    if (!p.st.showUpdated) return
    val t = java.time.Instant.ofEpochMilli(w.fetchedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    T("Akt. ${clock(t, p.st)}", p.text(fs, FontWeight.Normal, 0.5f), modifier)
}

/** Nagłówek „Miasto · 12° · Pochmurno” z godziną aktualizacji po prawej. */
@Composable
private fun HeaderLine(p: GadgetPaint, w: WeatherNow, fs: Float) {
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
private class HourValue(val text: String, val color: Color, val arrow: Float? = null)

/** Wartości godzinowe w stałej kolejności – tylko wybrane przez użytkownika. */
private val hourMetrics = listOf(
    WeatherMetric.Feels, WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind,
    WeatherMetric.Humidity, WeatherMetric.Uv, WeatherMetric.Cloud, WeatherMetric.Pressure,
)

private fun hourIcon(m: String): ImageVector = when (m) {
    WeatherMetric.Feels -> Icons.Rounded.Thermostat
    WeatherMetric.Precip -> Icons.Rounded.Umbrella
    WeatherMetric.Rain -> Icons.Rounded.WaterDrop
    WeatherMetric.Wind -> Icons.Rounded.Air
    WeatherMetric.Humidity -> Icons.Rounded.Opacity
    WeatherMetric.Uv -> Icons.Rounded.WbSunny
    WeatherMetric.Cloud -> Icons.Rounded.Cloud
    else -> Icons.Rounded.Speed
}

private fun hourValue(p: GadgetPaint, h: WeatherHour, m: String): HourValue {
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
private fun HourCell(p: GadgetPaint, v: HourValue, fs: Float, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (v.arrow != null) {
            Icon(Icons.Rounded.Navigation, null, tint = v.color, modifier = Modifier.size((fs * 0.95f * p.st.textScale).dp).rotate(v.arrow))
            Spacer(Modifier.width(1.dp))
        }
        T(v.text, p.text(fs, FontWeight.Normal, color = v.color).copy(textAlign = TextAlign.Center))
    }
}

/** Nazwa dnia nad pierwszą godziną nowej doby (na wykresie zamiast „00”). */
private fun isMidnight(h: WeatherHour, i: Int) = i > 0 && h.time.hour == 0

/**
 * Krótkie podsumowanie najbliższych godzin: kiedy zacznie (albo przestanie) padać oraz
 * najcieplejsza i najzimniejsza godzina.
 */
private fun hourlySummary(p: GadgetPaint, hours: List<WeatherHour>): String {
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

// ---------- Wykresy ----------

/**
 * Krzywa temperatury z etykietami nad punktami (i opcjonalnie druga, przerywana, z etykietami pod spodem).
 * Punkty leżą na środkach [main].size równych kolumn – tak samo jak Row z weight(1f).
 */
@Composable
private fun TempCurve(
    p: GadgetPaint,
    main: List<Double>,
    second: List<Double>?,
    labelSize: Float,
    modifier: Modifier,
    every: Int = 1,
    highlightFirst: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    val style = p.text(labelSize, FontWeight.SemiBold).copy(textAlign = TextAlign.Center)
    val style2 = p.text(labelSize * 0.9f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Center)
    Canvas(modifier) {
        val n = main.size
        if (n == 0) return@Canvas
        val all = main + second.orEmpty()
        val lo = all.min()
        val hi = all.max()
        val span = (hi - lo).coerceAtLeast(1.0)
        val labelH = measurer.measure("0°", style).size.height.toFloat()
        val top = labelH * 1.1f
        val bottom = size.height - if (second != null) labelH * 1.1f else labelH * 0.25f
        if (bottom <= top) return@Canvas
        fun x(i: Int) = (i + 0.5f) * size.width / n
        fun y(v: Double) = (bottom - (v - lo) / span * (bottom - top)).toFloat()
        fun curve(vals: List<Double>) = Path().apply {
            moveTo(x(0), y(vals[0]))
            for (i in 1 until vals.size) {
                val mx = (x(i - 1) + x(i)) / 2f
                cubicTo(mx, y(vals[i - 1]), mx, y(vals[i]), x(i), y(vals[i]))
            }
        }
        val stroke = (2.4f * density).coerceAtLeast(2f)
        val lineBrush: Brush = if (p.st.tempColors && n > 1) {
            Brush.horizontalGradient(main.map { temperatureColor(it) }, startX = x(0), endX = x(n - 1))
        } else SolidColor(p.accent)
        val line = curve(main)
        if (p.st.chartFill && n > 1) {
            val fill = Path().apply {
                addPath(line)
                lineTo(x(n - 1), size.height)
                lineTo(x(0), size.height)
                close()
            }
            val tint = if (p.st.tempColors) temperatureColor(main.average()) else p.accent
            drawPath(fill, Brush.verticalGradient(listOf(tint.copy(alpha = 0.35f), tint.copy(alpha = 0f)), startY = top, endY = size.height))
        }
        if (second != null && second.size == n && n > 1) {
            drawPath(curve(second), p.fg.copy(alpha = 0.45f), style = Stroke(stroke * 0.7f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2.5f, stroke * 2f))))
        }
        if (n > 1) drawPath(line, lineBrush, style = Stroke(stroke, cap = StrokeCap.Round))
        fun label(text: String, cx: Float, cy: Float, st: androidx.compose.ui.text.TextStyle) {
            val m = measurer.measure(text, st)
            val lx = (cx - m.size.width / 2f).coerceIn(0f, (size.width - m.size.width).coerceAtLeast(0f))
            drawText(m, topLeft = Offset(lx, cy))
        }
        main.forEachIndexed { i, v ->
            if (i % every != 0) return@forEachIndexed
            val c = Offset(x(i), y(v))
            val dot = if (highlightFirst && i == 0) stroke * 2.2f else stroke * 1.5f
            drawCircle(if (p.st.tempColors) temperatureColor(v) else p.accent, dot, c)
            drawCircle(p.fg, dot * 0.45f, c)
            label(p.deg(v), c.x, c.y - labelH - stroke, style.copy(color = p.tempColor(v)))
        }
        second?.forEachIndexed { i, v ->
            if (i % every != 0 || second.size != n) return@forEachIndexed
            label(p.deg(v), x(i), y(v) + stroke * 1.5f, style2)
        }
    }
}

/** Pasek zakresu temperatur dnia na tle zakresu całej prognozy; kropka = temperatura teraz. */
@Composable
private fun RangeBar(p: GadgetPaint, lo: Double, hi: Double, gLo: Double, gHi: Double, current: Double?, modifier: Modifier) {
    Canvas(modifier) {
        val h = size.height
        val r = h / 2f
        drawRoundRect(p.faint, cornerRadius = CornerRadius(r))
        val span = (gHi - gLo).coerceAtLeast(1.0)
        val x0 = ((lo - gLo) / span * size.width).toFloat().coerceIn(0f, size.width)
        val x1 = ((hi - gLo) / span * size.width).toFloat().coerceIn(x0, size.width).coerceAtLeast((x0 + h).coerceAtMost(size.width))
        val brush: Brush = if (p.st.tempColors) {
            Brush.horizontalGradient(listOf(temperatureColor(lo), temperatureColor(hi)), startX = x0, endX = x1.coerceAtLeast(x0 + 1f))
        } else SolidColor(p.accent)
        drawRoundRect(brush, topLeft = Offset(x0, 0f), size = Size((x1 - x0).coerceAtLeast(h), h), cornerRadius = CornerRadius(r))
        current?.let {
            val cx = ((it - gLo) / span * size.width).toFloat().coerceIn(r, size.width - r)
            drawCircle(Color.Black.copy(alpha = 0.35f), r * 1.35f, Offset(cx, r))
            drawCircle(Color.White, r * 1.1f, Offset(cx, r))
        }
    }
}

// ---------- Dni ----------

@Composable
private fun DayColumns(p: GadgetPaint, w: WeatherNow, days: List<Pair<Int, WeatherDay>>, compact: Boolean, modifier: Modifier) {
    val st = p.st
    val locale = Locale.getDefault()
    val metrics = if (compact) emptyList() else listOf(WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind, WeatherMetric.Uv).filter { p.has(it) }
    BoxWithConstraints(modifier) {
        val lines = (if (compact) 2 else 3) + metrics.size + if (st.showCondition && !compact) 1 else 0
        val colW = maxWidth.value / days.size
        val small = minOf(maxHeight.value / (lines * 1.45f + 2.4f * st.iconScale), colW * 0.24f).coerceIn(7f, 18f)
        val icon = minOf(small * 2.2f * st.textScale, colW * 0.62f)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            days.forEach { (off, d) ->
                val today = off == 0 && st.highlightToday && days.size > 1
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .then(if (today) Modifier.clip(RoundedCornerShape(12.dp)).background(p.fg.copy(alpha = 0.08f)) else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    T(dayLabel(off, d.date, st, locale), p.text(small, if (off == 0) FontWeight.Bold else FontWeight.Medium, 0.9f).copy(textAlign = TextAlign.Center))
                    WxIcon(p, d.code, true, icon.dp)
                    if (st.showCondition && !compact) T(WeatherRepo.describeShort(d.code), p.text(small * 0.8f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Center))
                    if (compact) {
                        T("${p.deg(d.max)} ${p.deg(d.min)}", p.text(small, FontWeight.SemiBold).copy(textAlign = TextAlign.Center))
                    } else {
                        T(p.deg(d.max), p.text(small * 1.05f, FontWeight.SemiBold, color = p.tempColor(d.max)).copy(textAlign = TextAlign.Center))
                        T(p.deg(d.min), p.text(small * 0.9f, FontWeight.Normal, 0.6f, color = p.tempColor(d.min)).copy(textAlign = TextAlign.Center))
                    }
                    metrics.forEach { m ->
                        val (text, color) = when (m) {
                            WeatherMetric.Precip -> "${d.precipProb}%" to precipColor(p)
                            WeatherMetric.Rain -> p.rain(d.precipSum) to precipColor(p)
                            WeatherMetric.Wind -> "${WeatherUnits.wind(d.windMax, st.windUnit).roundToInt()}" to p.fg.copy(alpha = 0.7f)
                            else -> "UV ${d.uv.roundToInt()}" to p.fg.copy(alpha = 0.7f)
                        }
                        T(text, p.text(small * 0.8f, FontWeight.Normal, color = color).copy(textAlign = TextAlign.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayList(p: GadgetPaint, w: WeatherNow, days: List<Pair<Int, WeatherDay>>, modifier: Modifier) {
    val st = p.st
    val locale = Locale.getDefault()
    val gLo = days.minOf { it.second.min }
    val gHi = days.maxOf { it.second.max }
    BoxWithConstraints(modifier) {
        val rowH = maxHeight.value / days.size
        val fs = minOf(rowH * 0.42f, 17f).coerceAtLeast(7f)
        val sc = st.textScale
        val wide = maxWidth.value > 260f
        Column(Modifier.fillMaxSize()) {
            days.forEachIndexed { i, (off, d) ->
                Row(
                    Modifier.fillMaxWidth().weight(1f)
                        .then(if (off == 0 && st.highlightToday && days.size > 1) Modifier.clip(RoundedCornerShape(10.dp)).background(p.fg.copy(alpha = 0.07f)) else Modifier)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    T(dayLabel(off, d.date, st, locale), p.text(fs, if (off == 0) FontWeight.Bold else FontWeight.Medium).copy(textAlign = TextAlign.Start), Modifier.weight(if (st.dayNames == 1) 1.6f else 1f))
                    WxIcon(p, d.code, true, minOf(rowH * 0.7f, fs * 1.7f * sc).dp)
                    if (st.showCondition && wide) T(WeatherRepo.describeShort(d.code), p.text(fs * 0.8f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Start), Modifier.weight(1f).padding(start = 6.dp))
                    if (p.has(WeatherMetric.Precip)) {
                        T(
                            if (d.precipProb >= 10) "${d.precipProb}%" else "",
                            p.text(fs * 0.8f, FontWeight.SemiBold, color = precipColor(p)).copy(textAlign = TextAlign.End),
                            Modifier.width((fs * 2.6f * sc).dp),
                        )
                    }
                    if (p.has(WeatherMetric.Wind) && wide) {
                        T(p.wind(d.windMax), p.text(fs * 0.75f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.End), Modifier.width((fs * 4.6f * sc).dp))
                    }
                    T(p.deg(d.min), p.text(fs, FontWeight.Normal, 0.65f, color = p.tempColor(d.min)).copy(textAlign = TextAlign.End), Modifier.width((fs * 2.8f * sc).dp))
                    RangeBar(
                        p, d.min, d.max, gLo, gHi, if (off == 0) w.temp else null,
                        Modifier.weight(1.6f).height((fs * 0.42f).coerceAtLeast(3f).dp).padding(horizontal = 6.dp),
                    )
                    T(p.deg(d.max), p.text(fs, FontWeight.SemiBold, color = p.tempColor(d.max)).copy(textAlign = TextAlign.Start), Modifier.width((fs * 2.8f * sc).dp))
                }
                if (i < days.lastIndex && !(off == 0 && st.highlightToday)) Separator(p)
            }
        }
    }
}

@Composable
private fun DayChart(p: GadgetPaint, days: List<Pair<Int, WeatherDay>>, modifier: Modifier) {
    val st = p.st
    val locale = Locale.getDefault()
    BoxWithConstraints(modifier) {
        val colW = maxWidth.value / days.size
        val small = minOf(maxHeight.value * 0.075f, colW * 0.24f).coerceIn(7f, 16f)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                days.forEach { (off, d) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        T(dayLabel(off, d.date, st, locale), p.text(small, if (off == 0) FontWeight.Bold else FontWeight.Medium, 0.9f).copy(textAlign = TextAlign.Center))
                        WxIcon(p, d.code, true, minOf(small * 2f * st.textScale, colW * 0.6f).dp)
                    }
                }
            }
            TempCurve(p, days.map { it.second.max }, days.map { it.second.min }, small * 0.95f, Modifier.fillMaxWidth().weight(1f).padding(vertical = 2.dp))
            if (p.has(WeatherMetric.Precip)) {
                Row(Modifier.fillMaxWidth()) {
                    days.forEach { (_, d) ->
                        T("${d.precipProb}%", p.text(small * 0.8f, FontWeight.SemiBold, color = precipColor(p)).copy(textAlign = TextAlign.Center), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ---------- Godziny ----------

@Composable
private fun HourChart(p: GadgetPaint, hours: List<WeatherHour>, modifier: Modifier) {
    val st = p.st
    BoxWithConstraints(modifier) {
        val colW = maxWidth.value / hours.size
        val every = ceil(30f * st.textScale / colW).toInt().coerceAtLeast(1)
        val small = minOf(maxHeight.value * 0.085f, colW * every * 0.3f).coerceIn(7f, 15f)
        // Dodatkowe wiersze pod wykresem – tyle, ile się zmieści nad minimalną wysokością krzywej
        val rowH = small * 1.35f * st.textScale
        val fixed = small * (4.2f + if (p.has(WeatherMetric.Precip)) 2f else 0f) * st.textScale
        val extra = hourMetrics.filter { p.has(it) && it != WeatherMetric.Precip }
            .take(((maxHeight.value - fixed - small * 4.5f - rowH) / rowH).toInt().coerceAtLeast(0))
        val locale = Locale.getDefault()
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                hours.forEachIndexed { i, h ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (i % every == 0) {
                            val midnight = isMidnight(h, i)
                            T(
                                if (midnight) cap(h.time.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale), locale) else hourLabel(h, i == 0, st),
                                p.text(small * 0.9f, if (i == 0 || midnight) FontWeight.Bold else FontWeight.Medium, 0.85f, color = if (midnight) p.accent else p.fg).copy(textAlign = TextAlign.Center),
                                Modifier.requiredWidth((colW * every).dp),
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                hours.forEachIndexed { i, h ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (i % every == 0) {
                            Box(Modifier.requiredWidth((colW * every).dp), contentAlignment = Alignment.Center) {
                                WxIcon(p, h.code, h.isDay, minOf(small * 1.9f * st.textScale, colW * every * 0.6f).dp)
                            }
                        }
                    }
                }
            }
            TempCurve(p, hours.map { it.temp }, null, small * 0.95f, Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp), every, st.highlightToday)
            if (p.has(WeatherMetric.Precip)) {
                val col = precipColor(p)
                Canvas(Modifier.fillMaxWidth().height((small * 0.9f).dp)) {
                    val cw = size.width / hours.size
                    hours.forEachIndexed { i, h ->
                        val bh = size.height * h.precipProb / 100f
                        if (bh > 0f) drawRoundRect(col.copy(alpha = 0.7f), Offset(i * cw + cw * 0.2f, size.height - bh), Size(cw * 0.6f, bh), CornerRadius(cw * 0.15f))
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    hours.forEachIndexed { i, h ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (i % every == 0) T(if (h.precipProb >= 10) "${h.precipProb}%" else "", p.text(small * 0.75f, FontWeight.SemiBold, color = col).copy(textAlign = TextAlign.Center), Modifier.requiredWidth((colW * every).dp))
                        }
                    }
                }
            }
            extra.forEach { m ->
                Row(Modifier.fillMaxWidth().height(rowH.dp), verticalAlignment = Alignment.CenterVertically) {
                    hours.forEachIndexed { i, h ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (i % every == 0) HourCell(p, hourValue(p, h, m), small * 0.78f, Modifier.requiredWidth((colW * every).dp))
                        }
                    }
                }
            }
            if (extra.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 1.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    extra.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(hourIcon(m), null, tint = p.accent.copy(alpha = 0.8f), modifier = Modifier.size((small * 0.75f * st.textScale).dp))
                            Spacer(Modifier.width(2.dp))
                            T(metricName(m), p.text(small * 0.62f, FontWeight.Normal, 0.55f))
                        }
                    }
                }
            }
        }
    }
}

private fun metricName(m: String) = when (m) {
    WeatherMetric.Feels -> "odczuwalna"
    WeatherMetric.Precip -> "szansa opadów"
    WeatherMetric.Rain -> "opady"
    WeatherMetric.Wind -> "wiatr"
    WeatherMetric.Humidity -> "wilgotność"
    WeatherMetric.Uv -> "UV"
    WeatherMetric.Cloud -> "chmury"
    else -> "ciśnienie"
}

@Composable
private fun HourColumns(p: GadgetPaint, hours: List<WeatherHour>, modifier: Modifier) {
    val st = p.st
    BoxWithConstraints(modifier) {
        val shown = hours.take((maxWidth.value / 38f).toInt().coerceAtLeast(2))
        val colW = maxWidth.value / shown.size
        val locale = Locale.getDefault()
        // Wartości, które się nie zmieszczą w wysokości, odpadają od końca listy
        val metrics = hourMetrics.filter { p.has(it) }.take(((maxHeight.value / 13f) - 4.5f).toInt().coerceAtLeast(0))
        val lines = 3 + metrics.size
        val small = minOf(maxHeight.value / (lines * 1.45f + 2.2f), colW * 0.26f).coerceIn(7f, 16f)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            shown.forEachIndexed { i, h ->
                if (isMidnight(h, i) && st.separators) Box(Modifier.width(1.dp).fillMaxHeight(0.8f).background(p.faint))
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .then(if (i == 0 && st.highlightToday) Modifier.clip(RoundedCornerShape(12.dp)).background(p.fg.copy(alpha = 0.08f)) else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val midnight = isMidnight(h, i)
                    T(
                        if (midnight) cap(h.time.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale), locale) else hourLabel(h, i == 0, st),
                        p.text(small * 0.9f, if (i == 0 || midnight) FontWeight.Bold else FontWeight.Medium, 0.85f, color = if (midnight) p.accent else p.fg).copy(textAlign = TextAlign.Center),
                    )
                    WxIcon(p, h.code, h.isDay, minOf(small * 2f * st.textScale, colW * 0.62f).dp)
                    T(p.deg(h.temp), p.text(small * 1.05f, FontWeight.SemiBold, color = p.tempColor(h.temp)).copy(textAlign = TextAlign.Center))
                    metrics.forEach { m -> HourCell(p, hourValue(p, h, m), small * 0.78f) }
                }
            }
        }
    }
}

@Composable
private fun HourList(p: GadgetPaint, hours: List<WeatherHour>, modifier: Modifier) {
    val st = p.st
    BoxWithConstraints(modifier) {
        val shown = hours.take((maxHeight.value / (20f * st.textScale)).toInt().coerceAtLeast(1))
        val rowH = maxHeight.value / shown.size
        val fs = minOf(rowH * 0.45f, 16f).coerceAtLeast(7f)
        val cellW = fs * 3.3f * st.textScale
        // Kolumny wartości od lewej, dopóki zostaje miejsce na godzinę, ikonę, opis i temperaturę
        val metrics = hourMetrics.filter { p.has(it) }.take(((maxWidth.value - fs * 14f * st.textScale) / cellW).toInt().coerceAtLeast(0))
        val describe = maxWidth.value - metrics.size * cellW > fs * 17f * st.textScale
        Column(Modifier.fillMaxSize()) {
            shown.forEachIndexed { i, h ->
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    T(hourLabel(h, i == 0, st), p.text(fs, if (i == 0) FontWeight.Bold else FontWeight.Medium).copy(textAlign = TextAlign.Start), Modifier.width((fs * 3.6f * st.textScale).dp))
                    WxIcon(p, h.code, h.isDay, minOf(rowH * 0.75f, fs * 1.7f * st.textScale).dp)
                    T(if (describe) WeatherRepo.describeShort(h.code) else "", p.text(fs * 0.85f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Start), Modifier.weight(1f).padding(start = 8.dp))
                    metrics.forEach { m -> HourCell(p, hourValue(p, h, m), fs * 0.8f, Modifier.width(cellW.dp)) }
                    T(p.deg(h.temp), p.text(fs, FontWeight.SemiBold, color = p.tempColor(h.temp)).copy(textAlign = TextAlign.End), Modifier.width((fs * 3.2f * st.textScale).dp))
                }
                if (i < shown.lastIndex) Separator(p)
            }
        }
    }
}

// ---------- Widżety ----------

@Composable
internal fun ClockWeather(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(st.showSeconds)
    val weather = rememberWeather(st)
    val locale = Locale.getDefault()
    val (time, ampm) = timeText(now, st, locale)
    val date = cap(now.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)), locale)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val big = minOf(maxHeight.value * 0.5f, maxWidth.value * 0.55f / ((time.length + 0.5f) * 0.58f)).coerceAtLeast(14f)
        val small = (big * 0.24f).coerceIn(11f, 20f)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.Bottom) {
                    T(time, p.text(big).copy(letterSpacing = (-1).sp, textAlign = TextAlign.Start))
                    if (ampm.isNotEmpty()) T(" $ampm", p.text(big * 0.28f, FontWeight.Medium, 0.85f), Modifier.padding(bottom = (big * 0.18f).dp))
                }
                if (st.showDate) T(date, p.text(small, FontWeight.Normal, 0.9f).copy(textAlign = TextAlign.Start))
            }
            Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(p.fg.copy(alpha = 0.3f)))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val w = weather?.getOrNull()
                if (w != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WxIcon(p, w.code, w.isDay, (big * 0.62f).dp)
                        Spacer(Modifier.width(6.dp))
                        T(p.deg(w.temp), p.text(big * 0.62f, color = p.tempColor(w.temp)))
                    }
                    T(WeatherRepo.describe(w.code), p.text(small * 0.9f, FontWeight.Normal, 0.85f))
                    if (st.showDetails) T(w.place, p.text(small * 0.8f, FontWeight.Normal, 0.65f))
                } else WeatherPlaceholder(p, weather, small)
            }
        }
    }
}

/** Pogoda teraz. Układ: 0 = klasyczny, 1 = duża temperatura, 2 = jedna linia. */
@Composable
internal fun WeatherNowView(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val today = w.days.firstOrNull { it.date == placeNow(w).toLocalDate() } ?: w.today
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val metrics = currentMetrics(p, w)
        when (st.mode) {
            1 -> {
                val big = minOf(maxHeight.value * 0.55f, maxWidth.value * 0.32f).coerceAtLeast(16f)
                val small = (big * 0.22f).coerceIn(9f, 16f)
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        T(p.deg(w.temp), p.text(big, color = p.tempColor(w.temp)).copy(letterSpacing = (-2).sp, textAlign = TextAlign.Start))
                        T(WeatherRepo.describe(w.code), p.text(small * 1.05f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                        if (st.showDetails) T(
                            listOfNotNull(w.place, today?.let { "${p.deg(it.max)} / ${p.deg(it.min)}" }).joinToString(" · "),
                            p.text(small * 0.9f, FontWeight.Normal, 0.75f).copy(textAlign = TextAlign.Start),
                        )
                        MetricStrip(p, metrics, small * 0.85f, Modifier.padding(top = 2.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        WxIcon(p, w.code, w.isDay, (big * 0.95f).dp)
                        UpdatedLabel(p, w, small * 0.75f)
                    }
                }
            }
            2 -> {
                val big = minOf(maxHeight.value * 0.6f, maxWidth.value * 0.18f).coerceAtLeast(12f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WxIcon(p, w.code, w.isDay, (big * 1.1f).dp)
                    Spacer(Modifier.width(8.dp))
                    T(p.deg(w.temp), p.text(big, color = p.tempColor(w.temp)))
                    if (st.showDetails) T("  ${w.place}", p.text(big * 0.4f, FontWeight.Normal, 0.75f))
                }
            }
            else -> {
                val big = minOf(maxHeight.value * 0.42f, maxWidth.value * 0.26f).coerceAtLeast(14f)
                val small = (big * 0.3f).coerceIn(10f, 18f)
                Column(horizontalAlignment = p.hAlign) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(w.place, p.text(small, FontWeight.SemiBold, 0.85f))
                        if (st.showUpdated) {
                            Spacer(Modifier.width(6.dp))
                            UpdatedLabel(p, w, small * 0.75f)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WxIcon(p, w.code, w.isDay, (big * 0.95f).dp)
                        Spacer(Modifier.width(8.dp))
                        T(p.deg(w.temp), p.text(big, color = p.tempColor(w.temp)))
                    }
                    T(WeatherRepo.describe(w.code) + (today?.let { "  ${p.deg(it.max)} / ${p.deg(it.min)}" } ?: ""), p.text(small, FontWeight.Normal, 0.9f))
                    if (st.showDetails && metrics.isEmpty()) {
                        T("Odczuwalna ${p.deg(w.feels)} · ${p.wind(w.wind)} · ${w.humidity}%", p.text(small * 0.85f, FontWeight.Normal, 0.7f))
                    }
                    MetricStrip(p, metrics, small * 0.8f)
                }
            }
        }
    }
}

/** Prognoza dzienna. Układ: 0 = kolumny, 1 = lista z paskami, 2 = wykres, 3 = kompaktowy. */
@Composable
internal fun Forecast(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val days = shownDays(w, st)
    if (days.isEmpty()) return@WithWeather
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val small = (maxHeight.value * 0.1f).coerceIn(9f, 16f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (st.showDetails) HeaderLine(p, w, small)
            val m = Modifier.fillMaxWidth().weight(1f)
            when (st.mode) {
                1 -> DayList(p, w, days, m)
                2 -> DayChart(p, days, m)
                3 -> DayColumns(p, w, days, compact = true, m)
                else -> DayColumns(p, w, days, compact = false, m)
            }
        }
    }
}

/** Prognoza godzinowa. Układ: 0 = wykres, 1 = kolumny, 2 = lista. */
@Composable
internal fun HourlyForecast(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val hours = shownHours(w, st)
    if (hours.isEmpty()) return@WithWeather
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val small = (maxHeight.value * 0.1f).coerceIn(9f, 16f)
        val tall = maxHeight.value > 120f
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (st.showDetails) HeaderLine(p, w, small)
            if (st.showSummary && tall) {
                T(
                    hourlySummary(p, shownHours(w, st.copy(hourStep = 1))),
                    p.text(small * 0.85f, FontWeight.Normal, 0.75f).copy(textAlign = TextAlign.Start),
                    Modifier.fillMaxWidth().then(if (!st.showDetails && st.showRefresh) Modifier.padding(end = 26.dp) else Modifier),
                )
            }
            val m = Modifier.fillMaxWidth().weight(1f)
            when (st.mode) {
                1 -> HourColumns(p, hours, m)
                2 -> HourList(p, hours, m)
                else -> HourChart(p, hours, m)
            }
        }
    }
}

/**
 * Pełny panel: bieżąca pogoda, pasek wartości, wykres godzinowy i lista dni.
 * Sekcje, które się nie zmieszczą, znikają od dołu.
 */
@Composable
internal fun WeatherFull(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val days = shownDays(w, st)
    val hours = shownHours(w, st)
    val today = w.days.firstOrNull { it.date == placeNow(w).toLocalDate() } ?: w.today
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight.value
        val big = (minOf(maxHeight.value * 0.16f, maxWidth.value * 0.17f)).coerceIn(22f, 64f)
        val small = (big * 0.27f).coerceIn(9f, 15f)
        val showHours = h > 170 && hours.size >= 3
        val showDays = h > 250 && days.isNotEmpty()
        val metrics = currentMetrics(p, w, skip = setOf(WeatherMetric.Feels))
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().padding(end = if (st.showRefresh) 22.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(w.place, p.text(small * 1.15f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start), Modifier.weight(1f, fill = false))
                        if (st.showUpdated) {
                            Spacer(Modifier.width(6.dp))
                            UpdatedLabel(p, w, small * 0.75f)
                        }
                    }
                    T(WeatherRepo.describe(w.code), p.text(small, FontWeight.Normal, 0.85f).copy(textAlign = TextAlign.Start))
                    T(
                        listOfNotNull(
                            today?.let { "↑ ${p.deg(it.max)}  ↓ ${p.deg(it.min)}" },
                            if (st.showDetails) "Odczuwalna ${p.deg(w.feels)}" else null,
                        ).joinToString("  ·  "),
                        p.text(small * 0.9f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Start),
                    )
                }
                WxIcon(p, w.code, w.isDay, (big * 1.05f).dp)
                Spacer(Modifier.width(6.dp))
                T(p.deg(w.temp), p.text(big, color = p.tempColor(w.temp)).copy(letterSpacing = (-1.5).sp))
            }
            if (metrics.isNotEmpty()) MetricStrip(p, metrics, small * 0.85f, Modifier.fillMaxWidth())
            if (showHours) {
                Separator(p)
                HourChart(p, hours, Modifier.fillMaxWidth().weight(if (showDays) 1f else 2f))
            }
            if (showDays) {
                Separator(p)
                DayList(p, w, days, Modifier.fillMaxWidth().weight(1.5f))
            } else if (!showHours) Spacer(Modifier.weight(1f))
        }
    }
}

/** Siatka albo lista szczegółów: wiatr z kierunkiem, wilgotność, ciśnienie, UV, opady… */
@Composable
internal fun WeatherDetails(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val metrics = currentMetrics(p, w)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (metrics.isEmpty()) {
            T("Wybierz wartości w „Dostosuj”", p.text(13f, FontWeight.Normal, 0.8f))
            return@BoxWithConstraints
        }
        val small = (maxHeight.value * 0.09f).coerceIn(9f, 15f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (st.showDetails) HeaderLine(p, w, small)
            if (st.mode == 1) {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val rowH = maxHeight.value / metrics.size
                    val fs = minOf(rowH * 0.45f, 16f).coerceAtLeast(7f)
                    Column(Modifier.fillMaxSize()) {
                        metrics.forEachIndexed { i, m ->
                            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                MetricIcon(m, p.accent, (fs * 1.3f * st.textScale).dp)
                                T(m.label, p.text(fs, FontWeight.Normal, 0.75f).copy(textAlign = TextAlign.Start), Modifier.weight(1f).padding(start = 8.dp))
                                T(m.value, p.text(fs, FontWeight.SemiBold).copy(textAlign = TextAlign.End))
                            }
                            if (i < metrics.lastIndex) Separator(p)
                        }
                    }
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val cols = (maxWidth.value / 96f).toInt().coerceIn(1, 5).coerceAtMost(metrics.size)
                    val rows = ceil(metrics.size / cols.toFloat()).toInt()
                    val tileH = (maxHeight.value - (rows - 1) * 6f) / rows
                    val fs = minOf(tileH * 0.22f, (maxWidth.value / cols) * 0.11f).coerceIn(7f, 15f)
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        metrics.chunked(cols).forEach { row ->
                            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { m ->
                                    Column(
                                        Modifier.weight(1f).fillMaxHeight()
                                            .clip(RoundedCornerShape((st.radius.coerceIn(0, 48) * 0.55f).dp))
                                            .background(p.fg.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            MetricIcon(m, p.accent, (fs * 1.1f * st.textScale).dp)
                                            Spacer(Modifier.width(4.dp))
                                            T(m.label, p.text(fs * 0.8f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Start))
                                        }
                                        T(m.value, p.text(fs * 1.2f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                                    }
                                }
                                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Łuk drogi słońca z jego położeniem teraz, godziny wschodu/zachodu i długość dnia. Układ: 0 = łuk, 1 = tekst. */
@Composable
internal fun SunArc(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val now = placeNow(w)
    val idx = w.days.indexOfFirst { it.date == now.toLocalDate() }.let { if (it < 0) 0 else it }
    val today = w.days.getOrNull(idx)
    val rise = today?.sunrise
    val set = today?.sunset
    if (today == null || rise == null || set == null) {
        T("Brak danych o słońcu", p.text(13f, FontWeight.Normal, 0.8f))
        return@WithWeather
    }
    val dayLen = Duration.between(rise, set)
    val tomorrow = w.days.getOrNull(idx + 1)?.let { t -> if (t.sunrise != null && t.sunset != null) Duration.between(t.sunrise, t.sunset) else null }
    val diff = tomorrow?.let { (it.seconds - dayLen.seconds) / 60 }
    val isDay = now.isAfter(rise) && now.isBefore(set)
    val frac = (Duration.between(rise, now).seconds.toFloat() / dayLen.seconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    val status = when {
        isDay -> "Do zachodu ${lengthText(Duration.between(now, set))}"
        now.isBefore(rise) -> "Do wschodu ${lengthText(Duration.between(now, rise))}"
        else -> w.days.getOrNull(idx + 1)?.sunrise?.let { "Do wschodu ${lengthText(Duration.between(now, it))}" } ?: "Po zachodzie"
    }
    val sunColor = if (st.iconStyle == 1) Color(0xFFFFC94D) else p.accent
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fs = (maxHeight.value * 0.09f).coerceIn(9f, 16f)
        val info: @Composable (Modifier) -> Unit = { m ->
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    T("Wschód", p.text(fs * 0.8f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Start))
                    T(clock(rise, st), p.text(fs * 1.2f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                }
                Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                    T(lengthText(dayLen), p.text(fs, FontWeight.SemiBold).copy(textAlign = TextAlign.Center))
                    if (st.showDetails && diff != null) {
                        T("jutro ${if (diff >= 0) "+" else "−"}${kotlin.math.abs(diff)} min", p.text(fs * 0.8f, FontWeight.Normal, 0.65f, color = p.accent).copy(textAlign = TextAlign.Center))
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    T("Zachód", p.text(fs * 0.8f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.End))
                    T(clock(set, st), p.text(fs * 1.2f, FontWeight.SemiBold).copy(textAlign = TextAlign.End))
                }
            }
        }
        if (st.mode == 1) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                if (st.showDetails) T(w.place, p.text(fs, FontWeight.SemiBold, 0.85f), Modifier.fillMaxWidth())
                info(Modifier.fillMaxWidth())
                T(status, p.text(fs * 0.85f, FontWeight.Normal, 0.75f), Modifier.fillMaxWidth())
            }
            return@BoxWithConstraints
        }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Canvas(Modifier.fillMaxSize()) {
                    val pad = size.width * 0.08f
                    val horizon = size.height * 0.86f
                    val rx = size.width / 2f - pad
                    val ry = (horizon - size.height * 0.08f).coerceAtLeast(1f)
                    val cx = size.width / 2f
                    val stroke = (2f * density)
                    drawLine(p.fg.copy(alpha = 0.3f), Offset(0f, horizon), Offset(size.width, horizon), stroke * 0.6f)
                    drawArc(
                        p.fg.copy(alpha = 0.35f), 180f, 180f, false, Offset(cx - rx, horizon - ry), Size(rx * 2, ry * 2),
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2.5f, stroke * 2.5f))),
                    )
                    if (isDay) {
                        drawArc(sunColor, 180f, 180f * frac, false, Offset(cx - rx, horizon - ry), Size(rx * 2, ry * 2), style = Stroke(stroke * 1.4f, cap = StrokeCap.Round))
                        val a = PI * (1 - frac)
                        val sun = Offset(cx + rx * cos(a).toFloat(), horizon - ry * sin(a).toFloat())
                        val sr = (size.minDimension * 0.07f).coerceAtLeast(4f)
                        drawCircle(sunColor.copy(alpha = 0.2f), sr * 2.2f, sun)
                        drawCircle(sunColor.copy(alpha = 0.35f), sr * 1.5f, sun)
                        drawCircle(sunColor, sr, sun)
                    } else {
                        val sr = (size.minDimension * 0.06f).coerceAtLeast(4f)
                        drawCircle(p.fg.copy(alpha = 0.6f), sr, Offset(cx, horizon + sr * 0.2f))
                    }
                }
                T(
                    status,
                    p.text(fs * 0.85f, FontWeight.Normal, 0.8f).copy(textAlign = TextAlign.Center),
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = (fs * 1.6f).dp),
                )
            }
            info(Modifier.fillMaxWidth())
        }
    }
}
