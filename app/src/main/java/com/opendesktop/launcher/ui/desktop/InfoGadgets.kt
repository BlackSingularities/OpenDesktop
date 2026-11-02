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
internal fun WorldClock(p: GadgetPaint) {
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
                        T(z.format(fmt), p.hero(big).copy(textAlign = TextAlign.Center))
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
                        T(z.format(fmt), p.hero(fs).copy(textAlign = TextAlign.End))
                    }
                }
            }
        }
    }
}

// ---------- Odliczanie ----------

@Composable
internal fun Countdown(p: GadgetPaint) {
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
                days == 0L -> T("To dziś!", p.hero(big * 0.8f, FontWeight.Bold))
                st.mode == 1 && days > 0 -> {
                    val d = Duration.between(now, target.atStartOfDay())
                    val parts = buildString {
                        append("${d.toDays()} d ${d.toHours() % 24} h ${d.toMinutes() % 60} min")
                        if (st.showSeconds) append(" ${d.seconds % 60} s")
                    }
                    T(parts, p.hero(big * 0.42f))
                }
                else -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        T("${kotlin.math.abs(days)}", p.hero(big))
                        T(" " + dayWord(kotlin.math.abs(days)), p.hero(big * 0.3f, FontWeight.Normal, 0.85f), Modifier.padding(bottom = (big * 0.15f).dp))
                    }
                    T(if (days > 0) "zostało" else "minęło", p.text(small, FontWeight.Normal, 0.7f), p.gap)
                }
            }
            if (st.showDetails) {
                T(target.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())), p.text(small * 0.85f, FontWeight.Normal, 0.6f))
            }
        }
    }
}

internal fun dayWord(n: Long) = if (n == 1L) "dzień" else "dni"

// ---------- Notatka, powitanie, przysłowie ----------

@Composable
internal fun Note(p: GadgetPaint) {
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
internal fun Greeting(p: GadgetPaint) {
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

internal val proverbs = listOf(
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
internal fun Proverb(p: GadgetPaint) {
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
internal fun TimeProgress(p: GadgetPaint) {
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

internal data class Usage(val used: Long, val total: Long) {
    val frac get() = if (total <= 0) 0f else used.toFloat() / total
}

internal fun gb(b: Long) = "%.1f GB".format(b / 1_073_741_824.0)

@Composable
internal fun StorageView(p: GadgetPaint) {
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
