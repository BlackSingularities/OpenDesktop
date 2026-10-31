package com.opendesktop.launcher.ui.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opendesktop.launcher.data.AirNow
import com.opendesktop.launcher.data.RainSlot
import com.opendesktop.launcher.data.WeatherExtras
import com.opendesktop.launcher.data.WeatherMetric
import com.opendesktop.launcher.data.WeatherNow
import com.opendesktop.launcher.data.WeatherUnits
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------- Wykres pogody: godziny albo dni ----------

/** Jeden punkt wykresu – godzina albo dzień; brakujące wartości (np. ciśnienie w dniach) = null. */
private class ChartPoint(
    val label: String,
    /** Etykieta nowej doby (nazwa dnia o północy) albo null. */
    val dayMark: String?,
    val temp: Double,
    /** Druga krzywa: odczuwalna (godziny) albo minimum (dni). */
    val temp2: Double?,
    val code: Int,
    val isDay: Boolean,
    val precipProb: Int,
    val precipMm: Double,
    val wind: Double?,
    val gusts: Double?,
    val humidity: Double?,
    val pressure: Double?,
    val cloud: Double?,
    val uv: Double?,
    val highlight: Boolean,
)

/** Dodatkowy pas pod wykresem: nazwa, wartości i sposób pokazania wartości. */
private class Lane(val metric: String, val values: List<Double?>, val text: (Double) -> String, val color: Color, val bars: Boolean = false)

private fun chartPoints(p: GadgetPaint, w: WeatherNow, days: Boolean, locale: Locale): List<ChartPoint> {
    val st = p.st
    if (days) {
        return shownDays(w, st).map { (off, d) ->
            ChartPoint(
                label = dayLabel(off, d.date, st, locale),
                dayMark = null,
                temp = d.max,
                temp2 = d.min,
                code = d.code,
                isDay = true,
                precipProb = d.precipProb,
                precipMm = d.precipSum,
                wind = d.windMax,
                gusts = null,
                humidity = null,
                pressure = null,
                cloud = null,
                uv = d.uv,
                highlight = off == 0 && st.highlightToday,
            )
        }
    }
    return shownHours(w, st).mapIndexed { i, h ->
        ChartPoint(
            label = hourLabel(h, i == 0, st),
            dayMark = if (i > 0 && h.time.hour < st.hourStep.coerceAtLeast(1)) cap(h.time.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale), locale) else null,
            temp = h.temp,
            temp2 = if (p.has(WeatherMetric.Feels)) h.feels else null,
            code = h.code,
            isDay = h.isDay,
            precipProb = h.precipProb,
            precipMm = h.precip,
            wind = h.wind,
            gusts = h.gusts,
            humidity = h.humidity.toDouble(),
            pressure = h.pressure,
            cloud = h.cloud.toDouble(),
            uv = if (h.isDay) h.uv else 0.0,
            highlight = i == 0 && st.highlightToday,
        )
    }
}

/**
 * Wykres pogody: krzywa temperatury (z odczuwalną albo minimum), słupki opadów i pasy z wybranymi
 * wartościami (wiatr, wilgotność, ciśnienie, zachmurzenie, UV). Układ: 0 = godziny, 1 = dni.
 * Wszystko rysowane na jednym Canvasie – nawet 168 godzin nie obciąża pulpitu.
 */
@Composable
internal fun WeatherChartView(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val locale = Locale.getDefault()
    val days = st.mode == 1
    val points = chartPoints(p, w, days, locale)
    if (points.size < 2) {
        T("Za mało danych – zwiększ zakres", p.text(12f, FontWeight.Normal, 0.75f))
        return@WithWeather
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val small = (maxHeight.value * 0.075f).coerceIn(8f, 14f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (st.showDetails) HeaderLine(p, w, small * 1.05f)
            val lanes = chartLanes(p, points)
            ChartCanvas(p, points, lanes, small, days, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

private fun chartLanes(p: GadgetPaint, pts: List<ChartPoint>): List<Lane> {
    val st = p.st
    val dim = p.fg.copy(alpha = 0.8f)
    return listOfNotNull(
        if (p.has(WeatherMetric.Wind)) Lane(WeatherMetric.Wind, pts.map { it.wind }, { "${WeatherUnits.wind(it, st.windUnit).roundToInt()}" }, Color(0xFF9FE6C3)) else null,
        if (p.has(WeatherMetric.Humidity)) Lane(WeatherMetric.Humidity, pts.map { it.humidity }, { "${it.roundToInt()}%" }, Color(0xFF8FD3FF)) else null,
        if (p.has(WeatherMetric.Cloud)) Lane(WeatherMetric.Cloud, pts.map { it.cloud }, { "${it.roundToInt()}%" }, Color(0xFFCFD8E3), bars = true) else null,
        if (p.has(WeatherMetric.Uv)) Lane(WeatherMetric.Uv, pts.map { it.uv }, { "${it.roundToInt()}" }, Color(0xFFFFB347), bars = true) else null,
        if (p.has(WeatherMetric.Pressure)) Lane(
            WeatherMetric.Pressure, pts.map { it.pressure },
            { WeatherUnits.pressure(it, st.pressureUnit).let { v -> if (st.pressureUnit == 2) "%.2f".format(v) else "${v.roundToInt()}" } }, dim,
        ) else null,
    ).filter { lane -> lane.values.any { it != null } }
}

@Composable
private fun ChartCanvas(p: GadgetPaint, pts: List<ChartPoint>, lanes: List<Lane>, small: Float, days: Boolean, modifier: Modifier) {
    val st = p.st
    val measurer = rememberTextMeasurer()
    val label = p.text(small * 0.9f, FontWeight.Medium, 0.85f).copy(textAlign = TextAlign.Center)
    val tempStyle = p.text(small, FontWeight.SemiBold).copy(textAlign = TextAlign.Center)
    val temp2Style = p.text(small * 0.88f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Center)
    val laneStyle = p.text(small * 0.78f, FontWeight.Normal, 0.75f).copy(textAlign = TextAlign.Center)
    val precipCol = precipColor(p)
    val showIcons = st.showCondition
    val showRain = p.has(WeatherMetric.Precip) || p.has(WeatherMetric.Rain)
    val iconPx = small * 1.9f * st.iconScale.coerceIn(0.5f, 2f)
    BoxWithConstraints(modifier) {
        val n = pts.size
        val colW = maxWidth.value / n
        // Co ile kolumn podpis – żeby etykiety się nie nakładały
        val every = ceil((small * 2.6f * st.textScale) / colW).toInt().coerceAtLeast(1)
        val labelH = small * 1.5f * st.textScale
        val iconsH = if (showIcons) iconPx + 2f else 0f
        val laneH = small * 2.1f * st.textScale
        // Pasy znikają od końca, gdy główny wykres zrobiłby się za niski
        val minMain = small * 6f * st.textScale
        val fitLanes = lanes.take(((maxHeight.value - labelH - iconsH - minMain) / laneH).toInt().coerceAtLeast(0))
        Canvas(Modifier.fillMaxSize()) {
            val px = density
            val top = (labelH + iconsH) * px
            val mainBottom = size.height - fitLanes.size * laneH * px
            drawChart(p, pts, measurer, tempStyle, temp2Style, label, precipCol, showRain, every, top, mainBottom, labelH * px, days)
            fitLanes.forEachIndexed { li, lane ->
                val y0 = mainBottom + li * laneH * px
                drawLane(p, lane, measurer, laneStyle, every, y0, laneH * px)
            }
        }
        if (showIcons) {
            pts.forEachIndexed { i, pt ->
                if (i % every != 0) return@forEachIndexed
                Box(
                    Modifier
                        .offset(x = ((i + 0.5f) * colW - iconPx / 2f).dp, y = labelH.dp)
                        .size(iconPx.dp),
                    contentAlignment = Alignment.Center,
                ) { WxIcon(p, pt.code, pt.isDay, (iconPx / st.iconScale.coerceIn(0.5f, 2f)).dp) }
            }
        }
    }
}

private fun DrawScope.textAt(measurer: TextMeasurer, text: String, style: TextStyle, cx: Float, y: Float) {
    if (text.isEmpty()) return
    val m = measurer.measure(text, style)
    val x = (cx - m.size.width / 2f).coerceIn(0f, (size.width - m.size.width).coerceAtLeast(0f))
    drawText(m, topLeft = Offset(x, y))
}

private fun DrawScope.drawChart(
    p: GadgetPaint,
    pts: List<ChartPoint>,
    measurer: TextMeasurer,
    tempStyle: TextStyle,
    temp2Style: TextStyle,
    label: TextStyle,
    precipCol: Color,
    showRain: Boolean,
    every: Int,
    top: Float,
    bottom: Float,
    labelH: Float,
    days: Boolean,
) {
    val st = p.st
    val n = pts.size
    val colW = size.width / n
    fun x(i: Int) = (i + 0.5f) * colW
    // Podpisy osi X i znaczniki nowej doby
    pts.forEachIndexed { i, pt ->
        if (pt.dayMark != null) {
            drawLine(p.faint, Offset(i * colW, labelH * 0.9f), Offset(i * colW, bottom), 1f * density)
            textAt(measurer, pt.dayMark, label.copy(color = p.accent, fontWeight = FontWeight.Bold), x(i), 0f)
        } else if (i % every == 0) {
            textAt(measurer, pt.label, label.copy(fontWeight = if (pt.highlight) FontWeight.Bold else label.fontWeight), x(i), 0f)
        }
    }
    val tLabelH = measurer.measure("0°", tempStyle).size.height.toFloat()
    val has2 = pts.any { it.temp2 != null }
    val curveTop = top + tLabelH * 1.15f
    val rainH = if (showRain) (bottom - top) * 0.28f else 0f
    val curveBottom = bottom - (if (has2) tLabelH * 1.1f else tLabelH * 0.3f) - rainH * 0.35f
    if (curveBottom <= curveTop) return
    // Słupki opadów u dołu (szansa w %, wysokość wg sumy mm – co najmniej widoczna kreska)
    if (showRain) {
        val maxMm = pts.maxOf { it.precipMm }.coerceAtLeast(if (days) 10.0 else 2.0)
        pts.forEachIndexed { i, pt ->
            val frac = if (p.has(WeatherMetric.Rain)) (pt.precipMm / maxMm).toFloat() else pt.precipProb / 100f
            if (frac <= 0.01f) return@forEachIndexed
            val bh = rainH * frac.coerceIn(0.04f, 1f)
            val bw = (colW * 0.62f).coerceAtLeast(1.5f)
            drawRoundRect(
                precipCol.copy(alpha = 0.25f + 0.5f * (pt.precipProb / 100f)),
                Offset(x(i) - bw / 2f, bottom - bh), Size(bw, bh), CornerRadius(bw * 0.3f),
            )
            if (i % every == 0 && pt.precipProb >= 20) {
                val t = if (p.has(WeatherMetric.Rain)) "%.1f".format(pt.precipMm) else "${pt.precipProb}%"
                val m = measurer.measure(t, label.copy(color = precipCol, fontSize = label.fontSize * 0.8f))
                if (bottom - bh - m.size.height > curveBottom) drawText(m, topLeft = Offset((x(i) - m.size.width / 2f).coerceIn(0f, size.width - m.size.width), bottom - bh - m.size.height))
            }
        }
    }
    val all = pts.map { it.temp } + pts.mapNotNull { it.temp2 }
    val lo = all.min()
    val hi = all.max()
    val span = (hi - lo).coerceAtLeast(1.0)
    fun y(v: Double) = (curveBottom - (v - lo) / span * (curveBottom - curveTop)).toFloat()
    fun curve(vals: List<Double>): Path = Path().apply {
        moveTo(x(0), y(vals[0]))
        for (i in 1 until vals.size) {
            val mx = (x(i - 1) + x(i)) / 2f
            cubicTo(mx, y(vals[i - 1]), mx, y(vals[i]), x(i), y(vals[i]))
        }
    }
    val stroke = (2.2f * density).coerceAtLeast(2f)
    val temps = pts.map { it.temp }
    val line = curve(temps)
    if (st.chartFill) {
        val fill = Path().apply {
            addPath(line)
            lineTo(x(n - 1), bottom)
            lineTo(x(0), bottom)
            close()
        }
        val tint = if (st.tempColors) temperatureColor(temps.average()) else p.accent
        drawPath(fill, Brush.verticalGradient(listOf(tint.copy(alpha = 0.32f), tint.copy(alpha = 0f)), startY = curveTop, endY = bottom))
    }
    if (has2) {
        val second = pts.map { it.temp2 ?: it.temp }
        drawPath(
            curve(second), p.fg.copy(alpha = 0.45f),
            style = Stroke(stroke * 0.7f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2.5f, stroke * 2f))),
        )
    }
    val brush: Brush = if (st.tempColors) Brush.horizontalGradient(temps.map { temperatureColor(it) }, startX = x(0), endX = x(n - 1)) else SolidColor(p.accent)
    drawPath(line, brush, style = Stroke(stroke, cap = StrokeCap.Round))
    // Najcieplej i najzimniej zawsze podpisane, nawet poza rytmem etykiet
    val iMax = temps.indices.maxBy { temps[it] }
    val iMin = temps.indices.minBy { temps[it] }
    pts.forEachIndexed { i, pt ->
        val c = Offset(x(i), y(pt.temp))
        val labeled = i % every == 0 || i == iMax || i == iMin
        if (pt.highlight) {
            drawLine(p.fg.copy(alpha = 0.35f), Offset(c.x, curveTop - tLabelH), Offset(c.x, bottom), 1f * density)
        }
        if (!labeled) return@forEachIndexed
        val dot = if (pt.highlight) stroke * 2f else stroke * 1.3f
        drawCircle(if (st.tempColors) temperatureColor(pt.temp) else p.accent, dot, c)
        drawCircle(p.fg, dot * 0.45f, c)
        // Wartość nad punktem; przy gęstych godzinach tylko w rytmie etykiet i dla skrajnych
        if (i % every == 0 || (i == iMax && every > 1) || (i == iMin && every > 1)) {
            textAt(measurer, p.deg(pt.temp), tempStyle.copy(color = p.tempColor(pt.temp)), c.x, c.y - tLabelH - stroke)
        }
        pt.temp2?.let { t2 -> if (i % every == 0) textAt(measurer, p.deg(t2), temp2Style, c.x, y(t2) + stroke * 1.5f) }
    }
}

private fun DrawScope.drawLane(p: GadgetPaint, lane: Lane, measurer: TextMeasurer, style: TextStyle, every: Int, y0: Float, h: Float) {
    val vals = lane.values
    val n = vals.size
    val colW = size.width / n
    fun x(i: Int) = (i + 0.5f) * colW
    if (p.st.separators) drawLine(p.faint, Offset(0f, y0), Offset(size.width, y0), 1f * density)
    val nameStyle = style.copy(color = lane.color, fontSize = style.fontSize * 0.85f, textAlign = TextAlign.Start)
    val name = measurer.measure(metricName(lane.metric), nameStyle)
    drawText(name, topLeft = Offset(2f, y0 + 1f))
    val present = vals.filterNotNull()
    if (present.isEmpty()) return
    val lo = if (lane.bars) 0.0 else present.min()
    val hi = present.max().let { if (lane.metric == WeatherMetric.Uv) it.coerceAtLeast(8.0) else it }
    val span = (hi - lo).coerceAtLeast(if (lane.metric == WeatherMetric.Pressure) 4.0 else 1.0)
    val textH = measurer.measure("0", style).size.height.toFloat()
    val gTop = y0 + name.size.height * 0.6f + textH * 0.2f
    val gBottom = y0 + h - textH * 1.05f
    if (gBottom <= gTop) return
    fun y(v: Double) = (gBottom - (v - lo) / span * (gBottom - gTop)).toFloat()
    if (lane.bars) {
        vals.forEachIndexed { i, v ->
            if (v == null || v <= 0.0) return@forEachIndexed
            val bw = (colW * 0.6f).coerceAtLeast(1.5f)
            drawRoundRect(lane.color.copy(alpha = 0.55f), Offset(x(i) - bw / 2f, y(v)), Size(bw, gBottom - y(v)), CornerRadius(bw * 0.25f))
        }
    } else {
        val path = Path()
        var started = false
        vals.forEachIndexed { i, v ->
            if (v == null) return@forEachIndexed
            if (!started) { path.moveTo(x(i), y(v)); started = true } else path.lineTo(x(i), y(v))
        }
        drawPath(path, lane.color, style = Stroke(1.6f * density, cap = StrokeCap.Round))
    }
    vals.forEachIndexed { i, v ->
        if (v == null || i % every != 0) return@forEachIndexed
        textAt(measurer, lane.text(v), style, x(i), gBottom + textH * 0.05f)
    }
}

// ---------- Jakość powietrza ----------

/** Zanieczyszczenia: klucz z [GadgetStyle.metrics], nazwa, wartość i próg „bardzo złej” jakości (µg/m³). */
private class Pollutant(val key: String, val name: String, val value: Double?, val bad: Double)

private fun pollutants(a: AirNow) = listOf(
    Pollutant("pm25", "PM2.5", a.pm25, 75.0),
    Pollutant("pm10", "PM10", a.pm10, 150.0),
    Pollutant("no2", "NO₂", a.no2, 230.0),
    Pollutant("o3", "O₃", a.o3, 240.0),
    Pollutant("so2", "SO₂", a.so2, 500.0),
    Pollutant("co", "CO", a.co, 10_000.0),
)

/** Lista zanieczyszczeń do wyboru w oknie Dostosuj (klucz, nazwa). */
val AirMetrics = listOf("pm25" to "PM2.5", "pm10" to "PM10", "no2" to "NO₂", "o3" to "Ozon", "so2" to "SO₂", "co" to "CO", "pollen" to "Pyłki")

@Composable
private fun rememberAir(lat: Double, lon: Double): Result<AirNow>? {
    var state by remember(lat, lon) { mutableStateOf<Result<AirNow>?>(null) }
    LaunchedEffect(lat, lon, WeatherRefresh.tick) {
        while (true) {
            val r = runCatching { WeatherExtras.air(lat, lon) }
            if (r.isSuccess || state?.isSuccess != true) state = r
            delay(if (r.isSuccess) 30 * 60_000L else 3 * 60_000L)
        }
    }
    return state
}

/** Jakość powietrza. Układ: 0 = wskaźnik, 1 = lista, 2 = prognoza indeksu na 24 h. */
@Composable
internal fun AirQualityView(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val state = rememberAir(w.lat, w.lon)
    val a = state?.getOrNull()
    if (a == null) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
            T(if (state == null) "Pobieranie jakości powietrza…" else "Brak danych o powietrzu", p.text(12f, FontWeight.Normal, 0.8f))
        }
        return@WithWeather
    }
    val st = p.st
    val (level, colorL) = WeatherExtras.aqiLevel(a.aqi)
    val color = Color(colorL)
    val shown = pollutants(a).filter { it.key in st.metrics && it.value != null }
    val pollen = if ("pollen" in st.metrics) WeatherExtras.pollenNames.mapNotNull { (k, name) -> a.pollen[k]?.takeIf { it >= 1 }?.let { name to it } } else emptyList()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fs = (minOf(maxHeight.value * 0.1f, maxWidth.value * 0.06f)).coerceIn(8f, 15f)
        val ring = minOf(maxHeight, maxWidth * 0.42f)
        when (st.mode) {
            2 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size((fs * 0.9f).dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(6.dp))
                    T("${a.aqi} · $level", p.text(fs * 1.1f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start), Modifier.weight(1f))
                    if (st.showDetails) T(w.place, p.text(fs * 0.85f, FontWeight.Normal, 0.65f))
                    if (st.showRefresh) Spacer(Modifier.width(26.dp))
                }
                AqiBars(p, a, fs, Modifier.fillMaxWidth().weight(1f))
            }
            1 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T("Powietrze", p.text(fs, FontWeight.SemiBold, 0.85f).copy(textAlign = TextAlign.Start), Modifier.weight(1f))
                    T("${a.aqi} · $level", p.text(fs, FontWeight.Bold, color = color))
                    if (st.showRefresh) Spacer(Modifier.width(26.dp))
                }
                shown.forEach { PollutantRow(p, it, fs) }
                if (pollen.isNotEmpty()) PollenRow(p, pollen, fs)
            }
            else -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = size.minDimension * 0.1f
                        val d = size.minDimension - sw
                        val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                        drawArc(p.faint, 135f, 270f, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                        drawArc(color, 135f, 270f * (a.aqi / 100f).coerceIn(0.02f, 1f), false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        T("${a.aqi}", p.hero(ring.value * 0.28f, FontWeight.SemiBold).copy(textAlign = TextAlign.Center))
                        T("EAQI", p.text(ring.value * 0.09f, FontWeight.Normal, 0.6f).copy(textAlign = TextAlign.Center))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    T(level, p.text(fs * 1.2f, FontWeight.Bold, color = color).copy(textAlign = TextAlign.Start))
                    if (st.showDetails) T(w.place, p.text(fs * 0.85f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Start))
                    shown.take(4).forEach { PollutantRow(p, it, fs * 0.85f) }
                    if (pollen.isNotEmpty()) PollenRow(p, pollen, fs * 0.85f)
                }
            }
        }
    }
}

@Composable
private fun PollutantRow(p: GadgetPaint, x: Pollutant, fs: Float) {
    val v = x.value ?: return
    val frac = (v / x.bad).toFloat().coerceIn(0f, 1f)
    val col = Color(WeatherExtras.aqiLevel((frac * 100).roundToInt()).second)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T(x.name, p.text(fs, FontWeight.Medium, 0.85f).copy(textAlign = TextAlign.Start), Modifier.width((fs * 3.6f * p.st.textScale).dp))
        Box(Modifier.weight(1f).height((fs * 0.45f).coerceAtLeast(3f).dp).clip(RoundedCornerShape(50)).background(p.faint)) {
            Box(Modifier.fillMaxWidth(frac.coerceAtLeast(0.03f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(col))
        }
        T(if (v >= 100) "${v.roundToInt()}" else "%.1f".format(v), p.text(fs, FontWeight.SemiBold).copy(textAlign = TextAlign.End), Modifier.width((fs * 3f * p.st.textScale).dp))
    }
}

@Composable
private fun PollenRow(p: GadgetPaint, pollen: List<Pair<String, Double>>, fs: Float) {
    val levels = listOf("niski", "niski", "średni", "wysoki")
    T(
        "Pyłki: " + pollen.joinToString(", ") { (n, v) -> "$n ${levels[WeatherExtras.pollenLevel(v)]}" },
        p.text(fs * 0.9f, FontWeight.Normal, 0.8f, color = Color(0xFFC5E1A5)).copy(textAlign = TextAlign.Start),
        Modifier.fillMaxWidth(),
        maxLines = 2,
    )
}

@Composable
private fun AqiBars(p: GadgetPaint, a: AirNow, fs: Float, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val style = p.text(fs * 0.8f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Center)
    Canvas(modifier) {
        val hrs = a.hours
        if (hrs.isEmpty()) return@Canvas
        val n = hrs.size
        val cw = size.width / n
        val textH = measurer.measure("0", style).size.height.toFloat()
        val maxV = hrs.maxOf { it.second }.coerceAtLeast(60).toFloat()
        val h = size.height - textH * 1.2f
        hrs.forEachIndexed { i, (t, v) ->
            val bh = (h * v / maxV).coerceAtLeast(2f)
            val bw = cw * 0.66f
            drawRoundRect(Color(WeatherExtras.aqiLevel(v).second).copy(alpha = 0.85f), Offset(i * cw + (cw - bw) / 2f, h - bh), Size(bw, bh), CornerRadius(bw * 0.3f))
            if (i % 3 == 0) textAt(measurer, if (i == 0) "Teraz" else t.format(java.time.format.DateTimeFormatter.ofPattern("HH")), style, i * cw + cw / 2f, h + textH * 0.15f)
        }
    }
}

// ---------- Opady co 15 minut ----------

@Composable
private fun rememberRain(lat: Double, lon: Double): Result<List<RainSlot>>? {
    var state by remember(lat, lon) { mutableStateOf<Result<List<RainSlot>>?>(null) }
    LaunchedEffect(lat, lon, WeatherRefresh.tick) {
        while (true) {
            val r = runCatching { WeatherExtras.rain(lat, lon) }
            if (r.isSuccess || state?.isSuccess != true) state = r
            delay(if (r.isSuccess) 10 * 60_000L else 3 * 60_000L)
        }
    }
    return state
}

/** Podsumowanie najbliższych opadów: kiedy zacznie albo przestanie padać. */
internal fun rainSummary(slots: List<RainSlot>, now: LocalDateTime, use24h: Boolean): String {
    if (slots.isEmpty()) return "Brak danych"
    fun wet(s: RainSlot) = s.mm >= 0.1
    fun at(t: LocalDateTime) = t.format(java.time.format.DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", if (use24h) Locale.getDefault() else Locale.ENGLISH))
    fun inMin(t: LocalDateTime) = java.time.Duration.between(now, t).toMinutes().coerceAtLeast(0)
    val hours = (slots.size * 15) / 60
    val first = slots.first()
    return if (wet(first)) {
        val stop = slots.firstOrNull { !wet(it) }
        if (stop == null) "Pada – przez najbliższe $hours h nie przestanie" else "Pada – przestanie ok. ${at(stop.time)}"
    } else {
        val start = slots.firstOrNull(::wet)
        when {
            start == null -> "Bez opadów przez $hours h"
            inMin(start.time) <= 15 -> "Zaraz zacznie padać"
            inMin(start.time) < 60 -> "Opady za ${inMin(start.time)} min (ok. ${at(start.time)})"
            else -> "Opady od ok. ${at(start.time)}"
        }
    }
}

/** Opady co kwadrans na najbliższe 6 godzin: słupki i zdanie podsumowania. */
@Composable
internal fun RainNowView(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val state = rememberRain(w.lat, w.lon)
    val now = placeNow(w)
    val slots = state?.getOrNull()?.filter { !it.time.isBefore(now.minusMinutes(14)) }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fs = (maxHeight.value * 0.1f).coerceIn(9f, 15f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(
                    if (slots == null) (if (state == null) "Pobieranie opadów…" else "Brak prognozy opadów") else rainSummary(slots, now, p.st.use24h),
                    p.text(fs * 1.05f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start),
                    Modifier.weight(1f),
                    maxLines = 2,
                )
                if (p.st.showRefresh) Spacer(Modifier.width(26.dp))
            }
            if (p.st.showDetails) T(w.place, p.text(fs * 0.8f, FontWeight.Normal, 0.6f).copy(textAlign = TextAlign.Start), Modifier.fillMaxWidth())
            val style = p.text(fs * 0.75f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Center)
            val bar = p.accent
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val list = slots ?: return@Canvas
                if (list.isEmpty()) return@Canvas
                val n = list.size
                val cw = size.width / n
                val textH = measurer.measure("0", style).size.height.toFloat()
                val h = size.height - textH * 1.2f
                // Skala: 0–2 mm na kwadrans (ulewa), lżejsze opady i tak dobrze widoczne
                drawLine(p.faint, Offset(0f, h), Offset(size.width, h), 1f * density)
                list.forEachIndexed { i, s ->
                    if (s.mm >= 0.05) {
                        val bh = (h * (s.mm / 2.0).toFloat().coerceIn(0.06f, 1f))
                        val bw = cw * 0.72f
                        drawRoundRect(bar.copy(alpha = 0.45f + 0.5f * (s.mm / 2.0).toFloat().coerceIn(0f, 1f)), Offset(i * cw + (cw - bw) / 2f, h - bh), Size(bw, bh), CornerRadius(bw * 0.3f))
                    }
                    if (s.time.minute == 0) {
                        drawLine(p.faint, Offset(i * cw, h), Offset(i * cw, h + textH * 0.25f), 1f * density)
                        textAt(measurer, s.time.format(java.time.format.DateTimeFormatter.ofPattern(if (p.st.use24h) "HH:mm" else "h a", if (p.st.use24h) Locale.getDefault() else Locale.ENGLISH)), style, i * cw, h + textH * 0.2f)
                    }
                }
            }
        }
    }
}

// ---------- Wiatr ----------

/** Róża wiatrów: strzałka w stronę, w którą wieje, prędkość i porywy. Układ: 0 = sama róża, 1 = z opisem obok. */
@Composable
internal fun WindView(p: GadgetPaint) = WithWeather(p) { w, _ ->
    val st = p.st
    val measurer = rememberTextMeasurer()
    val letter = p.text(10f, FontWeight.Bold, 0.7f).copy(textAlign = TextAlign.Center)
    val speed = "${WeatherUnits.wind(w.wind, st.windUnit).roundToInt()}"
    val unit = WeatherUnits.windNames[st.windUnit.coerceIn(0, 3)]
    val gusts = "${WeatherUnits.wind(w.gusts, st.windUnit).roundToInt()}"
    val beaufort = beaufort(w.wind)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val wide = st.mode == 1 && maxWidth > maxHeight * 1.4f
        val rose = if (wide) minOf(maxHeight, maxWidth * 0.5f) else minOf(maxWidth, maxHeight)
        val content: @Composable () -> Unit = {
            Box(Modifier.size(rose), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f * 0.92f
                    val c = center
                    drawCircle(p.faint, r, c, style = Stroke(r * 0.04f))
                    for (i in 0 until 36) {
                        val a = Math.toRadians(i * 10.0 - 90)
                        val major = i % 9 == 0
                        val r0 = r * if (major) 0.82f else 0.88f
                        drawLine(
                            p.fg.copy(alpha = if (major) 0.7f else 0.25f),
                            Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                            Offset(c.x + r * 0.95f * cos(a).toFloat(), c.y + r * 0.95f * sin(a).toFloat()),
                            r * 0.02f,
                        )
                    }
                    val ls = letter.copy(fontSize = letter.fontSize * (r / 60f).coerceIn(0.7f, 1.8f))
                    listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (n, deg) ->
                        val a = Math.toRadians(deg - 90.0)
                        val m = measurer.measure(n, ls.copy(color = if (n == "N") p.accent else ls.color))
                        val px = c.x + r * 0.66f * cos(a).toFloat() - m.size.width / 2f
                        val py = c.y + r * 0.66f * sin(a).toFloat() - m.size.height / 2f
                        drawText(m, topLeft = Offset(px, py))
                    }
                    // Strzałka: wiatr „z” kierunku windDir, więc grot wskazuje windDir + 180
                    rotate(w.windDir + 180f, c) {
                        val tip = Offset(c.x, c.y - r * 0.8f)
                        val tail = Offset(c.x, c.y + r * 0.62f)
                        drawLine(p.accent.copy(alpha = 0.55f), tail, tip, r * 0.035f, StrokeCap.Round)
                        val head = Path().apply {
                            moveTo(tip.x, tip.y)
                            lineTo(tip.x - r * 0.12f, tip.y + r * 0.2f)
                            lineTo(tip.x + r * 0.12f, tip.y + r * 0.2f)
                            close()
                        }
                        drawPath(head, p.accent)
                    }
                    drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.36f, c)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    T(speed, p.hero(rose.value * 0.2f, FontWeight.SemiBold).copy(textAlign = TextAlign.Center))
                    T(unit, p.text(rose.value * 0.075f, FontWeight.Normal, 0.75f).copy(textAlign = TextAlign.Center))
                }
            }
        }
        val fs = (maxHeight.value * 0.12f).coerceIn(9f, 16f)
        if (wide) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                content()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    T(beaufort, p.text(fs * 1.15f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                    T("Z kierunku ${WeatherUnits.compass(w.windDir)} (${w.windDir}°)", p.text(fs, FontWeight.Normal, 0.8f).copy(textAlign = TextAlign.Start))
                    T("Porywy $gusts $unit", p.text(fs, FontWeight.Normal, 0.8f, color = if (w.gusts >= 60) Color(0xFFFFB347) else p.fg).copy(textAlign = TextAlign.Start))
                    if (st.showDetails) T(w.place, p.text(fs * 0.85f, FontWeight.Normal, 0.6f).copy(textAlign = TextAlign.Start))
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
            if (st.showDetails) {
                T(
                    "↑ $gusts",
                    p.text((rose.value * 0.07f).coerceIn(8f, 13f), FontWeight.Medium, 0.75f),
                    Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

/** Opis siły wiatru w skali Beauforta (km/h). */
internal fun beaufort(kmh: Double): String = when {
    kmh < 1 -> "Cisza"
    kmh < 6 -> "Powiew"
    kmh < 12 -> "Słaby wiatr"
    kmh < 20 -> "Łagodny wiatr"
    kmh < 29 -> "Umiarkowany wiatr"
    kmh < 39 -> "Dość silny wiatr"
    kmh < 50 -> "Silny wiatr"
    kmh < 62 -> "Bardzo silny wiatr"
    kmh < 75 -> "Wichura"
    kmh < 89 -> "Silna wichura"
    kmh < 103 -> "Gwałtowna wichura"
    kmh < 118 -> "Burza"
    else -> "Huragan"
}
