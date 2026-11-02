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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.R
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

/** Przesunięcie treści pod godziną – suwak „Odstęp pod godziną”. */
    val gap: Modifier
        get() = if (st.timeGap == 0) Modifier else Modifier.offset(y = st.timeGap.dp)

    /**
     * Główna wartość widżetu – godzina, liczba dni, nazwa fazy. Poza wspólną skalą tekstu
     * ma własny mnożnik [GadgetStyle.timeScale], żeby powiększyć samą godzinę.
     */
    fun hero(size: Float, w: FontWeight = weight, alpha: Float = 1f, color: Color = fg) = text(size * st.timeScale, w, alpha, color)

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
internal val tempStops = listOf(
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

internal fun backgroundBrush(st: GadgetStyle): Brush {
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

internal fun batteryState(ctx: Context): Pair<Int, Boolean> = runCatching {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1 to false
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    (if (level < 0) -1 else level * 100 / scale) to charging
}.getOrDefault(-1 to false)

@Composable
internal fun rememberBattery(): Pair<Int, Boolean> {
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
        GadgetKind.DigitalClock, GadgetKind.AnalogClock, GadgetKind.WorldClock, GadgetKind.ClockWeather,
        GadgetKind.FlipClock, GadgetKind.NextAlarm, GadgetKind.SkyClock ->
            Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
        GadgetKind.Calendar, GadgetKind.DateCard, GadgetKind.Countdown, GadgetKind.Progress,
        GadgetKind.WeekStrip, GadgetKind.Holidays, GadgetKind.Agenda, GadgetKind.YearDots ->
            Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
        GadgetKind.Battery, GadgetKind.DeviceStatus -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
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
    val shape = if (kind == GadgetKind.MoonRound || (kind == GadgetKind.Wind && style.mode == 0)) CircleShape else RoundedCornerShape(style.radius.coerceIn(0, 60).dp)
    val p = remember(style) { GadgetPaint(style) }
    // Tło pogodowe potrzebuje danych – to samo zapytanie co treść widżetu (wspólna pamięć podręczna)
    val weather = if (kind?.isWeather == true && style.weatherBackground) rememberWeather(style)?.getOrNull() else null
    val brush = if (weather != null) {
        weatherBrush(weather.code, weather.isDay, ((style.background ushr 24) and 0xFF) / 255f)
    } else backgroundBrush(style)
    val borderColor = if (style.borderColor != 0L) Color(style.borderColor) else p.fg.copy(alpha = 0.22f)
    Box(modifier.padding(4.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (style.shadow > 0.01f) Modifier.shadow((style.shadow * 18f).dp, shape, ambientColor = Color(style.shadowColor), spotColor = Color(style.shadowColor))
                    else Modifier
                )
                .clip(shape)
                .background(brush)
                .then(if (style.border) Modifier.border(style.borderWidth.coerceIn(0.5f, 6f).dp, borderColor, shape) else Modifier),
        ) {
          Box(
            Modifier
                .fillMaxSize()
                .contentShadow(style)
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
                GadgetKind.MoonRound -> MoonRoundView(p)
                GadgetKind.SkyClock -> SkyClock(p)
                GadgetKind.Steps -> StepsView(p)
                GadgetKind.WeatherChart -> WeatherChartView(p)
                GadgetKind.AirQuality -> AirQualityView(p)
                GadgetKind.RainNow -> RainNowView(p)
                GadgetKind.Wind -> WindView(p)
                GadgetKind.FlipClock -> FlipClockView(p)
                GadgetKind.WeekStrip -> WeekStripView(p)
                GadgetKind.Holidays -> HolidaysView(p)
                GadgetKind.Agenda -> AgendaView(p)
                GadgetKind.NextAlarm -> NextAlarmView(p)
                GadgetKind.DeviceStatus -> DeviceStatusView(p)
                null -> Unit
            }
          }
        }
        // Na okrągłej karcie róg wypada poza kołem – tam bez przycisku (pogoda i tak odświeża się sama)
        if (kind?.isWeather == true && style.showRefresh && shape != CircleShape) {
            RefreshButton(p, Modifier.align(Alignment.TopEnd).padding((style.radius.coerceIn(0, 60) * 0.18f + 5f).dp))
        }
    }
}

/**
 * Cień pod całą treścią widżetu – tekstem, ikonami, wykresami i wskazówkami – liczony z jej kształtu
 * (Android 12+). Na starszych zostaje klasyczny cień tekstu.
 */
internal fun Modifier.contentShadow(st: GadgetStyle): Modifier {
    if (st.contentShadow <= 0.01f || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return this
    return graphicsLayer {
        val color = Color(st.shadowColor).copy(alpha = st.contentShadow.coerceIn(0f, 1f)).toArgb()
        val off = st.shadowOffset.dp.toPx()
        val blur = st.shadowBlur.coerceIn(0f, 16f).dp.toPx().coerceAtLeast(0.5f)
        val shadow = android.graphics.RenderEffect.createColorFilterEffect(
            android.graphics.BlendModeColorFilter(color, android.graphics.BlendMode.SRC_IN),
            android.graphics.RenderEffect.createBlurEffect(
                blur, blur,
                android.graphics.RenderEffect.createOffsetEffect(off * 0.3f, off),
                android.graphics.Shader.TileMode.DECAL,
            ),
        )
        // Treść (efekt tożsamościowy) rysowana na swoim cieniu
        renderEffect = android.graphics.RenderEffect.createBlendModeEffect(
            shadow, android.graphics.RenderEffect.createOffsetEffect(0f, 0f), android.graphics.BlendMode.SRC_OVER,
        ).asComposeRenderEffect()
    }
}
