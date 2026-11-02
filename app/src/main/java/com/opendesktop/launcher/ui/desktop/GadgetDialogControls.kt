package com.opendesktop.launcher.ui.desktop

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BorderStyle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.opendesktop.launcher.data.DeviceLocation
import com.opendesktop.launcher.data.GadgetKind
import com.opendesktop.launcher.data.GadgetStyle
import com.opendesktop.launcher.data.WeatherMetric
import com.opendesktop.launcher.data.WeatherPlace
import com.opendesktop.launcher.data.WeatherRepo
import com.opendesktop.launcher.data.withAppearance
import com.opendesktop.launcher.data.withLook
import com.opendesktop.launcher.ui.settings.DialogTextField
import com.opendesktop.launcher.ui.settings.Tile
import com.opendesktop.launcher.ui.theme.Palette
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

// ---------- Wybór koloru ----------

internal fun hsv(c: Long): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toInt(), it) }

internal fun fromHsv(h: Float, s: Float, v: Float): Long = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFF

internal fun hex(c: Long) = "#%06X".format(c and 0xFFFFFF)

/**
 * Wybór koloru: próbki i „własny” – suwaki odcienia, nasycenia i jasności oraz pole hex.
 * Zwraca kolor nieprzezroczysty; przezroczystość ustawia wywołujący.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPicker(
    title: String,
    value: Long,
    presets: List<Long>,
    auto: Boolean = false,
    onAuto: (() -> Unit)? = null,
    onPick: (Long) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(Color(value or 0xFF000000)).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape))
        Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text(if (auto) "auto" else hex(value), color = Palette.TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onAuto != null) Chip("Auto", auto, onClick = onAuto)
        presets.forEach { c ->
            val sel = !auto && (value and 0xFFFFFF) == (c and 0xFFFFFF)
            Swatch(Color(c or 0xFF000000), sel) { onPick(c or 0xFF000000) }
        }
        // Tęczowa próbka otwiera własny kolor
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                .border(if (open) 3.dp else 1.dp, if (open) Palette.Accent else Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable { open = !open },
        )
    }
    // Własny stan HSV: przy szarościach odcień nie wynika z koloru, więc nie odtwarzamy go z wartości
    val hsvState = remember { mutableStateOf(hsv(value or 0xFF000000)) }
    LaunchedEffect(value) {
        val (h, s, v) = hsvState.value
        if ((fromHsv(h, s, v) and 0xFFFFFF) != (value and 0xFFFFFF)) hsvState.value = hsv(value or 0xFF000000)
    }
    fun emit(h: Float, s: Float, v: Float) {
        hsvState.value = floatArrayOf(h, s, v)
        onPick(fromHsv(h, s, v))
    }
    if (open) {
        val hsv = hsvState.value
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            var text by remember(value) { mutableStateOf(hex(value)) }
            DialogTextField(text, { t ->
                text = t.take(7)
                val clean = t.trim().removePrefix("#")
                if (clean.length == 6) clean.toLongOrNull(16)?.let { onPick(0xFF000000 or it) }
            }, label = "Hex (#RRGGBB)")
            ValueSlider("Odcień", hsv[0], 0f..360f, { "${it.toInt()}°" }) { emit(it, hsv[1], hsv[2]) }
            ValueSlider("Nasycenie", hsv[1] * 100f, 0f..100f, { "${it.toInt()}%" }) { emit(hsv[0], it / 100f, hsv[2]) }
            ValueSlider("Jasność", hsv[2] * 100f, 0f..100f, { "${it.toInt()}%" }) { emit(hsv[0], hsv[1], it / 100f) }
        }
    }
}

@Composable
internal fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, if (selected) Palette.Accent else Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
    }
}

// ---------- Kontrolki okna ----------

/** Sekcja ustawień: karta z ikoną i nazwą, opcjonalna podpowiedź pod spodem. */
@Composable
internal fun Section(title: String, icon: ImageVector, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Palette.PanelHigh).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Palette.TextDim, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), color = Palette.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        content()
        if (hint != null) Hint(hint)
    }
}

@Composable
internal fun SubLabel(text: String) {
    Text(text, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
}

@Composable
internal fun Hint(text: String) {
    Text(text, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 16.sp)
}

/** Przełącznik segmentowy – wszystkie opcje widać naraz, bez przewijania w bok. */
@Composable
internal fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).padding(3.dp)) {
        options.forEachIndexed { i, name ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Palette.Accent else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(bg).clickable { onPick(i) }.padding(horizontal = 4.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name,
                    color = if (on) Palette.Ink else Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Wybór jednej z wielu opcji – chipy zawijane do kolejnych wierszy. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FlowChips(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, name -> Chip(name, i == selected) { onPick(i) } }
    }
}

@Composable
internal fun Chip(name: String, selected: Boolean, family: FontFamily? = null, onClick: () -> Unit) {
    Text(
        name,
        color = if (selected) Palette.Ink else Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = family,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Palette.Accent else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Przełączniki jako chipy z ikoną i ptaszkiem – zawijane, z pełnymi nazwami. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Toggles(tiles: List<Tile>, showIcons: Boolean = true) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tiles.forEach { t ->
            val bg by animateColorAsState(if (t.value) Palette.Accent else Color.White.copy(alpha = 0.08f), label = "toggle")
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(bg).clickable { t.onChange(!t.value) }.padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (t.value || showIcons) {
                    Icon(if (t.value) Icons.Rounded.Check else t.icon, null, tint = if (t.value) Palette.Ink else Palette.TextDim, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(t.label, color = if (t.value) Palette.Ink else Color.White, fontSize = 13.sp, fontWeight = if (t.value) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Suwak z nazwą i wartością; podgląd zmienia się już w trakcie przesuwania. */
@Composable
internal fun ValueSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(format(value), color = Palette.TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = Palette.Accent,
                activeTrackColor = Palette.Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

@Composable
internal fun ActionChip(icon: ImageVector, text: String, primary: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) Palette.Accent else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (primary) Palette.Ink else Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (primary) Palette.Ink else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(20.dp)) }
}
