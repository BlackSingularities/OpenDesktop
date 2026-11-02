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

// ---------- Styl karty ----------

@Composable
internal fun CardOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit, onApplyAll: ((GadgetStyle) -> Unit)?) {
    Section("Gotowe style", Icons.Rounded.Wallpaper) {
        LookGrid(st, set)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip(Icons.Rounded.ContentCopy, "Kopiuj wygląd") { copiedLook = st }
            copiedLook?.let { c -> ActionChip(Icons.Rounded.ContentPaste, "Wklej wygląd") { set(st.withAppearance(c)) } }
        }
    }
    if (onApplyAll != null) {
        Section(
            "Spójny wygląd", Icons.Rounded.DoneAll,
            hint = "Kolory, tło, rogi, cienie, czcionka i ikony tego widżetu trafią do wszystkich widżetów OpenDesktop na każdym ekranie, a nowe widżety dostaną je od razu. Treść i ustawienia każdego widżetu zostają.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip(Icons.Rounded.DoneAll, "Zastosuj do wszystkich", primary = true) { onApplyAll(st) }
            }
        }
    }
    Section("Tło", Icons.Rounded.Opacity) {
        FlowChips(gradients, st.gradient.coerceIn(0, 4)) { set(st.copy(gradient = it)) }
        ValueSlider("Krycie", ((st.background ushr 24) and 0xFF) / 255f * 100f, 0f..100f, { "${it.toInt()}%" }) { a ->
            val alpha = (a / 100f * 255f).toLong().coerceIn(0, 255)
            // Ta sama przezroczystość dla obu kolorów gradientu
            set(st.copy(background = (alpha shl 24) or (st.background and 0xFFFFFF), background2 = (alpha shl 24) or (st.background2 and 0xFFFFFF)))
        }
        if (st.weatherBackground) Hint("Tło zależy od pogody – wyłącz „Tło wg pogody” w Treści, żeby użyć własnego.")
    }
    Section("Kształt", Icons.Rounded.BorderStyle) {
        ValueSlider("Zaokrąglenie rogów", st.radius.toFloat(), 0f..48f, { "${it.toInt()} dp" }) { set(st.copy(radius = it.toInt())) }
        ValueSlider("Margines wewnętrzny", st.padding.toFloat(), 0f..32f, { "${it.toInt()} dp" }) { set(st.copy(padding = it.toInt())) }
        Toggles(listOf(Tile(Icons.Rounded.BorderStyle, "Obrys", st.border) { set(st.copy(border = it)) }))
        if (st.border) ValueSlider("Grubość obrysu", st.borderWidth, 0.5f..4f, { "%.1f dp".format(it) }) { set(st.copy(borderWidth = it)) }
    }
    Section("Cienie", Icons.Rounded.BlurOn, "Cień treści obejmuje tekst, ikony, wykresy i wskazówki zegara (Android 12+).") {
        FlowChips(shadowLooks.map { it.first }, shadowLooks.indexOfFirst { it.second.matches(st) }) { i -> set(shadowLooks[i].second.applyTo(st)) }
        ValueSlider("Cień pod kartą", st.shadow * 100f, 0f..100f, { "${it.toInt()}%" }) { set(st.copy(shadow = it / 100f)) }
        ValueSlider("Cień treści", st.contentShadow * 100f, 0f..100f, { "${it.toInt()}%" }) { set(st.copy(contentShadow = it / 100f)) }
        if (st.contentShadow > 0.01f) {
            ValueSlider("Rozmycie", st.shadowBlur, 0f..12f, { "%.1f dp".format(it) }) { set(st.copy(shadowBlur = it)) }
            ValueSlider("Przesunięcie", st.shadowOffset, 0f..8f, { "%.1f dp".format(it) }) { set(st.copy(shadowOffset = it)) }
        }
        if (st.shadow > 0.01f || st.contentShadow > 0.01f) {
            SubLabel("Kolor cienia")
            FlowChips(shadowColors.map { it.first }, shadowColors.indexOfFirst { it.second == st.shadowColor }) { set(st.copy(shadowColor = shadowColors[it].second)) }
        }
        Toggles(listOf(Tile(Icons.Rounded.BlurOn, "Klasyczny cień tekstu", st.textShadow) { set(st.copy(textShadow = it)) }))
    }
}

internal val shadowColors = listOf(
    "Czarny" to 0xFF000000, "Grafit" to 0xFF263238, "Granat" to 0xFF0D1B3E, "Brąz" to 0xFF3E2723, "Poświata" to 0xFFFFFFFF,
)

/** Gotowy zestaw cieni: karta, treść, rozmycie, przesunięcie. */
internal class ShadowLook(val card: Float, val content: Float, val blur: Float, val offset: Float) {
    fun matches(st: GadgetStyle) = st.shadow == card && st.contentShadow == content &&
        (content == 0f || (st.shadowBlur == blur && st.shadowOffset == offset))
    fun applyTo(st: GadgetStyle) = st.copy(shadow = card, contentShadow = content, shadowBlur = blur, shadowOffset = offset)
}

internal val shadowLooks = listOf(
    "Brak" to ShadowLook(0f, 0f, 3f, 1.5f),
    "Delikatne" to ShadowLook(0.2f, 0.35f, 2f, 1f),
    "Miękkie" to ShadowLook(0.4f, 0.55f, 5f, 2f),
    "Wyraźne" to ShadowLook(0.6f, 0.8f, 1.5f, 2.5f),
    "Uniesione" to ShadowLook(0.9f, 0.6f, 8f, 5f),
)

/** Gotowe style jako miniatury karty: tło, kolor tekstu i akcentu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LookGrid(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        looks.forEach { (name, look) ->
            val on = st.withLook(look) == st
            Column(Modifier.width(68.dp).clickable { set(st.withLook(look)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                val shape = RoundedCornerShape((look.radius * 0.5f).coerceIn(4f, 16f).dp)
                Box(
                    Modifier
                        .size(68.dp, 50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFDCE3EC), Color(0xFF3A4A5E))))
                        .border(if (on) 2.dp else 0.dp, if (on) Palette.Accent else Color.Transparent, RoundedCornerShape(14.dp))
                        .padding(6.dp),
                ) {
                    Box(
                        Modifier.fillMaxSize().clip(shape).background(backgroundBrush(look))
                            .then(if (look.border) Modifier.border(1.dp, if (look.borderColor != 0L) Color(look.borderColor) else Color(look.textColor).copy(alpha = 0.3f), shape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Aa", color = Color(look.textColor), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(look.accent)))
                        }
                    }
                }
                Text(name, color = if (on) Color.White else Palette.TextDim, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ---------- Kolory ----------

@Composable
internal fun ColorOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Section("Tekst i akcent", Icons.Rounded.ColorLens) {
        ColorPicker("Kolor tekstu", st.textColor, textColors) { set(st.copy(textColor = it)) }
        ColorPicker("Kolor akcentu", st.accent, accentColors) { set(st.copy(accent = it)) }
    }
    val alpha = ((st.background ushr 24) and 0xFF).let { if (it == 0L) 0x66L else it }
    Section("Tło", Icons.Rounded.Opacity, hint = if (st.weatherBackground) "Teraz tło zależy od pogody – te kolory działają po wyłączeniu „Tło wg pogody”." else null) {
        ColorPicker(if (st.gradient > 0) "Pierwszy kolor" else "Kolor tła", st.background or 0xFF000000, backgroundColors) { c ->
            set(st.copy(background = (alpha shl 24) or (c and 0xFFFFFF)))
        }
        if (st.gradient > 0) {
            ColorPicker("Drugi kolor", st.background2 or 0xFF000000, backgroundColors) { c ->
                set(st.copy(background2 = (alpha shl 24) or (c and 0xFFFFFF)))
            }
        }
    }
    if (st.border) {
        Section("Obrys", Icons.Rounded.BorderStyle) {
            ColorPicker("Kolor obrysu", if (st.borderColor == 0L) st.textColor else st.borderColor, accentColors, auto = st.borderColor == 0L, onAuto = { set(st.copy(borderColor = 0)) }) {
                set(st.copy(borderColor = (0xCCL shl 24) or (it and 0xFFFFFF)))
            }
        }
    }
}

// ---------- Tekst ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TextOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Section("Wielkość i grubość", Icons.Rounded.TextFields) {
        ValueSlider("Wielkość tekstu", st.textScale * 100f, 60f..160f, { "${it.toInt()}%" }) { set(st.copy(textScale = it / 100f)) }
        if (kind.heroLabel() != null) {
            ValueSlider(kind.heroLabel()!!, st.timeScale * 100f, 50f..200f, { "${it.toInt()}%" }) { set(st.copy(timeScale = it / 100f)) }
        }
        if (kind.usesTimeGap()) {
            ValueSlider("Odstęp pod godziną", st.timeGap.toFloat().coerceIn(-24f, 24f), -24f..24f, { "${it.toInt()} dp" }, steps = 47) {
                set(st.copy(timeGap = it.toInt()))
            }
        }
        FlowChips(weights.map { it.first }, weights.indexOfFirst { it.second == st.weight }) { set(st.copy(weight = weights[it].second)) }
    }
    Section("Krój pisma", Icons.Rounded.Title) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            fonts.forEachIndexed { i, name ->
                val family = when (i) {
                    1 -> FontFamily.Serif
                    2 -> FontFamily.Monospace
                    3 -> FontFamily.Cursive
                    4 -> FontFamily(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL))
                    else -> FontFamily.SansSerif
                }
                Chip(name, st.fontFamily == i, family) { set(st.copy(fontFamily = i)) }
            }
        }
    }
    if (kind.usesAlign()) {
        Section("Wyrównanie", Icons.Rounded.Subject) {
            Segmented(aligns.map { it.first }, aligns.indexOfFirst { it.second == st.align }) { set(st.copy(align = aligns[it].second)) }
        }
    }
}

/** Podpis suwaka osobnej wielkości głównej wartości; null = widżet jej nie ma. */
internal fun GadgetKind.heroLabel(): String? = when (this) {
    GadgetKind.DigitalClock, GadgetKind.ClockWeather, GadgetKind.SkyClock, GadgetKind.WorldClock -> "Wielkość godziny"
    GadgetKind.Countdown -> "Wielkość liczby dni"
    GadgetKind.DateCard -> "Wielkość liczby dnia"
    GadgetKind.MoonPhase, GadgetKind.MoonRound -> "Wielkość nazwy fazy"
    GadgetKind.FlipClock, GadgetKind.NextAlarm -> "Wielkość godziny"
    GadgetKind.Holidays -> "Wielkość liczby dni"
    GadgetKind.AirQuality -> "Wielkość indeksu"
    GadgetKind.Wind -> "Wielkość prędkości"
    else -> null
}

/** Widżety, w których pod główną wartością jest jeszcze jedna linia (data, dzień, podpis). */
internal fun GadgetKind.usesTimeGap() = this in setOf(
    GadgetKind.DigitalClock, GadgetKind.ClockWeather, GadgetKind.SkyClock, GadgetKind.Countdown, GadgetKind.DateCard, GadgetKind.FlipClock,
)

internal fun GadgetKind.usesAlign() = this !in setOf(
    GadgetKind.AnalogClock, GadgetKind.Forecast, GadgetKind.Calendar, GadgetKind.Progress, GadgetKind.Storage, GadgetKind.ClockWeather,
    GadgetKind.HourlyForecast, GadgetKind.WeatherFull, GadgetKind.WeatherDetails, GadgetKind.SunArc, GadgetKind.YearDots,
    GadgetKind.WeatherChart, GadgetKind.AirQuality, GadgetKind.RainNow, GadgetKind.WeekStrip, GadgetKind.Holidays, GadgetKind.Agenda,
    GadgetKind.DeviceStatus, GadgetKind.Wind,
)
