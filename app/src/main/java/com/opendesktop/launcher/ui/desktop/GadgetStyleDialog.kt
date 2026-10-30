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
import com.opendesktop.launcher.ui.settings.DialogTextField
import com.opendesktop.launcher.ui.settings.Tile
import com.opendesktop.launcher.ui.theme.Palette
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Gotowe zestawy wyglądu – zmieniają tylko kolory, tło, rogi, obrys i cienie, nie ustawienia treści. */
private val looks = listOf(
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

private fun GadgetStyle.withLook(o: GadgetStyle) = copy(
    background = o.background, background2 = o.background2, gradient = o.gradient, radius = o.radius, textColor = o.textColor,
    accent = o.accent, border = o.border, borderColor = o.borderColor, borderWidth = o.borderWidth, shadow = o.shadow, textShadow = o.textShadow,
)

/** Pełny wygląd (z czcionką i ikonami) – do kopiowania między widżetami. */
private fun GadgetStyle.withAppearance(o: GadgetStyle) = withLook(o).copy(
    padding = o.padding, textScale = o.textScale, timeScale = o.timeScale, weight = o.weight, fontFamily = o.fontFamily,
    iconStyle = o.iconStyle, iconScale = o.iconScale, tempColors = o.tempColors, weatherBackground = o.weatherBackground,
    separators = o.separators,
    contentShadow = o.contentShadow, shadowBlur = o.shadowBlur, shadowOffset = o.shadowOffset, shadowColor = o.shadowColor,
)

/** Schowek wyglądu, żeby nadać kilku widżetom ten sam styl (do zamknięcia aplikacji). */
private var copiedLook: GadgetStyle? = null

private val textColors = listOf(0xFFFFFFFF, 0xFF202124, 0xFFFFD54F, 0xFF80DEEA, 0xFFA5D6A7, 0xFFF48FB1, 0xFFB39DDB, 0xFFFFAB91, 0xFFECEFF1, 0xFF3E2723, 0xFF33FF66)
private val accentColors = listOf(0xFF8AB4FF, 0xFF1A73E8, 0xFFFF7043, 0xFF69F0AE, 0xFFFFD54F, 0xFFEC407A, 0xFFBA68C8, 0xFF4DD0E1, 0xFFFFFFFF, 0xFFFFB74D, 0xFF64FFDA, 0xFFFF5252)
private val backgroundColors = listOf(0xFF000000, 0xFFFFFFFF, 0xFF263238, 0xFF1E88E5, 0xFF43A047, 0xFFE53935, 0xFF8E24AA, 0xFFFFB300, 0xFF0D2847, 0xFF845EC2, 0xFFFF7E5F, 0xFF2C8C7A, 0xFFF7F1E3)

private val weights = listOf("Cienka" to 200, "Lekka" to 300, "Zwykła" to 400, "Średnia" to 500, "Gruba" to 600, "Czarna" to 800)
private val aligns = listOf("Do lewej" to 0, "Środek" to 1, "Do prawej" to 2)
private val fonts = listOf("Systemowa", "Szeryfowa", "Maszynowa", "Odręczna", "Wąska")
private val gradients = listOf("Jednolite", "Pionowy", "Poziomy", "Ukośny", "Promienisty")
private val iconStyles = listOf("Akcent", "Kolorowe", "Tekst", "Emoji")
private val quickCities = listOf("Warszawa", "Kraków", "Gdańsk", "Wrocław", "Poznań", "Łódź", "Szczecin", "Lublin", "Zakopane")

/** Tła pod podglądem: sprawdzenie czytelności na jasnej, mieszanej i ciemnej tapecie. */
private val backdrops = listOf(
    "Jasne" to Brush.linearGradient(listOf(Color(0xFFF6F2EA), Color(0xFFD9DEE6))),
    "Mieszane" to Brush.linearGradient(listOf(Color(0xFFF3EFE6), Color(0xFF7A8FA6), Color(0xFF0E1420))),
    "Ciemne" to Brush.linearGradient(listOf(Color(0xFF1B2433), Color(0xFF05070B))),
)

/** Wysokość podglądu zależna od proporcji kafelka. */
private fun previewHeight(kind: GadgetKind) = when {
    kind.h >= 4 -> 260.dp
    kind.h >= 3 -> 220.dp
    kind.h == 1 -> 84.dp
    kind.w <= 2 -> 170.dp
    else -> 150.dp
}

private enum class DialogPage(val label: String, val icon: ImageVector) {
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
fun GadgetStyleDialog(kind: GadgetKind, initial: GadgetStyle, onDismiss: () -> Unit, onSave: (GadgetStyle) -> Unit) {
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
                    DialogPage.Style -> CardOptions(st, set)
                    DialogPage.Colors -> ColorOptions(st, set)
                    DialogPage.Text -> TextOptions(kind, st, set)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PreviewStage(kind: GadgetKind, st: GadgetStyle, backdrop: Int, onBackdrop: (Int) -> Unit) {
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
private fun TabBar(pages: List<DialogPage>, selected: DialogPage, onPick: (DialogPage) -> Unit) {
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

// ---------- Miejsce (pogoda) ----------

@Composable
private fun PlaceOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(DeviceLocation.granted(ctx)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) {
            DeviceLocation.clear()
            set(st.copy(useDeviceLocation = true))
        }
    }
    Section("Lokalizacja", Icons.Rounded.Place) {
        Segmented(listOf("Moja lokalizacja", "Wybrane miejsce"), if (st.useDeviceLocation) 0 else 1) { i ->
            if (i == 1) set(st.copy(useDeviceLocation = false))
            else if (granted) set(st.copy(useDeviceLocation = true))
            else runCatching { ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
        }
        if (st.useDeviceLocation) DevicePlace(granted) { runCatching { ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } }
        else CityPicker(st, set)
    }
    Section("Odświeżanie", Icons.Rounded.Refresh, hint = "Pogoda odświeża się sama co 20 minut. Przycisk w rogu widżetu pobiera ją od razu.") {
        Toggles(listOf(Tile(Icons.Rounded.Refresh, "Przycisk odświeżania", st.showRefresh) { set(st.copy(showRefresh = it)) }))
        ActionChip(Icons.Rounded.Refresh, "Odśwież teraz") { WeatherRefresh.request() }
    }
    UnitOptions(st, set)
}

/** Stan „Moja lokalizacja”: wykryte miejsce albo prośba o zgodę. */
@Composable
private fun DevicePlace(granted: Boolean, onAsk: () -> Unit) {
    val ctx = LocalContext.current
    var place by remember { mutableStateOf<Result<WeatherPlace>?>(null) }
    LaunchedEffect(granted, WeatherRefresh.tick) {
        place = null
        if (granted) place = runCatching { DeviceLocation.current(ctx.applicationContext) }
    }
    when {
        !granted -> {
            Hint("Launcher potrzebuje przybliżonej lokalizacji, żeby pokazać pogodę tam, gdzie jesteś.")
            ActionChip(Icons.Rounded.MyLocation, "Zezwól na lokalizację", primary = true, onClick = onAsk)
        }
        place == null -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Ustalanie położenia…", color = Palette.TextDim, fontSize = 13.sp)
        }
        else -> place?.getOrNull()?.let { PlaceCard(Icons.Rounded.MyLocation, it.name, it.region.ifBlank { it.coords }) }
            ?: Hint(place?.exceptionOrNull()?.message ?: "Nie udało się ustalić położenia")
    }
}

@Composable
private fun CityPicker(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    PlaceCard(Icons.Rounded.Place, st.city.ifBlank { "Warszawa" }, if (st.coords.isNotBlank()) st.coords else "wyszukiwane po nazwie")
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            failed = false
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        val r = runCatching { WeatherRepo.search(query) }
        results = r.getOrDefault(emptyList())
        failed = r.isFailure
        searching = false
    }
    DialogTextField(
        value = query,
        onValueChange = { query = it.take(60) },
        placeholder = "Szukaj miejscowości albo wpisz 52.23, 21.01",
        leadingIcon = Icons.Rounded.Search,
        trailing = if (searching) ({ CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) }) else null,
    )
    if (results.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f))) {
            results.forEach { pl ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        set(st.copy(city = pl.name, coords = pl.coords))
                        query = ""
                    }.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Place, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(pl.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (pl.region.isNotBlank()) Text(pl.region, color = Palette.TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    } else if (failed) Hint("Brak połączenia – nie można wyszukać.")
    else if (query.trim().length >= 2 && !searching) Hint("Nic nie znaleziono.")
    SubLabel("Szybki wybór")
    FlowChips(quickCities, quickCities.indexOfFirst { it.equals(st.city.trim(), ignoreCase = true) && st.coords.isBlank() }) {
        set(st.copy(city = quickCities[it], coords = ""))
    }
}

@Composable
private fun PlaceCard(icon: ImageVector, name: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Palette.Accent), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Palette.Ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = Palette.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun UnitOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Section("Jednostki", Icons.Rounded.Speed) {
        SubLabel("Temperatura")
        Segmented(listOf("°C", "°F"), if (st.fahrenheit) 1 else 0) { set(st.copy(fahrenheit = it == 1)) }
        SubLabel("Wiatr")
        Segmented(listOf("km/h", "m/s", "mph", "węzły"), st.windUnit.coerceIn(0, 3)) { set(st.copy(windUnit = it)) }
        SubLabel("Ciśnienie")
        Segmented(listOf("hPa", "mmHg", "inHg"), st.pressureUnit.coerceIn(0, 2)) { set(st.copy(pressureUnit = it)) }
    }
}

// ---------- Styl karty ----------

@Composable
private fun CardOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Section("Gotowe style", Icons.Rounded.Wallpaper) {
        LookGrid(st, set)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip(Icons.Rounded.ContentCopy, "Kopiuj wygląd") { copiedLook = st }
            copiedLook?.let { c -> ActionChip(Icons.Rounded.ContentPaste, "Wklej wygląd") { set(st.withAppearance(c)) } }
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

private val shadowColors = listOf(
    "Czarny" to 0xFF000000, "Grafit" to 0xFF263238, "Granat" to 0xFF0D1B3E, "Brąz" to 0xFF3E2723, "Poświata" to 0xFFFFFFFF,
)

/** Gotowy zestaw cieni: karta, treść, rozmycie, przesunięcie. */
private class ShadowLook(val card: Float, val content: Float, val blur: Float, val offset: Float) {
    fun matches(st: GadgetStyle) = st.shadow == card && st.contentShadow == content &&
        (content == 0f || (st.shadowBlur == blur && st.shadowOffset == offset))
    fun applyTo(st: GadgetStyle) = st.copy(shadow = card, contentShadow = content, shadowBlur = blur, shadowOffset = offset)
}

private val shadowLooks = listOf(
    "Brak" to ShadowLook(0f, 0f, 3f, 1.5f),
    "Delikatne" to ShadowLook(0.2f, 0.35f, 2f, 1f),
    "Miękkie" to ShadowLook(0.4f, 0.55f, 5f, 2f),
    "Wyraźne" to ShadowLook(0.6f, 0.8f, 1.5f, 2.5f),
    "Uniesione" to ShadowLook(0.9f, 0.6f, 8f, 5f),
)

/** Gotowe style jako miniatury karty: tło, kolor tekstu i akcentu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookGrid(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
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
private fun ColorOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
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
private fun TextOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Section("Wielkość i grubość", Icons.Rounded.TextFields) {
        ValueSlider("Wielkość tekstu", st.textScale * 100f, 60f..160f, { "${it.toInt()}%" }) { set(st.copy(textScale = it / 100f)) }
        if (kind.heroLabel() != null) {
            ValueSlider(kind.heroLabel()!!, st.timeScale * 100f, 50f..200f, { "${it.toInt()}%" }) { set(st.copy(timeScale = it / 100f)) }
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
private fun GadgetKind.heroLabel(): String? = when (this) {
    GadgetKind.DigitalClock, GadgetKind.ClockWeather, GadgetKind.SkyClock, GadgetKind.WorldClock -> "Wielkość godziny"
    GadgetKind.Countdown -> "Wielkość liczby dni"
    GadgetKind.DateCard -> "Wielkość liczby dnia"
    GadgetKind.MoonPhase, GadgetKind.MoonRound -> "Wielkość nazwy fazy"
    else -> null
}

private fun GadgetKind.usesAlign() = this !in setOf(
    GadgetKind.AnalogClock, GadgetKind.Forecast, GadgetKind.Calendar, GadgetKind.Progress, GadgetKind.Storage, GadgetKind.ClockWeather,
    GadgetKind.HourlyForecast, GadgetKind.WeatherFull, GadgetKind.WeatherDetails, GadgetKind.SunArc, GadgetKind.YearDots,
)

// ---------- Treść ----------

/** Ustawienia treści właściwe dla rodzaju widżetu. */
@Composable
private fun KindOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    val clock = Tile(Icons.Rounded.AccessTime, "24 h", st.use24h) { set(st.copy(use24h = it)) }
    val seconds = Tile(Icons.Rounded.Timer, "Sekundy", st.showSeconds) { set(st.copy(showSeconds = it)) }
    val date = Tile(Icons.Rounded.DateRange, "Data", st.showDate) { set(st.copy(showDate = it)) }
    fun details(label: String) = Tile(Icons.Rounded.Info, label, st.showDetails) { set(st.copy(showDetails = it)) }
    val monday = Tile(Icons.Rounded.ViewWeek, "Tydzień od poniedziałku", st.mondayFirst) { set(st.copy(mondayFirst = it)) }
    val today = Tile(Icons.Rounded.Today, "Wyróżnij teraz", st.highlightToday) { set(st.copy(highlightToday = it)) }
    val condition = Tile(Icons.Rounded.Title, "Opis warunków", st.showCondition) { set(st.copy(showCondition = it)) }
    val tomorrow = Tile(Icons.Rounded.SkipNext, "Od jutra", st.startTomorrow) { set(st.copy(startTomorrow = it)) }
    val fill = Tile(Icons.Rounded.ShowChart, "Wypełnienie wykresu", st.chartFill) { set(st.copy(chartFill = it)) }
    val summary = Tile(Icons.Rounded.Subject, "Podsumowanie", st.showSummary) { set(st.copy(showSummary = it)) }
    val realMoon = Tile(Icons.Rounded.PhotoCamera, "Księżyc ze zdjęcia", st.realMoon) { set(st.copy(realMoon = it)) }
    val southern = Tile(Icons.Rounded.Public, "Półkula południowa", st.southern) { set(st.copy(southern = it)) }
    val daysSlider: @Composable () -> Unit = {
        ValueSlider("Liczba dni", st.days.toFloat().coerceIn(1f, 16f), 1f..16f, { "${it.toInt()}" }, steps = 14) { set(st.copy(days = it.toInt())) }
    }
    val hoursSlider: @Composable () -> Unit = {
        ValueSlider("Liczba godzin", st.hours.toFloat().coerceIn(3f, 48f), 3f..48f, { "${it.toInt()} h" }, steps = 44) { set(st.copy(hours = it.toInt())) }
        SubLabel("Odstęp")
        Segmented(listOf("1 h", "2 h", "3 h", "6 h"), listOf(1, 2, 3, 6).indexOf(st.hourStep)) { set(st.copy(hourStep = listOf(1, 2, 3, 6)[it])) }
    }
    val dayNames: @Composable () -> Unit = {
        SubLabel("Nazwy dni")
        Segmented(listOf("pon.", "poniedziałek", "29.10", "pon. 29"), st.dayNames.coerceIn(0, 3)) { set(st.copy(dayNames = it)) }
    }
    val layout: @Composable (List<String>, Int) -> Unit = { options, max ->
        Section("Układ", Icons.Rounded.Wallpaper) { Segmented(options, st.mode.coerceIn(0, max)) { set(st.copy(mode = it)) } }
    }

    when (kind) {
        GadgetKind.DigitalClock -> Section("Zegar", Icons.Rounded.AccessTime) {
            Toggles(listOf(clock, seconds, date, Tile(Icons.Rounded.Battery5Bar, "Bateria", st.showBattery) { set(st.copy(showBattery = it)) }))
        }
        GadgetKind.AnalogClock -> {
            layout(listOf("Kreski", "Kropki", "Obwódka"), 2)
            Section("Tarcza", Icons.Rounded.AccessTime) { Toggles(listOf(seconds, details("Cyfry"))) }
        }
        GadgetKind.ClockWeather -> Section("Zegar i pogoda", Icons.Rounded.AccessTime) {
            Toggles(listOf(clock, seconds, date, details("Nazwa miejsca")))
        }
        GadgetKind.Weather -> {
            layout(listOf("Klasyczny", "Duża temp.", "Jedna linia"), 2)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Szczegóły"))) }
            MetricOptions(st, WeatherMetric.all, set)
        }
        GadgetKind.Forecast -> {
            layout(listOf("Kolumny", "Lista", "Wykres", "Kompakt"), 3)
            Section("Zakres", Icons.Rounded.DateRange) {
                daysSlider()
                dayNames()
            }
            Section("Pokazuj", Icons.Rounded.Info) {
                Toggles(listOf(details("Nagłówek"), tomorrow, today, condition) + if (st.mode == 2) listOf(fill) else emptyList())
            }
            MetricOptions(st, listOf(WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind, WeatherMetric.Uv), set)
        }
        GadgetKind.HourlyForecast -> {
            layout(listOf("Wykres", "Kolumny", "Lista"), 2)
            Section("Zakres", Icons.Rounded.AccessTime) { hoursSlider() }
            Section("Pokazuj", Icons.Rounded.Info) {
                Toggles(listOf(details("Nagłówek"), summary, clock, today) + if (st.mode == 0) listOf(fill) else emptyList())
            }
            MetricOptions(
                st,
                listOf(WeatherMetric.Feels, WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind, WeatherMetric.Humidity, WeatherMetric.Uv, WeatherMetric.Cloud, WeatherMetric.Pressure),
                set,
                hint = "Wartości, które się nie zmieszczą, chowają się – powiększ widżet, żeby zobaczyć więcej.",
            )
        }
        GadgetKind.WeatherFull -> {
            Section("Zakres", Icons.Rounded.DateRange, hint = "Panel sam chowa sekcje, które się nie mieszczą – powiększ widżet, żeby zobaczyć godziny i dni.") {
                hoursSlider()
                daysSlider()
                dayNames()
            }
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Odczuwalna"), clock, today, tomorrow, condition, fill)) }
            MetricOptions(st, WeatherMetric.all.filter { it != WeatherMetric.Feels }, set)
        }
        GadgetKind.WeatherDetails -> {
            layout(listOf("Kafelki", "Lista"), 1)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Nagłówek"), clock)) }
            MetricOptions(st, WeatherMetric.all, set)
        }
        GadgetKind.SunArc -> {
            layout(listOf("Łuk", "Sam tekst"), 1)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(clock, details("Szczegóły"))) }
        }
        GadgetKind.Calendar -> Section("Kalendarz", Icons.Rounded.DateRange) { Toggles(listOf(monday)) }
        GadgetKind.DateCard -> Section("Kartka", Icons.Rounded.DateRange) { Toggles(listOf(details("Rok"))) }
        GadgetKind.Battery -> {
            layout(listOf("Pierścień", "Pasek"), 1)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Opis"))) }
        }
        GadgetKind.WorldClock -> {
            Section("Miasta (do 6)", Icons.Rounded.Public) { ZonePicker(st, set) }
            layout(listOf("Obok siebie", "Lista"), 1)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(clock, seconds, details("Różnica czasu"))) }
        }
        GadgetKind.Countdown -> {
            Section("Odliczanie", Icons.Rounded.Timer) {
                DialogTextField(st.label, { set(st.copy(label = it.take(40))) }, label = "Nazwa (np. Urlop)")
                DialogTextField(st.target, { set(st.copy(target = it.take(10))) }, label = "Data RRRR-MM-DD")
                val now = LocalDate.now()
                FlowChips(listOf("Za tydzień", "Za miesiąc", "Nowy Rok", "Wigilia"), -1) { i ->
                    val d = when (i) {
                        0 -> now.plusWeeks(1)
                        1 -> now.plusMonths(1)
                        2 -> LocalDate.of(now.year + 1, 1, 1)
                        else -> LocalDate.of(now.year, 12, 24).let { if (it.isBefore(now)) it.plusYears(1) else it }
                    }
                    set(st.copy(target = d.toString(), label = st.label.ifBlank { listOf("", "", "Nowy Rok", "Wigilia")[i] }))
                }
            }
            layout(listOf("Same dni", "Dni, godziny, minuty"), 1)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(seconds, details("Data"))) }
        }
        GadgetKind.Note -> {
            Section("Notatka", Icons.Rounded.Subject) {
                DialogTextField(st.label, { set(st.copy(label = it.take(40))) }, label = "Tytuł (opcjonalnie)")
                DialogTextField(st.text, { set(st.copy(text = it.take(500))) }, label = "Treść", singleLine = false, minLines = 3)
            }
            layout(listOf("Od góry", "Wyśrodkowana"), 1)
        }
        GadgetKind.Greeting -> Section("Powitanie", Icons.Rounded.WbSunny) {
            DialogTextField(st.label, { set(st.copy(label = it.take(30))) }, label = "Twoje imię")
            Toggles(listOf(date))
        }
        GadgetKind.Progress -> {
            layout(listOf("Dzień", "Tydzień", "Miesiąc", "Rok"), 3)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Dokładnie"), monday)) }
        }
        GadgetKind.Proverb -> {
            layout(listOf("Kursywa", "Prosto"), 1)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Źródło"))) }
        }
        GadgetKind.Storage -> Section("Pamięć", Icons.Rounded.Info) { Toggles(listOf(details("Ilość GB"))) }
        GadgetKind.MoonPhase -> {
            layout(listOf("Naturalny", "Akcent", "Kolor tekstu"), 2)
            Section("Pokazuj", Icons.Rounded.Info) {
                Toggles(listOf(details("Pełnia i nów"), realMoon, southern))
            }
        }
        GadgetKind.MoonRound -> {
            Section("Kolor Księżyca", Icons.Rounded.Wallpaper) { Segmented(listOf("Naturalny", "Akcent", "Kolor tekstu"), st.mode.coerceIn(0, 2)) { set(st.copy(mode = it)) } }
            Section("Pokazuj", Icons.Rounded.Info) {
                Toggles(listOf(details("Oświetlenie %"), realMoon, southern))
            }
        }
        GadgetKind.SkyClock -> {
            Section("Zegar", Icons.Rounded.AccessTime) { Toggles(listOf(clock, seconds, date, details("Temperatura i oświetlenie"))) }
            Section("Kolor Księżyca", Icons.Rounded.Wallpaper) {
                Segmented(listOf("Naturalny", "Akcent", "Kolor tekstu"), st.mode.coerceIn(0, 2)) { set(st.copy(mode = it)) }
                Toggles(listOf(realMoon, southern))
            }
        }
        GadgetKind.Steps -> {
            layout(listOf("Pierścień", "Pasek", "Tydzień"), 2)
            Section("Cel", Icons.Rounded.Info) {
                ValueSlider("Dzienny cel kroków", st.goal.toFloat().coerceIn(2000f, 20000f), 2000f..20000f, { "%,d".format(it.toInt()) }, steps = 35) {
                    set(st.copy(goal = ((it / 500f).toInt() * 500).coerceIn(2000, 20000)))
                }
                Toggles(listOf(details("Kalorie i dystans")))
            }
        }
        GadgetKind.YearDots -> {
            layout(listOf("Rok", "Miesiąc"), 1)
            Section("Pokazuj", Icons.Rounded.Info) {
                DialogTextField(st.label, { set(st.copy(label = it.take(30))) }, label = "Tytuł (opcjonalnie)")
                Toggles(listOf(details("Licznik dni")))
            }
        }
    }
}

/** Wygląd wspólny dla widżetów pogody: tło wg pogody, ikony, kolory temperatur. */
@Composable
private fun WeatherLookOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Section("Wygląd pogody", Icons.Rounded.WbSunny) {
        Toggles(
            listOf(
                Tile(Icons.Rounded.Wallpaper, "Tło wg pogody", st.weatherBackground) { on ->
                    // Włączenie na prawie przezroczystym tle nic by nie zmieniło – podnosimy krycie
                    val alpha = (st.background ushr 24) and 0xFF
                    val bg = if (on && alpha < 0x99) (0xD9L shl 24) or (st.background and 0xFFFFFF) else st.background
                    set(st.copy(weatherBackground = on, background = bg))
                },
                Tile(Icons.Rounded.Thermostat, "Kolorowe temperatury", st.tempColors) { set(st.copy(tempColors = it)) },
                Tile(Icons.Rounded.Numbers, "Dziesiąte części", st.decimals) { set(st.copy(decimals = it)) },
                Tile(Icons.Rounded.Update, "Godzina aktualizacji", st.showUpdated) { set(st.copy(showUpdated = it)) },
            ) + if (kind in setOf(GadgetKind.Forecast, GadgetKind.HourlyForecast, GadgetKind.WeatherFull, GadgetKind.WeatherDetails)) {
                listOf(Tile(Icons.Rounded.HorizontalRule, "Linie podziału", st.separators) { set(st.copy(separators = it)) })
            } else emptyList()
        )
        SubLabel("Ikony")
        Segmented(iconStyles, st.iconStyle.coerceIn(0, 3)) { set(st.copy(iconStyle = it)) }
        ValueSlider("Wielkość ikon", st.iconScale * 100f, 50f..200f, { "${it.toInt()}%" }) { set(st.copy(iconScale = it / 100f)) }
    }
}

private val metricTiles = mapOf(
    WeatherMetric.Feels to (Icons.Rounded.Thermostat to "Odczuwalna"),
    WeatherMetric.Wind to (Icons.Rounded.Air to "Wiatr"),
    WeatherMetric.Humidity to (Icons.Rounded.Opacity to "Wilgotność"),
    WeatherMetric.Pressure to (Icons.Rounded.Speed to "Ciśnienie"),
    WeatherMetric.Uv to (Icons.Rounded.WbSunny to "Indeks UV"),
    WeatherMetric.Precip to (Icons.Rounded.Umbrella to "Szansa opadów"),
    WeatherMetric.Rain to (Icons.Rounded.WaterDrop to "Suma opadów"),
    WeatherMetric.Cloud to (Icons.Rounded.Cloud to "Zachmurzenie"),
    WeatherMetric.Sun to (Icons.Rounded.WbTwilight to "Wschód i zachód"),
)

@Composable
private fun MetricOptions(st: GadgetStyle, available: List<String>, set: (GadgetStyle) -> Unit, hint: String? = null) {
    Section("Wartości", Icons.Rounded.Thermostat, hint = hint) {
        Toggles(
            available.mapNotNull { m ->
                val (icon, label) = metricTiles[m] ?: return@mapNotNull null
                Tile(icon, label, m in st.metrics) { on -> set(st.copy(metrics = if (on) st.metrics + m else st.metrics - m)) }
            }
        )
    }
}

@Composable
private fun ZonePicker(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Toggles(
        WorldZones.map { (id, name) ->
            Tile(Icons.Rounded.Public, name, id in st.zones) { on -> set(st.copy(zones = if (on) (st.zones + id).takeLast(6) else st.zones - id)) }
        },
        showIcons = false,
    )
}

// ---------- Wybór koloru ----------

private fun hsv(c: Long): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toInt(), it) }

private fun fromHsv(h: Float, s: Float, v: Float): Long = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFF

private fun hex(c: Long) = "#%06X".format(c and 0xFFFFFF)

/**
 * Wybór koloru: próbki i „własny” – suwaki odcienia, nasycenia i jasności oraz pole hex.
 * Zwraca kolor nieprzezroczysty; przezroczystość ustawia wywołujący.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(
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
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
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
private fun Section(title: String, icon: ImageVector, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
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
private fun SubLabel(text: String) {
    Text(text, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 16.sp)
}

/** Przełącznik segmentowy – wszystkie opcje widać naraz, bez przewijania w bok. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
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
private fun FlowChips(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, name -> Chip(name, i == selected) { onPick(i) } }
    }
}

@Composable
private fun Chip(name: String, selected: Boolean, family: FontFamily? = null, onClick: () -> Unit) {
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
private fun Toggles(tiles: List<Tile>, showIcons: Boolean = true) {
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
private fun ValueSlider(
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
private fun ActionChip(icon: ImageVector, text: String, primary: Boolean = false, onClick: () -> Unit) {
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
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(20.dp)) }
}
