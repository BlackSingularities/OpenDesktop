package com.opendesktop.launcher.ui.desktop

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.opendesktop.launcher.domain.Holiday
import com.opendesktop.launcher.domain.Holidays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.Locale
import kotlin.math.ceil
import java.time.format.TextStyle as JTextStyle

/**
 * Wspólny komunikat widżetu (brak uprawnień, brak danych) z opcjonalnym przyciskiem –
 * ten sam wygląd we wszystkich widżetach.
 */
@Composable
internal fun GadgetNotice(p: GadgetPaint, icon: ImageVector, message: String, action: String? = null, onAction: () -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val fs = (minOf(maxHeight.value * 0.13f, maxWidth.value * 0.07f)).coerceIn(9f, 15f)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = p.fg.copy(alpha = 0.7f), modifier = Modifier.size((fs * 1.8f).dp))
            T(message, p.text(fs, FontWeight.Normal, 0.85f).copy(textAlign = TextAlign.Center), maxLines = 3)
            if (action != null) {
                T(
                    action,
                    p.text(fs, FontWeight.SemiBold, color = if (p.accent.luminance() > 0.55f) Color.Black else Color.White).copy(textAlign = TextAlign.Center, shadow = null),
                    Modifier.clip(RoundedCornerShape(50)).background(p.accent).clickable(onClick = onAction).padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
        }
    }
}

// ---------- Zegar klapkowy ----------

/** Cyfry na kartach (kolor akcentu) z linią zgięcia; zmiana cyfry przewija kartę w dół. */
@Composable
internal fun FlipClockView(p: GadgetPaint) {
    val st = p.st
    val now = rememberNow(st.showSeconds)
    val locale = Locale.getDefault()
    val hour = if (st.use24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val groups = listOfNotNull("%02d".format(hour), "%02d".format(now.minute), if (st.showSeconds) "%02d".format(now.second) else null)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val dateH = if (st.showDate) maxHeight.value * 0.2f else 0f
        val digits = groups.size * 2
        val cardW = minOf((maxWidth.value - (groups.size - 1) * 10f - digits * 4f) / digits, (maxHeight.value - dateH) * 0.68f).coerceAtLeast(10f)
        val cardH = cardW / 0.68f
        Column(horizontalAlignment = p.hAlign) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                groups.forEachIndexed { gi, g ->
                    if (gi > 0) Spacer(Modifier.width((cardW * 0.22f).dp))
                    g.forEach { ch -> FlipCard(p, ch, cardW, cardH) }
                }
                if (!st.use24h) {
                    T(if (now.hour < 12) " AM" else " PM", p.text(cardW * 0.3f, FontWeight.SemiBold, 0.85f))
                }
            }
            if (st.showDate) {
                T(
                    cap(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), locale),
                    p.text((dateH * 0.5f).coerceIn(9f, 20f), FontWeight.Medium, 0.9f),
                    p.gap.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FlipCard(p: GadgetPaint, ch: Char, w: Float, h: Float) {
    val card = p.accent
    val shape = RoundedCornerShape((w * 0.14f).dp)
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(w.dp, h.dp)
            .then(if (p.st.shadow > 0.01f) Modifier.shadow((p.st.shadow * 8f).dp, shape) else Modifier)
            .clip(shape)
            .background(card),
        contentAlignment = Alignment.Center,
    ) {
        // Górna połowa odrobinę jaśniejsza – jak prawdziwa klapka
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter).background(Color.White.copy(alpha = 0.05f)))
        AnimatedContent(
            targetState = ch,
            transitionSpec = { (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut()) },
            label = "flip",
        ) { c ->
            T("$c", p.hero(h * 0.62f / p.st.textScale.coerceAtLeast(0.3f)).copy(textAlign = TextAlign.Center, lineHeight = (h * 0.7f).sp, shadow = null))
        }
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(Color.Black.copy(alpha = 0.45f)))
    }
}

// ---------- Tydzień ----------

@Composable
internal fun WeekStripView(p: GadgetPaint) {
    val st = p.st
    val today = rememberNow(false).toLocalDate()
    val locale = Locale.getDefault()
    val start = Holidays.weekStart(today, st.mondayFirst)
    val holidays = remember(today.year) { (Holidays.forYear(today.year) + Holidays.forYear(today.year + 1)).filter { it.free }.map { it.date }.toSet() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val labelW = if (st.showDetails) maxWidth.value * 0.16f else 0f
        val cell = (maxWidth.value - labelW) / 7f
        val fs = minOf(maxHeight.value * 0.3f, cell * 0.3f).coerceIn(8f, 18f)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (st.showDetails) {
                Column(Modifier.width(labelW.dp)) {
                    T("tydz.", p.text(fs * 0.7f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Start))
                    T("${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}", p.hero(fs * 1.3f, FontWeight.SemiBold, color = p.accent).copy(textAlign = TextAlign.Start))
                }
            }
            for (i in 0 until 7) {
                val d = start.plusDays(i.toLong())
                val isToday = d == today
                val off = d.dayOfWeek.value >= 6 || d in holidays
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    T(
                        d.dayOfWeek.getDisplayName(JTextStyle.SHORT_STANDALONE, locale).take(3).lowercase(locale),
                        p.text(fs * 0.72f, FontWeight.Medium, if (off) 0.55f else 0.8f, color = if (d in holidays) Color(0xFFFF8A80) else p.fg).copy(textAlign = TextAlign.Center),
                    )
                    Box(
                        Modifier.size((fs * 1.9f).dp).clip(CircleShape).then(if (isToday) Modifier.background(p.accent) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        T(
                            "${d.dayOfMonth}",
                            p.text(fs, if (isToday) FontWeight.Bold else FontWeight.Normal, if (d.isBefore(today)) 0.5f else 1f,
                                color = if (isToday) (if (p.accent.luminance() > 0.55f) Color.Black else Color.White) else p.fg,
                            ).copy(textAlign = TextAlign.Center, shadow = if (isToday) null else p.shadow),
                        )
                    }
                }
            }
        }
    }
}

// ---------- Święta ----------

/** Najbliższe święta. Układ: 0 = najbliższe duże + lista, 1 = sama lista. */
@Composable
internal fun HolidaysView(p: GadgetPaint) {
    val st = p.st
    val today = rememberNow(false).toLocalDate()
    val locale = Locale.getDefault()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rows = (maxHeight.value / 22f).toInt().coerceIn(1, 12)
        val list = remember(today, st.showDetails, rows) { Holidays.upcoming(today, rows + 1, customary = st.showDetails) }
        val fs = (maxHeight.value * 0.085f).coerceIn(9f, 15f)
        val boxH = maxHeight.value
        if (list.isEmpty()) return@BoxWithConstraints
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val rest = if (st.mode == 0) {
                val next = list.first()
                val days = ChronoUnit.DAYS.between(today, next.date)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        T(next.name, p.text(fs * 1.25f, FontWeight.SemiBold, color = if (next.free) p.accent else p.fg).copy(textAlign = TextAlign.Start))
                        T(
                            listOfNotNull(holidayDate(next, locale), Holidays.bridgeHint(next)).joinToString(" · "),
                            p.text(fs * 0.85f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Start),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        T(if (days == 0L) "dziś" else "$days", p.hero(fs * 2.2f, FontWeight.SemiBold).copy(textAlign = TextAlign.End))
                        if (days > 0) T(if (days == 1L) "dzień" else "dni", p.text(fs * 0.8f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.End))
                    }
                }
                Separator(p)
                list.drop(1)
            } else list
            val maxRows = ((boxH - if (st.mode == 0) fs * 4.6f else 0f) / (fs * 1.75f)).toInt().coerceAtLeast(0)
            rest.take(maxRows).forEach { h ->
                val days = ChronoUnit.DAYS.between(today, h.date)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size((fs * 0.5f).dp).clip(CircleShape).background(if (h.free) p.accent else p.faint))
                    Spacer(Modifier.width(6.dp))
                    T(h.name, p.text(fs, FontWeight.Medium, if (h.free) 1f else 0.75f).copy(textAlign = TextAlign.Start), Modifier.weight(1f))
                    T(holidayDate(h, locale), p.text(fs * 0.85f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.End))
                    T(if (days == 0L) "dziś" else "$days d", p.text(fs * 0.85f, FontWeight.SemiBold).copy(textAlign = TextAlign.End), Modifier.width((fs * 3.4f * st.textScale).dp))
                }
            }
        }
    }
}

private fun holidayDate(h: Holiday, locale: Locale) =
    h.date.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale) + " " + h.date.format(DateTimeFormatter.ofPattern("d MMM", locale))

// ---------- Najbliższe wydarzenia ----------

private class AgendaEvent(val title: String, val begin: LocalDateTime, val end: LocalDateTime, val allDay: Boolean, val color: Int, val location: String)

private fun loadEvents(ctx: Context, days: Int): List<AgendaEvent> {
    val zone = ZoneId.systemDefault()
    val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    val end = start + days.coerceIn(1, 31) * 86_400_000L
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        android.content.ContentUris.appendId(it, start)
        android.content.ContentUris.appendId(it, end)
    }.build()
    val cols = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.DISPLAY_COLOR,
        CalendarContract.Instances.EVENT_LOCATION,
    )
    val out = ArrayList<AgendaEvent>()
    val nowMs = System.currentTimeMillis()
    ctx.contentResolver.query(uri, cols, "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
        while (c.moveToNext()) {
            val allDay = c.getInt(3) == 1
            val b = c.getLong(1)
            val e = c.getLong(2)
            // Wydarzenia całodniowe zapisane są w UTC – bez przesunięcia wypadłyby dzień wcześniej
            val tz = if (allDay) ZoneId.of("UTC") else zone
            if (!allDay && e < nowMs) continue
            out += AgendaEvent(
                title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(bez tytułu)",
                begin = Instant.ofEpochMilli(b).atZone(tz).toLocalDateTime(),
                end = Instant.ofEpochMilli(e).atZone(tz).toLocalDateTime(),
                allDay = allDay,
                color = c.getInt(4),
                location = c.getString(5).orEmpty(),
            )
        }
    }
    val today = LocalDate.now()
    // Całodniowe kończące się przed dziś (zakres zapytania łapie też wczorajsze w UTC) odpadają
    return out.filter { !it.allDay || it.end.toLocalDate().isAfter(today) }
}

/** Lista wydarzeń z kalendarza telefonu na [GadgetStyle.days] dni, pogrupowana po dniach. */
@Composable
internal fun AgendaView(p: GadgetPaint) {
    val st = p.st
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        tick++
    }
    if (!granted) {
        GadgetNotice(p, Icons.Rounded.Event, "Pozwól na odczyt kalendarza, żeby widzieć wydarzenia", "Pozwól") { ask.launch(Manifest.permission.READ_CALENDAR) }
        return
    }
    val minute = rememberNow(false)
    val events by produceState<List<AgendaEvent>?>(null, st.days, tick, minute.hour) {
        value = withContext(Dispatchers.IO) { runCatching { loadEvents(ctx, st.days) }.getOrDefault(emptyList()) }
    }
    val list = events ?: return
    if (list.isEmpty()) {
        GadgetNotice(p, Icons.Rounded.Event, "Brak wydarzeń w ciągu ${st.days.coerceIn(1, 31)} dni")
        return
    }
    val locale = Locale.getDefault()
    val today = minute.toLocalDate()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fs = (maxHeight.value * 0.075f).coerceIn(9f, 15f)
        val rowH = fs * 2.1f * st.textScale
        var budget = maxHeight.value
        Column(Modifier.fillMaxSize()) {
            // Trwające od wczoraj (także wielodniowe) trafiają pod „Dziś”
            list.groupBy { it.begin.toLocalDate().coerceAtLeastDay(today) }.forEach { (day, evs) ->
                if (budget < rowH * 1.6f) return@forEach
                val offset = ChronoUnit.DAYS.between(today, day)
                val title = when (offset) {
                    0L -> "Dziś"
                    1L -> "Jutro"
                    else -> cap(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), locale)
                }
                T(title, p.text(fs * 0.85f, FontWeight.Bold, color = p.accent).copy(textAlign = TextAlign.Start), Modifier.fillMaxWidth().padding(top = 2.dp))
                budget -= rowH * 0.7f
                evs.forEach { e ->
                    if (budget < rowH) return@forEach
                    budget -= rowH
                    Row(Modifier.fillMaxWidth().height(rowH.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Kolor kalendarza jako kropka
                        Box(Modifier.size((fs * 0.6f).dp).clip(CircleShape).background(Color(e.color or 0xFF000000.toInt())))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            T(e.title, p.text(fs, FontWeight.Medium).copy(textAlign = TextAlign.Start))
                            val time = if (e.allDay) "cały dzień" else clock(e.begin, st) + "–" + clock(e.end, st)
                            T(
                                listOfNotNull(time, e.location.takeIf { st.showDetails && it.isNotBlank() }).joinToString(" · "),
                                p.text(fs * 0.8f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Start),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LocalDate.coerceAtLeastDay(min: LocalDate) = if (isBefore(min)) min else this

// ---------- Następny budzik ----------

@Composable
internal fun NextAlarmView(p: GadgetPaint) {
    val st = p.st
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(AlarmManager::class.java) }
    var next by remember { mutableStateOf(am?.nextAlarmClock?.triggerTime) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { next = am?.nextAlarmClock?.triggerTime }
        }
        ContextCompat.registerReceiver(ctx, receiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { next = am?.nextAlarmClock?.triggerTime }
    val now = rememberNow(false)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = p.boxAlign) {
        val fs = minOf(maxHeight.value * 0.36f, maxWidth.value * 0.12f).coerceIn(10f, 30f)
        val t = next?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (t != null) Icons.Rounded.Alarm else Icons.Rounded.AlarmOff, null, tint = p.accent, modifier = Modifier.size((fs * 1.3f).dp))
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) {
                if (t == null) {
                    T("Brak budzika", p.text(fs * 0.7f, FontWeight.Medium).copy(textAlign = TextAlign.Start))
                } else {
                    val days = ChronoUnit.DAYS.between(now.toLocalDate(), t.toLocalDate())
                    val day = when (days) {
                        0L -> "dziś"
                        1L -> "jutro"
                        else -> t.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        T(clock(t, st), p.hero(fs, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                        T("  $day", p.text(fs * 0.45f, FontWeight.Normal, 0.8f), Modifier.padding(bottom = (fs * 0.12f).dp))
                    }
                    if (st.showDetails) {
                        val d = Duration.between(now, t).coerceAtLeast(Duration.ZERO)
                        T("za " + lengthText(d.plusMinutes(1)), p.text(fs * 0.42f, FontWeight.Normal, 0.65f).copy(textAlign = TextAlign.Start))
                    }
                }
            }
        }
    }
}

private fun Duration.coerceAtLeast(min: Duration) = if (this < min) min else this

// ---------- Stan telefonu ----------

private class DeviceInfo(
    val battery: Int,
    val charging: Boolean,
    val temp: Float,
    val storageUsed: Float,
    val storageFree: Long,
    val ramUsed: Float,
    val ramFree: Long,
    val uptimeMs: Long,
)

private fun deviceInfo(ctx: Context): DeviceInfo {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
    val fs = StatFs(Environment.getDataDirectory().path)
    val mi = ActivityManager.MemoryInfo()
    (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    return DeviceInfo(
        battery = if (level < 0) -1 else level * 100 / scale.coerceAtLeast(1),
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
        temp = temp,
        storageUsed = 1f - fs.availableBytes.toFloat() / fs.totalBytes.coerceAtLeast(1),
        storageFree = fs.availableBytes,
        ramUsed = 1f - mi.availMem.toFloat() / mi.totalMem.coerceAtLeast(1),
        ramFree = mi.availMem,
        uptimeMs = SystemClock.elapsedRealtime(),
    )
}

private fun gbText(b: Long) = "%.1f GB".format(b / 1_073_741_824.0)

/** Kafelki: bateria, temperatura, pamięć, RAM i czas pracy. Układ: 0 = kafelki, 1 = lista. */
@Composable
internal fun DeviceStatusView(p: GadgetPaint) {
    val st = p.st
    val ctx = LocalContext.current
    val info by produceState<DeviceInfo?>(null) {
        while (true) {
            value = withContext(Dispatchers.IO) { runCatching { deviceInfo(ctx) }.getOrNull() }
            delay(30_000L)
        }
    }
    val d = info ?: return
    val up = Duration.ofMillis(d.uptimeMs)
    val tiles = listOf(
        Triple(if (d.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd, "Bateria", "${d.battery}%") to (d.battery / 100f),
        Triple(Icons.Rounded.Thermostat, "Temperatura", "%.1f°".format(d.temp)) to ((d.temp - 20f) / 25f).coerceIn(0f, 1f),
        Triple(Icons.Rounded.SdStorage, "Pamięć", if (st.showDetails) "wolne ${gbText(d.storageFree)}" else "${(d.storageUsed * 100).toInt()}%") to d.storageUsed,
        Triple(Icons.Rounded.Memory, "RAM", if (st.showDetails) "wolne ${gbText(d.ramFree)}" else "${(d.ramUsed * 100).toInt()}%") to d.ramUsed,
        Triple(Icons.Rounded.Timer, "Czas pracy", if (up.toDays() > 0) "${up.toDays()} d ${up.toHours() % 24} h" else "${up.toHours()} h ${up.toMinutes() % 60} min") to null,
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (st.mode == 1) {
            val fs = (maxHeight.value / tiles.size * 0.42f).coerceIn(8f, 15f)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                tiles.forEach { (t, frac) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(t.first, null, tint = p.accent, modifier = Modifier.size((fs * 1.3f).dp))
                        T(t.second, p.text(fs, FontWeight.Normal, 0.75f).copy(textAlign = TextAlign.Start), Modifier.weight(1f).padding(start = 8.dp))
                        if (frac != null) {
                            Box(Modifier.width((fs * 4f).dp).height((fs * 0.4f).dp).clip(RoundedCornerShape(50)).background(p.faint)) {
                                Box(Modifier.fillMaxWidth(frac.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(barColor(p, t.second, frac)))
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        T(t.third, p.text(fs, FontWeight.SemiBold).copy(textAlign = TextAlign.End))
                    }
                }
            }
        } else {
            val cols = (maxWidth.value / 92f).toInt().coerceIn(2, 5)
            val rows = ceil(tiles.size / cols.toFloat()).toInt()
            val fs = minOf((maxHeight.value / rows) * 0.2f, (maxWidth.value / cols) * 0.12f).coerceIn(7f, 15f)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tiles.chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (t, frac) ->
                            Column(
                                Modifier.weight(1f).fillMaxHeight()
                                    .clip(RoundedCornerShape((st.radius.coerceIn(0, 48) * 0.55f).dp))
                                    .background(p.fg.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(t.first, null, tint = p.accent, modifier = Modifier.size((fs * 1.1f * st.textScale).dp))
                                    Spacer(Modifier.width(4.dp))
                                    T(t.second, p.text(fs * 0.8f, FontWeight.Normal, 0.7f).copy(textAlign = TextAlign.Start))
                                }
                                T(t.third, p.text(fs * 1.15f, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                                if (frac != null) {
                                    Spacer(Modifier.height(3.dp))
                                    Box(Modifier.fillMaxWidth().height((fs * 0.32f).coerceAtLeast(2f).dp).clip(RoundedCornerShape(50)).background(p.faint)) {
                                        Box(Modifier.fillMaxWidth(frac.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(barColor(p, t.second, frac)))
                                    }
                                }
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** Kolor paska: czerwony przy prawie pełnej pamięci, gorącym telefonie albo pustej baterii. */
private fun barColor(p: GadgetPaint, name: String, frac: Float): Color = when (name) {
    "Bateria" -> if (frac < 0.16f) Color(0xFFFF5252) else p.accent
    else -> if (frac > 0.9f) Color(0xFFFF5252) else p.accent
}
