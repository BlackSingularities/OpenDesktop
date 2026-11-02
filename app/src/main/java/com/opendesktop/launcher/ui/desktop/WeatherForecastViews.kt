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

// ---------- Wykresy ----------

/**
 * Krzywa temperatury z etykietami nad punktami (i opcjonalnie druga, przerywana, z etykietami pod spodem).
 * Punkty leżą na środkach [main].size równych kolumn – tak samo jak Row z weight(1f).
 */
@Composable
internal fun TempCurve(
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
internal fun RangeBar(p: GadgetPaint, lo: Double, hi: Double, gLo: Double, gHi: Double, current: Double?, modifier: Modifier) {
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
internal fun DayColumns(p: GadgetPaint, w: WeatherNow, days: List<Pair<Int, WeatherDay>>, compact: Boolean, modifier: Modifier) {
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
internal fun DayList(p: GadgetPaint, w: WeatherNow, days: List<Pair<Int, WeatherDay>>, modifier: Modifier) {
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
internal fun DayChart(p: GadgetPaint, days: List<Pair<Int, WeatherDay>>, modifier: Modifier) {
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
internal fun HourChart(p: GadgetPaint, hours: List<WeatherHour>, modifier: Modifier) {
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

internal fun metricName(m: String) = when (m) {
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
internal fun HourColumns(p: GadgetPaint, hours: List<WeatherHour>, modifier: Modifier) {
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
internal fun HourList(p: GadgetPaint, hours: List<WeatherHour>, modifier: Modifier) {
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
