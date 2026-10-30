package com.opendesktop.launcher.ui.desktop

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.opendesktop.launcher.data.Health
import kotlinx.coroutines.delay
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Wspólny sygnał „odczytaj ponownie” dla wszystkich widżetów kroków (powrót na pulpit, nadanie uprawnień). */
private object HealthRefresh {
    var tick by mutableIntStateOf(0)
}

private sealed interface StepsState {
    data object Loading : StepsState
    data class Problem(val text: String, val action: String? = null) : StepsState
    data class Ready(val data: Health.Summary) : StepsState
}

/** Kroki i kalorie dziś. Układ: 0 = pierścień, 1 = pasek, 2 = tydzień. */
@Composable
internal fun StepsView(p: GadgetPaint) {
    val ctx = LocalContext.current
    val status = remember { Health.status(ctx) }
    var state by remember { mutableStateOf<StepsState>(StepsState.Loading) }
    val ask = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { HealthRefresh.tick++ }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { HealthRefresh.tick++ }
    LaunchedEffect(status, HealthRefresh.tick) {
        if (status != Health.Status.Ready) {
            state = StepsState.Problem(
                if (status == Health.Status.NeedsUpdate) "Zaktualizuj Health Connect" else "Kroki wymagają Health Connect",
                if (status == Health.Status.NeedsUpdate) "Aktualizuj" else "Zainstaluj",
            )
            return@LaunchedEffect
        }
        while (true) {
            val granted = runCatching { Health.granted(ctx) }.getOrDefault(emptySet())
            state = if (!Health.canRead(granted)) {
                StepsState.Problem("Połącz z Health Connect, żeby widzieć kroki i kalorie", "Połącz")
            } else {
                runCatching { Health.read(ctx) }.fold(
                    { StepsState.Ready(it) },
                    { (state as? StepsState.Ready) ?: StepsState.Problem("Nie udało się odczytać danych") },
                )
            }
            delay(5 * 60_000L)
        }
    }
    when (val s = state) {
        StepsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { T("Wczytywanie…", p.text(13f, FontWeight.Normal, 0.7f)) }
        is StepsState.Problem -> StepsProblem(p, s) {
            if (status == Health.Status.Ready) ask.launch(Health.permissions)
            else runCatching { ctx.startActivity(Health.storeIntent()) }
        }
        is StepsState.Ready -> when (p.st.mode) {
            1 -> StepsBar(p, s.data)
            2 -> StepsWeek(p, s.data)
            else -> StepsRing(p, s.data)
        }
    }
}

@Composable
private fun StepsProblem(p: GadgetPaint, s: StepsState.Problem, onAction: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val fs = (minOf(maxHeight.value, maxWidth.value) * 0.1f).coerceIn(10f, 15f)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.DirectionsWalk, null, tint = p.accent, modifier = Modifier.size((fs * 2f).dp))
            T(s.text, p.text(fs, FontWeight.Normal, 0.85f).copy(textAlign = TextAlign.Center), maxLines = 2)
            s.action?.let { label ->
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(p.accent)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                ) { T(label, p.text(fs * 0.95f, FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.Black).copy(shadow = null)) }
            }
        }
    }
}

private fun count(n: Long): String = "%,d".format(Locale.getDefault(), n)

private fun kcal(d: Health.Day): Int = (if (d.activeKcal > 0) d.activeKcal else d.totalKcal).roundToInt()

private fun kcalLabel(d: Health.Day) = if (d.activeKcal > 0) "kcal aktywnie" else "kcal"

private fun km(m: Double): String = if (m < 1000) "${m.roundToInt()} m" else "%.1f km".format(m / 1000)

/** Kalorie, dystans i cel – wiersze z ikonami. */
@Composable
private fun StatLines(p: GadgetPaint, data: Health.Summary, fs: Float, align: Alignment.Horizontal) {
    val d = data.today
    val pct = (d.steps * 100 / p.st.goal.coerceAtLeast(1)).toInt()
    Column(horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy((fs * 0.25f).dp)) {
        if (p.st.showDetails && data.hasCalories) StatLine(p, Icons.Rounded.LocalFireDepartment, "${kcal(d)}", kcalLabel(d), fs)
        if (p.st.showDetails && data.hasDistance) StatLine(p, Icons.Rounded.Straighten, km(d.meters), "", fs)
        StatLine(p, Icons.Rounded.Flag, "$pct%", "z ${count(p.st.goal.toLong())}", fs)
    }
}

@Composable
private fun StatLine(p: GadgetPaint, icon: ImageVector, value: String, unit: String, fs: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = p.accent, modifier = Modifier.size((fs * 1.15f).dp))
        Spacer(Modifier.width((fs * 0.35f).dp))
        T(value, p.text(fs * 1.1f, FontWeight.SemiBold))
        if (unit.isNotEmpty()) T(" $unit", p.text(fs * 0.85f, FontWeight.Normal, 0.65f))
    }
}

@Composable
private fun StepsRing(p: GadgetPaint, data: Health.Summary) {
    val d = data.today
    val frac = (d.steps.toFloat() / p.st.goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val ring = minOf(maxHeight.value, maxWidth.value * 0.48f)
        val fs = (ring * 0.13f).coerceIn(9f, 16f)
        val wide = maxWidth.value > ring * 1.6f
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (wide) Arrangement.Start else Arrangement.Center) {
            Box(Modifier.size(ring.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = size.minDimension * 0.09f
                    val inset = sw / 2
                    val arcSize = Size(size.width - sw, size.height - sw)
                    drawArc(p.faint, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                    if (frac > 0f) drawArc(p.accent, 135f, 270f * frac, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.DirectionsWalk, null, tint = p.accent, modifier = Modifier.size((ring * 0.16f).dp))
                    T(count(d.steps), p.text(ring * 0.19f, FontWeight.SemiBold))
                    T("kroków", p.text(ring * 0.09f, FontWeight.Normal, 0.65f))
                }
            }
            if (wide) {
                Spacer(Modifier.width((fs * 1.2f).dp))
                StatLines(p, data, fs, Alignment.Start)
            }
        }
    }
}

@Composable
private fun StepsBar(p: GadgetPaint, data: Health.Summary) {
    val d = data.today
    val frac = (d.steps.toFloat() / p.st.goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val big = minOf(maxHeight.value * 0.36f, maxWidth.value * 0.14f).coerceAtLeast(14f)
        val fs = (big * 0.36f).coerceIn(9f, 15f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        T(count(d.steps), p.text(big, FontWeight.SemiBold).copy(textAlign = TextAlign.Start))
                        T(" kroków", p.text(fs, FontWeight.Normal, 0.65f), Modifier.padding(bottom = (big * 0.14f).dp))
                    }
                }
                StatLines(p, data, fs, Alignment.Start)
            }
            Spacer(Modifier.height((fs * 0.6f).dp))
            Box(Modifier.fillMaxWidth().height((fs * 0.6f).dp).clip(CircleShape).background(p.faint)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(p.accent))
            }
        }
    }
}

@Composable
private fun StepsWeek(p: GadgetPaint, data: Health.Summary) {
    val d = data.today
    val locale = Locale.getDefault()
    val goal = p.st.goal.coerceAtLeast(1)
    val max = maxOf(goal.toLong(), data.week.maxOf { it.steps }).toFloat()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fs = (maxHeight.value * 0.09f).coerceIn(9f, 15f)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DirectionsWalk, null, tint = p.accent, modifier = Modifier.size((fs * 1.4f).dp))
                T(count(d.steps), p.text(fs * 1.5f, FontWeight.SemiBold))
                T(" kroków", p.text(fs * 0.9f, FontWeight.Normal, 0.65f), Modifier.weight(1f))
                if (p.st.showDetails && data.hasCalories) StatLine(p, Icons.Rounded.LocalFireDepartment, "${kcal(d)}", "kcal", fs)
            }
            Spacer(Modifier.height((fs * 0.5f).dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                data.week.forEach { day ->
                    val today = day.date == d.date
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Canvas(Modifier.fillMaxSize()) {
                                // Linia celu
                                val gy = size.height * (1f - goal / max)
                                drawLine(p.fg.copy(alpha = 0.25f), Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                            }
                            Box(
                                Modifier
                                    .width((fs * 1.1f).dp)
                                    .fillMaxHeight((day.steps / max).coerceIn(0.03f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        when {
                                            today -> p.accent
                                            day.steps >= goal -> p.accent.copy(alpha = 0.55f)
                                            else -> p.fg.copy(alpha = 0.3f)
                                        }
                                    ),
                            )
                        }
                        T(
                            day.date.dayOfWeek.getDisplayName(JTextStyle.NARROW_STANDALONE, locale).uppercase(locale),
                            p.text(fs * 0.8f, if (today) FontWeight.SemiBold else FontWeight.Normal, if (today) 1f else 0.6f, color = if (today) p.accent else p.fg),
                        )
                    }
                }
            }
        }
    }
}
