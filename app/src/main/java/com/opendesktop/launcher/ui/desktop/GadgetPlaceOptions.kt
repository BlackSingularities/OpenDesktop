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

// ---------- Miejsce (pogoda) ----------

@Composable
internal fun PlaceOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
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
internal fun DevicePlace(granted: Boolean, onAsk: () -> Unit) {
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
internal fun CityPicker(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
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
internal fun PlaceCard(icon: ImageVector, name: String, detail: String) {
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
internal fun UnitOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Section("Jednostki", Icons.Rounded.Speed) {
        SubLabel("Temperatura")
        Segmented(listOf("°C", "°F"), if (st.fahrenheit) 1 else 0) { set(st.copy(fahrenheit = it == 1)) }
        SubLabel("Wiatr")
        Segmented(listOf("km/h", "m/s", "mph", "węzły"), st.windUnit.coerceIn(0, 3)) { set(st.copy(windUnit = it)) }
        SubLabel("Ciśnienie")
        Segmented(listOf("hPa", "mmHg", "inHg"), st.pressureUnit.coerceIn(0, 2)) { set(st.copy(pressureUnit = it)) }
    }
}
