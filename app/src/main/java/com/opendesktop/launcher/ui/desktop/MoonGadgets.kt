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

// ---------- Księżyc ----------

@Composable
internal fun MoonPhaseView(p: GadgetPaint) {
    val st = p.st
    val today = rememberNow(false).toLocalDate()
    val phase = Moon.phase(today)
    val lit = Moon.illumination(phase)
    val name = Moon.name(phase)
    val toFull = Moon.daysUntil(phase, 0.5)
    val toNew = Moon.daysUntil(phase, 0.0)
    val litColor = moonLit(p)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val wide = maxWidth > maxHeight * 1.35f
        val moonSize = if (wide) minOf(maxHeight, maxWidth * 0.45f) else minOf(maxWidth, maxHeight * 0.62f)
        val fs = (moonSize.value * 0.13f).coerceIn(9f, 18f)
        val texts: @Composable () -> Unit = {
            T(name, p.hero(fs * 1.1f, FontWeight.SemiBold))
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
                MoonDisk(phase, litColor, p.faint, st.southern, Modifier.size(moonSize), st.realMoon)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { texts() }
            }
        } else {
            Column(horizontalAlignment = p.hAlign, verticalArrangement = Arrangement.Center) {
                MoonDisk(phase, litColor, p.faint, st.southern, Modifier.size(moonSize).align(p.hAlign), st.realMoon)
                Spacer(Modifier.height(4.dp))
                texts()
            }
        }
    }
}

/** Kolor oświetlonej części Księżyca: naturalny, akcent albo kolor tekstu. */
internal fun moonLit(p: GadgetPaint): Color = when (p.st.mode) {
    1 -> p.accent
    2 -> p.fg
    else -> Color(0xFFF4EEDC)
}

/** Sam Księżyc w okrągłej karcie; w szczegółach procent oświetlenia na dole. */
@Composable
internal fun MoonRoundView(p: GadgetPaint) {
    val st = p.st
    val phase = Moon.phase(rememberNow(false).toLocalDate())
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val s = minOf(maxWidth, maxHeight)
        if (st.showDetails) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MoonDisk(phase, moonLit(p), p.faint, st.southern, Modifier.size(s * 0.78f), st.realMoon)
                T("${Moon.illumination(phase)}%", p.hero((s.value * 0.09f).coerceIn(8f, 14f), FontWeight.Medium, 0.8f))
            }
        } else MoonDisk(phase, moonLit(p), p.faint, st.southern, Modifier.size(s * 0.96f), st.realMoon)
    }
}

/** Kształt oświetlonej części tarczy: półokrąg domknięty elipsą terminatora. */
internal fun litPath(phase: Double, c: Offset, r: Float): Path {
    val k = cos(2 * PI * phase).toFloat()
    val rx = abs(k) * r
    val waxing = phase < 0.5
    return Path().apply {
        moveTo(c.x, c.y - r)
        arcTo(Rect(c.x - r, c.y - r, c.x + r, c.y + r), -90f, if (waxing) 180f else -180f, false)
        arcTo(Rect(c.x - rx, c.y - r, c.x + rx, c.y + r), 90f, if (waxing == (k > 0)) -180f else 180f, false)
        close()
    }
}

/**
 * Tarcza Księżyca: ciemny dysk, oświetlona część z półokręgu i elipsy terminatora.
 * Z [real] zamiast rysowanej tarczy pokazuje zdjęcie Księżyca z wyciętym tłem.
 */
@Composable
internal fun MoonDisk(phase: Double, lit: Color, dark: Color, southern: Boolean, modifier: Modifier, real: Boolean = false) {
    if (real) {
        MoonPhoto(phase, southern, modifier)
        return
    }
    Canvas(modifier) {
        val r = size.minDimension / 2f * 0.94f
        val c = center
        drawCircle(dark, r, c)
        val path = litPath(phase, c, r)
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

/**
 * Prawdziwy Księżyc: zdjęcie pełni z wyciętym tłem, a nieoświetlona część przykryta
 * ciemną zasłoną o kryciu 95% – ledwie widoczna, ale widać, gdzie kończy się tarcza.
 */
@Composable
internal fun MoonPhoto(phase: Double, southern: Boolean, modifier: Modifier) {
    val photo = ImageBitmap.imageResource(R.drawable.moon_photo)
    Canvas(modifier) {
        val r = size.minDimension / 2f * 0.94f
        val c = center
        val d = (r * 2f).roundToInt()
        val path = litPath(phase, c, r)
        scale(if (southern) -1f else 1f, 1f, c) {
            drawImage(
                photo,
                dstOffset = IntOffset((c.x - r).roundToInt(), (c.y - r).roundToInt()),
                dstSize = IntSize(d, d),
            )
            clipPath(path, ClipOp.Difference) {
                drawCircle(Color(0xFF05070B).copy(alpha = 0.95f), r, c)
            }
        }
    }
}

// ---------- Rok w kropkach ----------

@Composable
internal fun YearDots(p: GadgetPaint) {
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
