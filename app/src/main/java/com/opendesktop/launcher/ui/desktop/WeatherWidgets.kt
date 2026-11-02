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
                    T(time, p.hero(big).copy(letterSpacing = (-1).sp, textAlign = TextAlign.Start))
                    if (ampm.isNotEmpty()) T(" $ampm", p.hero(big * 0.28f, FontWeight.Medium, 0.85f), Modifier.padding(bottom = (big * 0.18f).dp))
                }
                if (st.showDate) T(date, p.text(small, FontWeight.Normal, 0.9f).copy(textAlign = TextAlign.Start), p.gap)
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

/**
 * Zegar z dniem tygodnia i datą; po lewej pogoda teraz, po prawej dzisiejszy Księżyc.
 * Nad ikonami godzina wschodu, pod nimi zachodu (słońca – z prognozy, Księżyca – liczone lokalnie).
 */
@Composable
internal fun SkyClock(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(st.showSeconds)
    val state = rememberWeather(st)
    val w = state?.getOrNull()
    val locale = Locale.getDefault()
    val (time, ampm) = timeText(now, st, locale)
    val here = w?.let { placeNow(it) } ?: now
    val day = here.toLocalDate()
    val today = w?.days?.firstOrNull { it.date == day }
    val phase = Moon.phase(day)
    val moon = remember(w?.lat, w?.lon, w?.utcOffset, day) { w?.let { Moon.riseSet(day, it.lat, it.lon, it.utcOffset) } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val icon = minOf(maxHeight.value * 0.34f, maxWidth.value * 0.15f).coerceAtLeast(16f)
        val small = (icon * 0.32f).coerceIn(9f, 15f)
        val big = minOf(maxHeight.value * 0.4f, maxWidth.value * 0.52f / ((time.length + 0.5f) * 0.58f)).coerceAtLeast(14f)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            SkyColumn(p, today?.sunrise, today?.sunset, small) {
                if (w != null) {
                    WxIcon(p, w.code, w.isDay, icon.dp)
                    if (st.showDetails) T(p.deg(w.temp), p.text(small * 1.1f, FontWeight.SemiBold, color = p.tempColor(w.temp)))
                } else ShadowedGlyph(if (state == null) Icons.Rounded.Cloud else Icons.Rounded.CloudOff, p.fg.copy(alpha = 0.6f), icon.dp, st.textShadow)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    T(time, p.hero(big).copy(letterSpacing = (-1).sp, textAlign = TextAlign.Center))
                    if (ampm.isNotEmpty()) T(" $ampm", p.hero(big * 0.28f, FontWeight.Medium, 0.85f), Modifier.padding(bottom = (big * 0.18f).dp))
                }
                T(cap(now.format(DateTimeFormatter.ofPattern("EEEE", locale)), locale), p.text(small * 1.3f, FontWeight.Medium), p.gap)
                if (st.showDate) T(now.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)), p.text(small * 1.05f, FontWeight.Normal, 0.75f), p.gap)
            }
            SkyColumn(p, moon?.first, moon?.second, small) {
                MoonDisk(phase, moonLit(p), p.faint, st.southern, Modifier.size(icon.dp), st.realMoon)
                if (st.showDetails) T("${Moon.illumination(phase)}%", p.text(small * 1.1f, FontWeight.SemiBold, 0.85f))
            }
        }
    }
}

/** Kolumna: wschód nad ikoną, zachód pod nią. */
@Composable
internal fun SkyColumn(p: GadgetPaint, rise: LocalDateTime?, set: LocalDateTime?, fs: Float, center: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
        SkyTime(p, Icons.Rounded.NorthEast, rise, fs)
        center()
        SkyTime(p, Icons.Rounded.SouthEast, set, fs)
    }
}

@Composable
internal fun SkyTime(p: GadgetPaint, icon: ImageVector, t: LocalDateTime?, fs: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = p.accent, modifier = Modifier.size((fs * 0.9f).dp))
        T(clock(t, p.st), p.text(fs, FontWeight.Medium, 0.85f))
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
