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

// ---------- Zegary ----------

internal fun timeText(now: LocalDateTime, st: GadgetStyle, locale: Locale): Pair<String, String> {
    val pattern = (if (st.use24h) "HH:mm" else "h:mm") + if (st.showSeconds) ":ss" else ""
    val ampm = if (st.use24h) "" else now.format(DateTimeFormatter.ofPattern("a", Locale.ENGLISH))
    return now.format(DateTimeFormatter.ofPattern(pattern, locale)) to ampm
}

@Composable
internal fun DigitalClock(p: GadgetPaint) {
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
                T(time, p.hero(big).copy(letterSpacing = (-1).sp))
                if (ampm.isNotEmpty()) T(" $ampm", p.hero(big * 0.28f, FontWeight.Medium, 0.85f), Modifier.padding(bottom = (big * 0.18f).dp))
            }
            if (st.showDate || battery >= 0) {
                val line = listOfNotNull(date.takeIf { st.showDate }, if (battery >= 0) "$battery%" else null).joinToString("  ·  ")
                T(line, p.text(small, FontWeight.Normal, 0.9f), p.gap)
            }
        }
    }
}

@Composable
internal fun AnalogClock(p: GadgetPaint) {
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
internal fun MonthCalendar(p: GadgetPaint) {
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
internal fun DateCard(p: GadgetPaint) {
    val today = rememberNow(false).toLocalDate()
    val locale = Locale.getDefault()
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val big = minOf(maxHeight.value * 0.48f, maxWidth.value * 0.55f).coerceAtLeast(14f)
        Column(horizontalAlignment = p.hAlign) {
            T(cap(today.dayOfWeek.getDisplayName(JTextStyle.FULL_STANDALONE, locale), locale), p.text(big * 0.24f, FontWeight.Bold, color = p.accent))
            T("${today.dayOfMonth}", p.hero(big).copy(lineHeight = (big * 1.05f * p.st.textScale * p.st.timeScale).sp))
            T(
                cap(today.format(DateTimeFormatter.ofPattern(if (p.st.showDetails) "LLLL yyyy" else "LLLL", locale)), locale),
                p.text(big * 0.2f, FontWeight.Normal, 0.85f),
                p.gap,
            )
        }
    }
}

// ---------- Bateria ----------

@Composable
internal fun BatteryView(p: GadgetPaint) {
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
