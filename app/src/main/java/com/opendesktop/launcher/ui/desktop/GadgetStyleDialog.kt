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

/** Gotowe zestawy wyglądu – zmieniają tylko kolory, tło, rogi, obrys i cienie, nie ustawienia treści. */
internal val looks = listOf(
    "Szkło" to GadgetStyle(),
    "Bez tła" to GadgetStyle(background = 0x00000000, border = false, textShadow = true),
    "Ciemny" to GadgetStyle(background = 0xB3101418, border = false, shadow = 0.4f, accent = 0xFF8AB4FF),
    "Jasny" to GadgetStyle(background = 0xF2FFFFFF, textColor = 0xFF202124, accent = 0xFF1A73E8, border = false, shadow = 0.5f),
    "Neon" to GadgetStyle(background = 0xCC0B0F1A, textColor = 0xFF80DEEA, accent = 0xFFFF4081, border = true, borderColor = 0xAAFF4081, borderWidth = 1.5f),
    "Ciepły" to GadgetStyle(background = 0xE6FFF3E0, textColor = 0xFF4E342E, accent = 0xFFFF7043, border = false, shadow = 0.3f),
    "Las" to GadgetStyle(background = 0xD91B3A2B, textColor = 0xFFE8F5E9, accent = 0xFFA5D6A7, border = false),
    "Ocean" to GadgetStyle(background = 0xD90D2847, textColor = 0xFFE3F2FD, accent = 0xFF4FC3F7, border = false, shadow = 0.3f),
    "Róż" to GadgetStyle(background = 0xE6FCE4EC, textColor = 0xFF880E4F, accent = 0xFFEC407A, border = false),
    "Zachód" to GadgetStyle(background = 0xE6FF7E5F, background2 = 0xE6845EC2, gradient = 3, textColor = 0xFFFFFFFF, accent = 0xFFFFE082, border = false, shadow = 0.35f, textShadow = true),
    "Zorza" to GadgetStyle(background = 0xE60F2027, background2 = 0xE62C8C7A, gradient = 1, textColor = 0xFFE0F7FA, accent = 0xFF64FFDA, border = false, shadow = 0.3f),
    "Noc" to GadgetStyle(background = 0xF00B1026, background2 = 0xF0302B63, gradient = 4, textColor = 0xFFE8EAF6, accent = 0xFFB39DDB, border = true, borderColor = 0x33B39DDB),
    "Grafit" to GadgetStyle(background = 0xF22B2D31, background2 = 0xF2141517, gradient = 1, textColor = 0xFFECEFF1, accent = 0xFFFFB74D, border = true, borderColor = 0x22FFFFFF, shadow = 0.5f, radius = 18),
    "Papier" to GadgetStyle(background = 0xFAF7F1E3, textColor = 0xFF3E2723, accent = 0xFFD84315, border = true, borderColor = 0x333E2723, radius = 8, shadow = 0.2f),
    "Mięta" to GadgetStyle(background = 0xEBE0F2E9, background2 = 0xEBB2DFDB, gradient = 3, textColor = 0xFF004D40, accent = 0xFF00897B, border = false, shadow = 0.25f),
    "Terminal" to GadgetStyle(background = 0xF0000000, textColor = 0xFF33FF66, accent = 0xFF33FF66, border = true, borderColor = 0x8033FF66, radius = 4),
)

/** Schowek wyglądu, żeby nadać kilku widżetom ten sam styl (do zamknięcia aplikacji). */
internal var copiedLook: GadgetStyle? = null

internal val textColors = listOf(0xFFFFFFFF, 0xFF202124, 0xFFFFD54F, 0xFF80DEEA, 0xFFA5D6A7, 0xFFF48FB1, 0xFFB39DDB, 0xFFFFAB91, 0xFFECEFF1, 0xFF3E2723, 0xFF33FF66)
internal val accentColors = listOf(0xFF8AB4FF, 0xFF1A73E8, 0xFFFF7043, 0xFF69F0AE, 0xFFFFD54F, 0xFFEC407A, 0xFFBA68C8, 0xFF4DD0E1, 0xFFFFFFFF, 0xFFFFB74D, 0xFF64FFDA, 0xFFFF5252)
internal val backgroundColors = listOf(0xFF000000, 0xFFFFFFFF, 0xFF263238, 0xFF1E88E5, 0xFF43A047, 0xFFE53935, 0xFF8E24AA, 0xFFFFB300, 0xFF0D2847, 0xFF845EC2, 0xFFFF7E5F, 0xFF2C8C7A, 0xFFF7F1E3)

internal fun hoursText(h: Int) = if (h >= 48 && h % 24 == 0) "${h / 24} dni ($h h)" else "$h h"

internal val weights = listOf("Cienka" to 200, "Lekka" to 300, "Zwykła" to 400, "Średnia" to 500, "Gruba" to 600, "Czarna" to 800)
internal val aligns = listOf("Do lewej" to 0, "Środek" to 1, "Do prawej" to 2)
internal val fonts = listOf("Systemowa", "Szeryfowa", "Maszynowa", "Odręczna", "Wąska")
internal val gradients = listOf("Jednolite", "Pionowy", "Poziomy", "Ukośny", "Promienisty")
internal val iconStyles = listOf("Akcent", "Kolorowe", "Tekst", "Emoji")
internal val quickCities = listOf("Warszawa", "Kraków", "Gdańsk", "Wrocław", "Poznań", "Łódź", "Szczecin", "Lublin", "Zakopane")

/** Tła pod podglądem: sprawdzenie czytelności na jasnej, mieszanej i ciemnej tapecie. */
internal val backdrops = listOf(
    "Jasne" to Brush.linearGradient(listOf(Color(0xFFF6F2EA), Color(0xFFD9DEE6))),
    "Mieszane" to Brush.linearGradient(listOf(Color(0xFFF3EFE6), Color(0xFF7A8FA6), Color(0xFF0E1420))),
    "Ciemne" to Brush.linearGradient(listOf(Color(0xFF1B2433), Color(0xFF05070B))),
)

/** Wysokość podglądu zależna od proporcji kafelka. */
internal fun previewHeight(kind: GadgetKind) = when {
    kind.h >= 4 -> 260.dp
    kind.h >= 3 -> 220.dp
    kind.h == 1 -> 84.dp
    kind.w <= 2 -> 170.dp
    else -> 150.dp
}

internal enum class DialogPage(val label: String, val icon: ImageVector) {
    Place("Miejsce", Icons.Rounded.Place),
    Content("Treść", Icons.Rounded.Tune),
    Style("Styl", Icons.Rounded.Wallpaper),
    Colors("Kolory", Icons.Rounded.ColorLens),
    Text("Tekst", Icons.Rounded.TextFields),
}

/**
 * Dostosowanie wbudowanego widżetu na pełnym ekranie: pasek z zapisem, duży podgląd na wybranym
 * tle i karty – miejsce (pogoda), treść, styl karty, kolory i tekst. Każda zmiana od razu
 * widać w podglądzie; zapis dopiero przyciskiem.
 */
@Composable
fun GadgetStyleDialog(
    kind: GadgetKind,
    initial: GadgetStyle,
    onDismiss: () -> Unit,
    onSave: (GadgetStyle) -> Unit,
    /** Zapis tego widżetu i przeniesienie jego wyglądu na wszystkie pozostałe. */
    onApplyAll: ((GadgetStyle) -> Unit)? = null,
) {
    var st by remember { mutableStateOf(initial) }
    val pages = remember(kind) { DialogPage.entries.filter { it != DialogPage.Place || kind.isWeather } }
    var page by remember { mutableStateOf(pages.first()) }
    var backdrop by remember { mutableIntStateOf(1) }
    val set: (GadgetStyle) -> Unit = { st = it }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Palette.Panel)) {
            // Pasek: zamknij, nazwa, przywróć, zapisz
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Rounded.Close, "Zamknij", onClick = onDismiss)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(kind.label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (st != initial) "Niezapisane zmiany" else kind.description, color = if (st != initial) Palette.Accent else Palette.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                RoundIcon(Icons.Rounded.RestartAlt, "Przywróć domyślne") { st = kind.defaultStyle() }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Zapisz",
                    color = Palette.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Palette.Accent).clickable { onSave(st) }.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
            PreviewStage(kind, st, backdrop) { backdrop = it }
            TabBar(pages, page) { page = it }
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (page) {
                    DialogPage.Place -> PlaceOptions(st, set)
                    DialogPage.Content -> {
                        KindOptions(kind, st, set)
                        if (kind.isWeather) WeatherLookOptions(kind, st, set)
                    }
                    DialogPage.Style -> CardOptions(st, set, onApplyAll)
                    DialogPage.Colors -> ColorOptions(st, set)
                    DialogPage.Text -> TextOptions(kind, st, set)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
internal fun PreviewStage(kind: GadgetKind, st: GadgetStyle, backdrop: Int, onBackdrop: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(backdrops[backdrop].second)
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Szerokość jak na pulpicie: wąskie widżety nie rozciągają się na cały ekran
            val frac = (kind.w / 4f).coerceIn(0.5f, 1f)
            GadgetWidget(kind, st, Modifier.fillMaxWidth(frac).height(previewHeight(kind)))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tło podglądu", color = Palette.TextFaint, fontSize = 11.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
            backdrops.forEachIndexed { i, (name, brush) ->
                Row(
                    Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i == backdrop) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onBackdrop(i) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(brush).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text(name, color = if (i == backdrop) Color.White else Palette.TextDim, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
internal fun TabBar(pages: List<DialogPage>, selected: DialogPage, onPick: (DialogPage) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 10.dp)
            .clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.06f)).padding(4.dp),
    ) {
        pages.forEach { pg ->
            val on = pg == selected
            val bg by animateColorAsState(if (on) Palette.Accent else Color.Transparent, label = "tab")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(bg).clickable { onPick(pg) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(pg.icon, null, tint = if (on) Palette.Ink else Palette.TextDim, modifier = Modifier.size(18.dp))
                Text(pg.label, color = if (on) Palette.Ink else Palette.TextDim, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}
