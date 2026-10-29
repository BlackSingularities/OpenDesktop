package com.opendesktop.launcher.ui.desktop

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.CalendarContract
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.GadgetKind
import com.opendesktop.launcher.data.GadgetStyle
import com.opendesktop.launcher.data.WeatherUnits
import com.opendesktop.launcher.domain.Moon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.time.format.TextStyle as JTextStyle

/** Kolory, cień, czcionka i jednostki wyliczone raz ze stylu widżetu. */
internal class GadgetPaint(val st: GadgetStyle) {
    val fg = Color(st.textColor)
    val accent = Color(st.accent)
    val shadow: Shadow? = if (st.textShadow) {
        Shadow(if (fg.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f), Offset(0f, 1.5f), 6f)
    } else null
    val weight = FontWeight(st.weight.coerceIn(100, 900))
    val family: FontFamily? = when (st.fontFamily) {
        1 -> FontFamily.Serif
        2 -> FontFamily.Monospace
        3 -> FontFamily.Cursive
        4 -> FontFamily(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL))
        else -> null
    }
    val hAlign: Alignment.Horizontal = when (st.align) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val boxAlign: Alignment = when (st.align) {
        0 -> Alignment.CenterStart
        2 -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val textAlign: TextAlign = when (st.align) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    /** Kolor linii podziału i nieaktywnych torów. */
    val faint = fg.copy(alpha = 0.18f)

    /** [size] w sp przed przemnożeniem przez skalę tekstu użytkownika. */
    fun text(size: Float, w: FontWeight = weight, alpha: Float = 1f, color: Color = fg, italic: Boolean = false) = TextStyle(
        color = color.copy(alpha = color.alpha * alpha),
        fontSize = (size * st.textScale).coerceAtLeast(6f).sp,
        fontWeight = w,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        fontFamily = family,
        shadow = shadow,
        textAlign = textAlign,
    )

    // ----- Jednostki pogody (dane zawsze metryczne) -----

    fun deg(c: Double): String {
        val v = WeatherUnits.temp(c, st.fahrenheit)
        return if (st.decimals) "%.1f°".format(v) else "${v.roundToInt()}°"
    }

    /** Kolor temperatury (wejście w °C); bez kolorowania – kolor tekstu. */
    fun tempColor(c: Double): Color = if (st.tempColors) temperatureColor(c) else fg

    fun wind(kmh: Double): String {
        val u = st.windUnit.coerceIn(0, 3)
        return "${WeatherUnits.wind(kmh, u).roundToInt()} ${WeatherUnits.windNames[u]}"
    }

    fun pressure(hpa: Double): String {
        val u = st.pressureUnit.coerceIn(0, 2)
        val v = WeatherUnits.pressure(hpa, u)
        return (if (u == 2) "%.2f".format(v) else "${v.roundToInt()}") + " " + WeatherUnits.pressureNames[u]
    }

    fun rain(mm: Double): String = if (st.fahrenheit) "%.2f in".format(mm / 25.4) else "%.1f mm".format(mm)

    fun iconTint(code: Int, isDay: Boolean): Color = when (st.iconStyle) {
        1 -> conditionColor(code, isDay)
        2 -> fg
        else -> accent
    }

    fun has(metric: String) = metric in st.metrics
}

/** Skala barw temperatury: fiolet przy mrozie, przez błękit, zieleń i żółć, do czerwieni w upale. */
private val tempStops = listOf(
    -20.0 to Color(0xFFB388FF),
    -8.0 to Color(0xFF7C9CFF),
    0.0 to Color(0xFF64C8FF),
    8.0 to Color(0xFF4DD0E1),
    14.0 to Color(0xFF8BE08F),
    20.0 to Color(0xFFFFE066),
    26.0 to Color(0xFFFFB347),
    33.0 to Color(0xFFFF6B5A),
    40.0 to Color(0xFFE53970),
)

fun temperatureColor(c: Double): Color {
    if (c <= tempStops.first().first) return tempStops.first().second
    if (c >= tempStops.last().first) return tempStops.last().second
    val i = tempStops.indexOfLast { it.first <= c }
    val (t0, c0) = tempStops[i]
    val (t1, c1) = tempStops[i + 1]
    return lerp(c0, c1, ((c - t0) / (t1 - t0)).toFloat())
}

/** Kolor ikony pogody dla trybu „kolorowe”. */
fun conditionColor(code: Int, isDay: Boolean): Color = when (code) {
    0, 1 -> if (isDay) Color(0xFFFFC94D) else Color(0xFFFFE9A8)
    2 -> if (isDay) Color(0xFFFFD97A) else Color(0xFFCFD8E3)
    3 -> Color(0xFFCFD8E3)
    45, 48 -> Color(0xFFB0BEC5)
    in 51..57 -> Color(0xFF8FD3FF)
    in 61..67, in 80..82 -> Color(0xFF4FB3FF)
    in 71..77, 85, 86 -> Color(0xFFE3F4FF)
    in 95..99 -> Color(0xFFFFB13D)
    else -> Color(0xFFCFD8E3)
}

/** Tło karty zależne od warunków i pory dnia. */
fun weatherBrush(code: Int, isDay: Boolean, alpha: Float): Brush {
    val (top, bottom) = when (code) {
        0, 1 -> if (isDay) 0xFF2F7FD8 to 0xFF7EC3F2 else 0xFF0A1633 to 0xFF253D72
        2 -> if (isDay) 0xFF4A7FB8 to 0xFF9BB9D6 else 0xFF121C33 to 0xFF34445E
        3, 45, 48 -> if (isDay) 0xFF5F7387 to 0xFF9AA8B6 else 0xFF1B232D to 0xFF3A4552
        in 51..67, in 80..82 -> if (isDay) 0xFF34495E to 0xFF5D7A93 else 0xFF101A26 to 0xFF2A3A4C
        in 71..77, 85, 86 -> if (isDay) 0xFF6F8FB0 to 0xFFB6CADD else 0xFF1E2B3D to 0xFF4A5D75
        in 95..99 -> if (isDay) 0xFF2B2845 to 0xFF5A4E73 else 0xFF141225 to 0xFF35304D
        else -> 0xFF34495E to 0xFF5D7A93
    }
    return Brush.verticalGradient(listOf(Color(top).copy(alpha = alpha), Color(bottom).copy(alpha = alpha)))
}

private fun backgroundBrush(st: GadgetStyle): Brush {
    val a = Color(st.background)
    val b = Color(st.background2)
    return when (st.gradient) {
        1 -> Brush.verticalGradient(listOf(a, b))
        2 -> Brush.horizontalGradient(listOf(a, b))
        3 -> Brush.linearGradient(listOf(a, b), start = Offset.Zero, end = Offset.Infinite)
        4 -> Brush.radialGradient(listOf(a, b))
        else -> SolidColor(a)
    }
}

@Composable
internal fun T(text: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(text, style = style, modifier = modifier, maxLines = maxLines, softWrap = maxLines > 1, overflow = TextOverflow.Ellipsis)
}

/** Bieżący czas odświeżany co sekundę albo na początku każdej minuty. */
@Composable
internal fun rememberNow(everySecond: Boolean): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(everySecond) {
        while (true) {
            now = LocalDateTime.now()
            val ms = System.currentTimeMillis()
            delay(if (everySecond) 1000L - ms % 1000L else 60_000L - ms % 60_000L)
        }
    }
    return now
}

private fun batteryState(ctx: Context): Pair<Int, Boolean> = runCatching {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1 to false
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    (if (level < 0) -1 else level * 100 / scale) to charging
}.getOrDefault(-1 to false)

@Composable
private fun rememberBattery(): Pair<Int, Boolean> {
    val ctx = LocalContext.current
    val state by produceState(batteryState(ctx)) {
        while (true) {
            delay(20_000L)
            value = batteryState(ctx)
        }
    }
    return state
}

internal fun cap(s: String, locale: Locale) = s.replaceFirstChar { it.titlecase(locale) }

/** Akcja po stuknięciu – otwiera aplikację systemową pasującą do widżetu. */
fun openGadgetTarget(ctx: Context, kind: GadgetKind?) {
    val intent = when (kind) {
        GadgetKind.DigitalClock, GadgetKind.AnalogClock, GadgetKind.WorldClock, GadgetKind.ClockWeather ->
            Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
        GadgetKind.Calendar, GadgetKind.DateCard, GadgetKind.Countdown, GadgetKind.Progress ->
            Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
        GadgetKind.Battery -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        GadgetKind.Storage -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        else -> null
    } ?: return
    runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/**
 * Wbudowany widżet: karta (tło, rogi, obrys, cień) z zawartością zależną od rodzaju.
 * Treść skaluje się do rozmiaru kafelka.
 */
@Composable
fun GadgetWidget(kind: GadgetKind?, style: GadgetStyle, modifier: Modifier) {
    val shape = RoundedCornerShape(style.radius.coerceIn(0, 60).dp)
    val p = remember(style) { GadgetPaint(style) }
    // Tło pogodowe potrzebuje danych – to samo zapytanie co treść widżetu (wspólna pamięć podręczna)
    val weather = if (kind?.isWeather == true && style.weatherBackground) rememberWeather(style.city)?.getOrNull() else null
    val brush = if (weather != null) {
        weatherBrush(weather.code, weather.isDay, ((style.background ushr 24) and 0xFF) / 255f)
    } else backgroundBrush(style)
    val borderColor = if (style.borderColor != 0L) Color(style.borderColor) else p.fg.copy(alpha = 0.22f)
    Box(modifier.padding(4.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (style.shadow > 0.01f) Modifier.shadow((style.shadow * 18f).dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                    else Modifier
                )
                .clip(shape)
                .background(brush)
                .then(if (style.border) Modifier.border(style.borderWidth.coerceIn(0.5f, 6f).dp, borderColor, shape) else Modifier)
                .padding(style.padding.coerceIn(0, 40).dp),
            contentAlignment = p.boxAlign,
        ) {
            when (kind) {
                GadgetKind.DigitalClock -> DigitalClock(p)
                GadgetKind.AnalogClock -> AnalogClock(p)
                GadgetKind.ClockWeather -> ClockWeather(p)
                GadgetKind.Weather -> WeatherNowView(p)
                GadgetKind.Forecast -> Forecast(p)
                GadgetKind.HourlyForecast -> HourlyForecast(p)
                GadgetKind.WeatherFull -> WeatherFull(p)
                GadgetKind.WeatherDetails -> WeatherDetails(p)
                GadgetKind.SunArc -> SunArc(p)
                GadgetKind.Calendar -> MonthCalendar(p)
                GadgetKind.DateCard -> DateCard(p)
                GadgetKind.Battery -> BatteryView(p)
                GadgetKind.WorldClock -> WorldClock(p)
                GadgetKind.Countdown -> Countdown(p)
                GadgetKind.Note -> Note(p)
                GadgetKind.Greeting -> Greeting(p)
                GadgetKind.Progress -> TimeProgress(p)
                GadgetKind.Proverb -> Proverb(p)
                GadgetKind.Storage -> StorageView(p)
                GadgetKind.MoonPhase -> MoonPhaseView(p)
                GadgetKind.YearDots -> YearDots(p)
                null -> Unit
            }
        }
    }
}

// ---------- Zegary ----------

internal fun timeText(now: LocalDateTime, st: GadgetStyle, locale: Locale): Pair<String, String> {
    val pattern = (if (st.use24h) "HH:mm" else "h:mm") + if (st.showSeconds) ":ss" else ""
    val ampm = if (st.use24h) "" else now.format(DateTimeFormatter.ofPattern("a", Locale.ENGLISH))
    return now.format(DateTimeFormatter.ofPattern(pattern, locale)) to ampm
}

@Composable
private fun DigitalClock(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(st.showSeconds)
    val battery = if (st.showBattery) rememberBattery().first else -1
    val locale = Locale.getDefault()
    val (time, ampm) = timeText(now, st, locale)
    val date = cap(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), locale)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val chars = time.length + if (ampm.isNotEmpty()) 1.2f else 0f
        val big = minOf(maxHeight.value * (if (st.showDate) 0.52f else 0.72f), maxWidth.value / (chars * 0.58f)).coerceAtLeast(14f)
        val small = (big * 0.2f).coerceIn(11f, 22f)
        Column(horizontalAlignment = p.hAlign) {
            Row(verticalAlignment = Alignment.Bottom) {
                T(time, p.text(big).copy(letterSpacing = (-1).sp))
                if (ampm.isNotEmpty()) T(" $ampm", p.text(big * 0.28f, FontWeight.Medium, 0.85f), Modifier.padding(bottom = (big * 0.18f).dp))
            }
            if (st.showDate || battery >= 0) {
                val line = listOfNotNull(date.takeIf { st.showDate }, if (battery >= 0) "$battery%" else null).joinToString("  ·  ")
                T(line, p.text(small, FontWeight.Normal, 0.9f))
            }
        }
    }
}

@Composable
private fun AnalogClock(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(st.showSeconds)
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2f
        val c = center
        val fg = p.fg
        fun at(angleDeg: Float, radius: Float): Offset {
            val a = Math.toRadians(angleDeg.toDouble() - 90.0)
            return Offset(c.x + radius * cos(a).toFloat(), c.y + radius * sin(a).toFloat())
        }
        // Tarcza: 0 = klasyczna z kreskami, 1 = kropki, 2 = sama obwódka
        when (st.mode) {
            0 -> for (i in 0 until 60) {
                val major = i % 5 == 0
                drawLine(
                    fg.copy(alpha = if (major) 0.9f else 0.35f),
                    at(i * 6f, r * (if (major) 0.84f else 0.9f)),
                    at(i * 6f, r * 0.96f),
                    strokeWidth = r * (if (major) 0.035f else 0.015f),
                    cap = StrokeCap.Round,
                )
            }
            1 -> for (i in 0 until 12) drawCircle(fg.copy(alpha = if (i % 3 == 0) 0.95f else 0.55f), r * (if (i % 3 == 0) 0.045f else 0.03f), at(i * 30f, r * 0.88f))
            else -> drawCircle(fg.copy(alpha = 0.6f), r * 0.96f, c, style = Stroke(r * 0.03f))
        }
        if (st.showDetails) {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = fg.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = r * 0.17f * st.textScale
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (st.weight >= 500) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                if (st.textShadow) setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.argb(170, 0, 0, 0))
            }
            for (h in 1..12) {
                val pos = at(h * 30f, r * 0.68f)
                drawContext.canvas.nativeCanvas.drawText("$h", pos.x, pos.y - (paint.ascent() + paint.descent()) / 2f, paint)
            }
        }
        val h = now.hour % 12 + now.minute / 60f
        val m = now.minute + now.second / 60f
        val handShadow = Color.Black.copy(alpha = if (st.textShadow) 0.35f else 0f)
        listOf(
            Triple(h * 30f, r * 0.5f, r * 0.07f),
            Triple(m * 6f, r * 0.76f, r * 0.045f),
        ).forEach { (a, len, w) ->
            if (handShadow.alpha > 0f) drawLine(handShadow, c + Offset(0f, 2f), at(a, len) + Offset(0f, 2f), w, StrokeCap.Round)
            drawLine(fg, c, at(a, len), w, StrokeCap.Round)
        }
        if (st.showSeconds) {
            drawLine(p.accent, at(now.second * 6f + 180f, r * 0.15f), at(now.second * 6f, r * 0.86f), r * 0.02f, StrokeCap.Round)
        }
        drawCircle(p.accent, r * 0.06f, c)
    }
}

// ---------- Kalendarz ----------

@Composable
private fun MonthCalendar(p: GadgetPaint) {
    val st = p.st
    val today = rememberNow(false).toLocalDate()
    val locale = Locale.getDefault()
    val ym = YearMonth.from(today)
    val first = ym.atDay(1).dayOfWeek
    val offset = if (st.mondayFirst) first.value - 1 else first.value % 7
    val cells = offset + ym.lengthOfMonth()
    val rows = (cells + 6) / 7
    val order = if (st.mondayFirst) DayOfWeek.values().toList() else listOf(DayOfWeek.SUNDAY) + DayOfWeek.values().toList().dropLast(1)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cellH = maxHeight.value / (rows + 2.2f)
        val fs = minOf(cellH * 0.5f, maxWidth.value / 7f * 0.42f).coerceAtLeast(8f)
        Column(Modifier.fillMaxSize()) {
            T(
                cap(today.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)), locale),
                p.text(fs * 1.25f, FontWeight.Bold, color = p.accent),
                Modifier.fillMaxWidth().padding(bottom = 2.dp),
            )
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                order.forEach { d ->
                    T(
                        d.getDisplayName(JTextStyle.NARROW_STANDALONE, locale).uppercase(locale),
                        p.text(fs * 0.85f, FontWeight.SemiBold, 0.55f).copy(textAlign = TextAlign.Center),
                        Modifier.weight(1f),
                    )
                }
            }
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (day in 1..ym.lengthOfMonth()) {
                                val isToday = day == today.dayOfMonth
                                val dow = ym.atDay(day).dayOfWeek
                                val weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
                                if (isToday) {
                                    Box(Modifier.size((cellH * 0.95f).dp).clip(CircleShape).background(p.accent))
                                }
                                T(
                                    "$day",
                                    p.text(
                                        fs,
                                        if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        if (weekend && !isToday) 0.6f else 1f,
                                        color = if (isToday) (if (p.accent.luminance() > 0.55f) Color.Black else Color.White) else p.fg,
                                    ).copy(textAlign = TextAlign.Center, shadow = if (isToday) null else p.shadow),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCard(p: GadgetPaint) {
    val today = rememberNow(false).toLocalDate()
    val locale = Locale.getDefault()
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val big = minOf(maxHeight.value * 0.48f, maxWidth.value * 0.55f).coerceAtLeast(14f)
        Column(horizontalAlignment = p.hAlign) {
            T(cap(today.dayOfWeek.getDisplayName(JTextStyle.FULL_STANDALONE, locale), locale), p.text(big * 0.24f, FontWeight.Bold, color = p.accent))
            T("${today.dayOfMonth}", p.text(big).copy(lineHeight = (big * 1.05f * p.st.textScale).sp))
            T(
                cap(today.format(DateTimeFormatter.ofPattern(if (p.st.showDetails) "LLLL yyyy" else "LLLL", locale)), locale),
                p.text(big * 0.2f, FontWeight.Normal, 0.85f),
            )
        }
    }
}

// ---------- Bateria ----------

@Composable
private fun BatteryView(p: GadgetPaint) {
    val (level, charging) = rememberBattery()
    val color = when {
        level in 0..15 && !charging -> Color(0xFFFF5252)
        charging -> Color(0xFF69F0AE)
        else -> p.accent
    }
    val frac = (level.coerceAtLeast(0) / 100f)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val small = (minOf(maxWidth.value, maxHeight.value) * 0.13f).coerceIn(10f, 18f)
        if (p.st.mode == 1) {
            // Pasek poziomy
            Column(Modifier.fillMaxWidth(), horizontalAlignment = p.hAlign) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (charging) Icon(Icons.Rounded.Bolt, null, tint = color, modifier = Modifier.size((small * 2f).dp))
                    T(if (level >= 0) "$level%" else "—", p.text(small * 2.4f))
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height((small * 0.9f).dp).clip(RoundedCornerShape(50)).background(p.fg.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
                }
                if (p.st.showDetails) T(if (charging) "Ładowanie" else "Na baterii", p.text(small, FontWeight.Normal, 0.75f), Modifier.padding(top = 4.dp))
            }
        } else {
            val ring = minOf(maxWidth, maxHeight)
            Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = size.minDimension * 0.09f
                    val inset = sw / 2f
                    val arcSize = Size(size.minDimension - sw, size.minDimension - sw)
                    val tl = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f).let { if (inset > 0) it else it }
                    drawArc(p.fg.copy(alpha = 0.18f), 0f, 360f, false, tl, arcSize, style = Stroke(sw))
                    drawArc(color, -90f, 360f * frac, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (charging) Icon(Icons.Rounded.Bolt, null, tint = color, modifier = Modifier.size((ring.value * 0.16f).dp))
                    T(if (level >= 0) "$level%" else "—", p.text(ring.value * 0.2f).copy(textAlign = TextAlign.Center))
                    if (p.st.showDetails && !charging) T("bateria", p.text(ring.value * 0.08f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Center))
                }
            }
        }
    }
}

// ---------- Zegary świata ----------

/** Popularne strefy z polskimi nazwami miast (wybór w ustawieniach widżetu). */
val WorldZones = listOf(
    "Europe/London" to "Londyn",
    "Europe/Lisbon" to "Lizbona",
    "Europe/Paris" to "Paryż",
    "Europe/Berlin" to "Berlin",
    "Europe/Kyiv" to "Kijów",
    "Europe/Istanbul" to "Stambuł",
    "Asia/Dubai" to "Dubaj",
    "Asia/Kolkata" to "Delhi",
    "Asia/Bangkok" to "Bangkok",
    "Asia/Shanghai" to "Pekin",
    "Asia/Tokyo" to "Tokio",
    "Australia/Sydney" to "Sydney",
    "Pacific/Auckland" to "Auckland",
    "America/Sao_Paulo" to "São Paulo",
    "America/New_York" to "Nowy Jork",
    "America/Chicago" to "Chicago",
    "America/Denver" to "Denver",
    "America/Los_Angeles" to "Los Angeles",
    "Pacific/Honolulu" to "Honolulu",
)

fun zoneName(id: String): String = WorldZones.firstOrNull { it.first == id }?.second ?: id.substringAfterLast('/').replace('_', ' ')

@Composable
private fun WorldClock(p: GadgetPaint) {
    val st = p.st
    rememberNow(st.showSeconds)
    val zones = st.zones.mapNotNull { id -> runCatching { ZoneId.of(id) }.getOrNull()?.let { id to it } }.take(6)
    if (zones.isEmpty()) {
        T("Wybierz miasta w ustawieniach", p.text(13f, FontWeight.Normal, 0.8f))
        return
    }
    val here = ZonedDateTime.now()
    val locale = Locale.getDefault()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = maxWidth.value / zones.size > 64f && st.mode == 0
        val pattern = (if (st.use24h) "HH:mm" else "h:mm a") + if (st.showSeconds) ":ss".let { if (st.use24h) it else "" } else ""
        val fmt = DateTimeFormatter.ofPattern(pattern, if (st.use24h) locale else Locale.ENGLISH)
        fun diff(z: ZonedDateTime): String {
            val h = (z.offset.totalSeconds - here.offset.totalSeconds) / 3600f
            val day = ChronoUnit.DAYS.between(here.toLocalDate(), z.toLocalDate())
            val hs = if (h == 0f) "teraz" else (if (h > 0) "+" else "") + (if (h % 1f == 0f) h.toInt().toString() else "%.1f".format(h)) + " h"
            return hs + when { day > 0 -> ", jutro"; day < 0 -> ", wczoraj"; else -> "" }
        }
        if (horizontal) {
            val big = minOf(maxHeight.value * 0.32f, maxWidth.value / zones.size / (pattern.length * 0.62f)).coerceAtLeast(10f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                zones.forEach { (id, zone) ->
                    val z = ZonedDateTime.now(zone)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        T(zoneName(id), p.text(big * 0.38f, FontWeight.SemiBold, color = p.accent).copy(textAlign = TextAlign.Center))
                        T(z.format(fmt), p.text(big).copy(textAlign = TextAlign.Center))
                        if (st.showDetails) T(diff(z), p.text(big * 0.32f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Center))
                    }
                }
            }
        } else {
            val rowH = maxHeight.value / zones.size
            val fs = (rowH * 0.5f).coerceIn(9f, 26f)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                zones.forEach { (id, zone) ->
                    val z = ZonedDateTime.now(zone)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(zoneName(id), p.text(fs * 0.7f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                            if (st.showDetails) T(diff(z), p.text(fs * 0.5f, FontWeight.Normal, 0.6f).copy(textAlign = TextAlign.Start))
                        }
                        T(z.format(fmt), p.text(fs).copy(textAlign = TextAlign.End))
                    }
                }
            }
        }
    }
}

// ---------- Odliczanie ----------

@Composable
private fun Countdown(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(st.mode == 1 && st.showSeconds)
    val target = runCatching { LocalDate.parse(st.target.trim()) }.getOrNull()
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val big = minOf(maxHeight.value * 0.45f, maxWidth.value * 0.3f).coerceAtLeast(14f)
        val small = (big * 0.26f).coerceIn(10f, 20f)
        Column(horizontalAlignment = p.hAlign) {
            if (target == null) {
                T(st.label.ifBlank { "Odliczanie" }, p.text(small * 1.2f, FontWeight.SemiBold))
                T("Ustaw datę w „Dostosuj”", p.text(small, FontWeight.Normal, 0.7f))
                return@Column
            }
            val days = ChronoUnit.DAYS.between(now.toLocalDate(), target)
            if (st.label.isNotBlank()) T(st.label, p.text(small * 1.1f, FontWeight.SemiBold, color = p.accent))
            when {
                days == 0L -> T("To dziś!", p.text(big * 0.8f, FontWeight.Bold))
                st.mode == 1 && days > 0 -> {
                    val d = Duration.between(now, target.atStartOfDay())
                    val parts = buildString {
                        append("${d.toDays()} d ${d.toHours() % 24} h ${d.toMinutes() % 60} min")
                        if (st.showSeconds) append(" ${d.seconds % 60} s")
                    }
                    T(parts, p.text(big * 0.42f))
                }
                else -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        T("${kotlin.math.abs(days)}", p.text(big))
                        T(" " + dayWord(kotlin.math.abs(days)), p.text(big * 0.3f, FontWeight.Normal, 0.85f), Modifier.padding(bottom = (big * 0.15f).dp))
                    }
                    T(if (days > 0) "zostało" else "minęło", p.text(small, FontWeight.Normal, 0.7f))
                }
            }
            if (st.showDetails) {
                T(target.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())), p.text(small * 0.85f, FontWeight.Normal, 0.6f))
            }
        }
    }
}

private fun dayWord(n: Long) = if (n == 1L) "dzień" else "dni"

// ---------- Notatka, powitanie, przysłowie ----------

@Composable
private fun Note(p: GadgetPaint) {
    val st = p.st
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = if (st.mode == 1) p.boxAlign else Alignment.TopStart) {
        val body = st.text.ifBlank { "Stuknij i przytrzymaj, wybierz „Dostosuj”, żeby wpisać notatkę." }
        // Krótsza notatka = większe litery
        val area = maxWidth.value * maxHeight.value
        val fs = kotlin.math.sqrt(area / (body.length.coerceAtLeast(8) * 2.2f)).coerceIn(10f, 34f)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = p.hAlign) {
            if (st.label.isNotBlank()) T(st.label, p.text(fs * 0.8f, FontWeight.Bold, color = p.accent), Modifier.fillMaxWidth().padding(bottom = 4.dp))
            T(body, p.text(fs, alpha = if (st.text.isBlank()) 0.6f else 1f), Modifier.fillMaxWidth(), maxLines = 20)
        }
    }
}

@Composable
private fun Greeting(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(false)
    val locale = Locale.getDefault()
    val hello = when (now.hour) {
        in 5..10 -> "Dzień dobry"
        in 11..17 -> "Miłego dnia"
        in 18..21 -> "Dobry wieczór"
        else -> "Dobranoc"
    }
    val title = if (st.label.isBlank()) hello else "$hello, ${st.label}"
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val big = minOf(maxHeight.value * (if (st.showDate) 0.42f else 0.55f), maxWidth.value / (title.length * 0.55f)).coerceAtLeast(12f)
        Column(horizontalAlignment = p.hAlign) {
            T(title, p.text(big))
            if (st.showDate) T(cap(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), locale), p.text(big * 0.42f, FontWeight.Normal, 0.85f))
        }
    }
}

private val proverbs = listOf(
    "Gdzie kucharek sześć, tam nie ma co jeść." to "przysłowie polskie",
    "Kto rano wstaje, temu Pan Bóg daje." to "przysłowie polskie",
    "Nie chwal dnia przed zachodem słońca." to "przysłowie polskie",
    "Darowanemu koniowi nie zagląda się w zęby." to "przysłowie polskie",
    "Kropla drąży skałę." to "przysłowie łacińskie",
    "Lepszy wróbel w garści niż gołąb na dachu." to "przysłowie polskie",
    "Bez pracy nie ma kołaczy." to "przysłowie polskie",
    "Apetyt rośnie w miarę jedzenia." to "przysłowie",
    "Co się odwlecze, to nie uciecze." to "przysłowie polskie",
    "Fortuna kołem się toczy." to "przysłowie polskie",
    "Prawdziwych przyjaciół poznaje się w biedzie." to "przysłowie",
    "Ziarnko do ziarnka, a zbierze się miarka." to "przysłowie polskie",
    "Śpiesz się powoli." to "przysłowie łacińskie",
    "Kto pyta, nie błądzi." to "przysłowie polskie",
    "Czego Jaś się nie nauczy, tego Jan nie będzie umiał." to "przysłowie polskie",
    "Mądry Polak po szkodzie." to "przysłowie polskie",
    "Jak sobie pościelesz, tak się wyśpisz." to "przysłowie polskie",
    "Wszystko dobre, co się dobrze kończy." to "przysłowie",
    "Nie od razu Kraków zbudowano." to "przysłowie polskie",
    "Kto mieczem wojuje, od miecza ginie." to "przysłowie",
    "Cicha woda brzegi rwie." to "przysłowie polskie",
    "Nie ma tego złego, co by na dobre nie wyszło." to "przysłowie polskie",
    "Tonący brzytwy się chwyta." to "przysłowie polskie",
    "Wilk syty i owca cała." to "przysłowie polskie",
    "Gdzie dwóch się bije, tam trzeci korzysta." to "przysłowie polskie",
    "Nosił wilk razy kilka, ponieśli i wilka." to "przysłowie polskie",
    "Kwiecień plecień, bo przeplata trochę zimy, trochę lata." to "przysłowie ludowe",
    "Prawda w oczy kole." to "przysłowie polskie",
    "Jaka praca, taka płaca." to "przysłowie polskie",
    "Pierwsze koty za płoty." to "przysłowie polskie",
    "Dzieci i ryby głosu nie mają." to "przysłowie polskie",
)

@Composable
private fun Proverb(p: GadgetPaint) {
    val today = rememberNow(false).toLocalDate()
    val (text, source) = proverbs[(today.toEpochDay() % proverbs.size).toInt().let { if (it < 0) it + proverbs.size else it }]
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val fs = kotlin.math.sqrt(maxWidth.value * maxHeight.value / (text.length * 2.6f)).coerceIn(11f, 30f)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = p.hAlign) {
            T("„$text”", p.text(fs, italic = p.st.mode == 0), Modifier.fillMaxWidth(), maxLines = 5)
            if (p.st.showDetails) T("— $source", p.text(fs * 0.6f, FontWeight.Normal, 0.7f, color = p.accent), Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

// ---------- Postęp czasu ----------

@Composable
private fun TimeProgress(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(false)
    val locale = Locale.getDefault()
    val (title, frac) = when (st.mode) {
        0 -> "Dzień" to (now.toLocalTime().toSecondOfDay() / 86_400f)
        1 -> {
            val start = now.toLocalDate().with(if (st.mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY).let {
                if (it.isAfter(now.toLocalDate())) it.minusWeeks(1) else it
            }.atStartOfDay()
            "Tydzień" to (Duration.between(start, now).seconds / (7 * 86_400f))
        }
        2 -> cap(now.format(DateTimeFormatter.ofPattern("LLLL", locale)), locale) to
            ((now.dayOfMonth - 1 + now.toLocalTime().toSecondOfDay() / 86_400f) / now.toLocalDate().lengthOfMonth())
        else -> "Rok ${now.year}" to ((now.dayOfYear - 1 + now.toLocalTime().toSecondOfDay() / 86_400f) / now.toLocalDate().lengthOfYear())
    }
    val pct = (frac * 100f).coerceIn(0f, 100f)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val fs = (maxHeight.value * 0.3f).coerceIn(10f, 22f)
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                T(title, p.text(fs, FontWeight.SemiBold).copy(textAlign = TextAlign.Start), Modifier.weight(1f))
                T(if (st.showDetails) "%.1f%%".format(pct) else "${pct.toInt()}%", p.text(fs).copy(textAlign = TextAlign.End))
            }
            Spacer(Modifier.height((fs * 0.35f).dp))
            Box(Modifier.fillMaxWidth().height((fs * 0.55f).dp).clip(RoundedCornerShape(50)).background(p.fg.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().clip(RoundedCornerShape(50)).background(p.accent))
            }
        }
    }
}

// ---------- Pamięć ----------

private data class Usage(val used: Long, val total: Long) {
    val frac get() = if (total <= 0) 0f else used.toFloat() / total
}

private fun gb(b: Long) = "%.1f GB".format(b / 1_073_741_824.0)

@Composable
private fun StorageView(p: GadgetPaint) {
    val ctx = LocalContext.current
    val data by produceState<Pair<Usage, Usage>?>(null) {
        while (true) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val fs = StatFs(Environment.getDataDirectory().path)
                    val disk = Usage(fs.totalBytes - fs.availableBytes, fs.totalBytes)
                    val mi = ActivityManager.MemoryInfo()
                    (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
                    disk to Usage(mi.totalMem - mi.availMem, mi.totalMem)
                }.getOrNull()
            }
            delay(60_000L)
        }
    }
    val d = data ?: return
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val fs = (maxHeight.value * 0.13f).coerceIn(9f, 18f)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy((fs * 0.6f).dp)) {
            listOf("Pamięć wewnętrzna" to d.first, "RAM" to d.second).forEach { (name, u) ->
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        T(name, p.text(fs, FontWeight.SemiBold).copy(textAlign = TextAlign.Start), Modifier.weight(1f))
                        T("${(u.frac * 100).toInt()}%", p.text(fs, FontWeight.SemiBold, color = p.accent).copy(textAlign = TextAlign.End))
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.fillMaxWidth().height((fs * 0.5f).dp).clip(RoundedCornerShape(50)).background(p.fg.copy(alpha = 0.18f))) {
                        Box(
                            Modifier.fillMaxWidth(u.frac.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50))
                                .background(if (u.frac > 0.9f) Color(0xFFFF5252) else p.accent)
                        )
                    }
                    if (p.st.showDetails) T("${gb(u.used)} z ${gb(u.total)}", p.text(fs * 0.8f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Start))
                }
            }
        }
    }
}

// ---------- Księżyc ----------

@Composable
private fun MoonPhaseView(p: GadgetPaint) {
    val st = p.st
    val today = rememberNow(false).toLocalDate()
    val phase = Moon.phase(today)
    val lit = Moon.illumination(phase)
    val name = Moon.name(phase)
    val toFull = Moon.daysUntil(phase, 0.5)
    val toNew = Moon.daysUntil(phase, 0.0)
    val litColor = when (st.mode) {
        1 -> p.accent
        2 -> p.fg
        else -> Color(0xFFF4EEDC)
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val wide = maxWidth > maxHeight * 1.35f
        val moonSize = if (wide) minOf(maxHeight, maxWidth * 0.45f) else minOf(maxWidth, maxHeight * 0.62f)
        val fs = (moonSize.value * 0.13f).coerceIn(9f, 18f)
        val texts: @Composable () -> Unit = {
            T(name, p.text(fs * 1.1f, FontWeight.SemiBold))
            T("Oświetlenie $lit%", p.text(fs * 0.85f, FontWeight.Normal, 0.75f))
            if (st.showDetails) {
                T(
                    if (toFull == 0) "Pełnia dziś" else "Pełnia za $toFull ${dayWord(toFull.toLong())}",
                    p.text(fs * 0.8f, FontWeight.Normal, 0.65f, color = p.accent),
                )
                if (wide) T(if (toNew == 0) "Nów dziś" else "Nów za $toNew ${dayWord(toNew.toLong())}", p.text(fs * 0.8f, FontWeight.Normal, 0.6f))
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                MoonDisk(phase, litColor, p.faint, st.southern, Modifier.size(moonSize))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { texts() }
            }
        } else {
            Column(horizontalAlignment = p.hAlign, verticalArrangement = Arrangement.Center) {
                MoonDisk(phase, litColor, p.faint, st.southern, Modifier.size(moonSize).align(p.hAlign))
                Spacer(Modifier.height(4.dp))
                texts()
            }
        }
    }
}

/** Tarcza Księżyca: ciemny dysk, oświetlona część z półokręgu i elipsy terminatora. */
@Composable
private fun MoonDisk(phase: Double, lit: Color, dark: Color, southern: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f * 0.94f
        val c = center
        drawCircle(dark, r, c)
        val k = cos(2 * PI * phase).toFloat()
        val rx = abs(k) * r
        val waxing = phase < 0.5
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            arcTo(Rect(c.x - r, c.y - r, c.x + r, c.y + r), -90f, if (waxing) 180f else -180f, false)
            arcTo(Rect(c.x - rx, c.y - r, c.x + rx, c.y + r), 90f, if (waxing == (k > 0)) -180f else 180f, false)
            close()
        }
        scale(if (southern) -1f else 1f, 1f, c) {
            drawPath(path, lit)
            // Kilka „mórz”, widocznych tylko na oświetlonej części
            clipPath(path) {
                listOf(Triple(-0.28f, -0.22f, 0.22f), Triple(0.18f, -0.3f, 0.14f), Triple(0.05f, 0.18f, 0.2f), Triple(-0.35f, 0.3f, 0.1f), Triple(0.38f, 0.12f, 0.09f))
                    .forEach { (x, y, rr) -> drawCircle(Color.Black.copy(alpha = 0.08f), r * rr, Offset(c.x + x * r, c.y + y * r)) }
            }
        }
        drawCircle(lit.copy(alpha = 0.25f), r, c, style = Stroke(r * 0.025f))
    }
}

// ---------- Rok w kropkach ----------

@Composable
private fun YearDots(p: GadgetPaint) {
    val st = p.st
    val today = rememberNow(false).toLocalDate()
    val locale = Locale.getDefault()
    val month = st.mode == 1
    val total = if (month) today.lengthOfMonth() else today.lengthOfYear()
    val index = if (month) today.dayOfMonth else today.dayOfYear
    val pct = (index - 1) * 100 / total
    val title = st.label.ifBlank { if (month) cap(today.format(DateTimeFormatter.ofPattern("LLLL", locale)), locale) else "${today.year}" }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fs = (maxHeight.value * 0.11f).coerceIn(9f, 16f)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                T(title, p.text(fs * 1.1f, FontWeight.SemiBold, color = p.accent).copy(textAlign = TextAlign.Start), Modifier.weight(1f))
                if (st.showDetails) T("dzień $index z $total · $pct%", p.text(fs * 0.85f, FontWeight.Normal, 0.75f).copy(textAlign = TextAlign.End))
            }
            Spacer(Modifier.height((fs * 0.4f).dp))
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val aspect = size.width / size.height.coerceAtLeast(1f)
                val cols = ceil(sqrt(total * aspect)).toInt().coerceIn(1, total)
                val rows = ceil(total / cols.toFloat()).toInt()
                val cell = minOf(size.width / cols, size.height / rows)
                val ox = (size.width - cell * cols) / 2f
                val oy = (size.height - cell * rows) / 2f
                for (i in 0 until total) {
                    val cx = ox + (i % cols + 0.5f) * cell
                    val cy = oy + (i / cols + 0.5f) * cell
                    val day = i + 1
                    when {
                        day == index -> drawCircle(p.accent, cell * 0.42f, Offset(cx, cy))
                        day < index -> drawCircle(p.fg.copy(alpha = 0.85f), cell * 0.32f, Offset(cx, cy))
                        else -> drawCircle(p.fg.copy(alpha = 0.2f), cell * 0.32f, Offset(cx, cy))
                    }
                }
            }
        }
    }
}
