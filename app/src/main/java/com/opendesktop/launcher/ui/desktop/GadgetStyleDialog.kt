package com.opendesktop.launcher.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Padding
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.GadgetKind
import com.opendesktop.launcher.data.GadgetStyle
import com.opendesktop.launcher.ui.settings.DarkDialog
import com.opendesktop.launcher.ui.settings.PillButton
import com.opendesktop.launcher.ui.settings.SliderRow
import com.opendesktop.launcher.ui.settings.Tile
import com.opendesktop.launcher.ui.settings.TileGrid
import com.opendesktop.launcher.ui.theme.Palette
import java.time.LocalDate

/** Gotowe zestawy wyglądu – zmieniają tylko kolory, rogi, obrys i cienie, nie ustawienia treści. */
private val looks = listOf(
    "Szkło" to GadgetStyle(),
    "Bez tła" to GadgetStyle(background = 0x00000000, border = false, textShadow = true),
    "Ciemny" to GadgetStyle(background = 0xB3101418, border = false, shadow = 0.4f, accent = 0xFF8AB4FF),
    "Jasny" to GadgetStyle(background = 0xF2FFFFFF, textColor = 0xFF202124, accent = 0xFF1A73E8, border = false, shadow = 0.5f),
    "Neon" to GadgetStyle(background = 0xCC0B0F1A, textColor = 0xFF80DEEA, accent = 0xFFFF4081, border = true),
    "Ciepły" to GadgetStyle(background = 0xE6FFF3E0, textColor = 0xFF4E342E, accent = 0xFFFF7043, border = false, shadow = 0.3f),
    "Las" to GadgetStyle(background = 0xD91B3A2B, textColor = 0xFFE8F5E9, accent = 0xFFA5D6A7, border = false),
    "Ocean" to GadgetStyle(background = 0xD90D2847, textColor = 0xFFE3F2FD, accent = 0xFF4FC3F7, border = false, shadow = 0.3f),
    "Róż" to GadgetStyle(background = 0xE6FCE4EC, textColor = 0xFF880E4F, accent = 0xFFEC407A, border = false),
)

private fun GadgetStyle.withLook(o: GadgetStyle) = copy(
    background = o.background, radius = o.radius, textColor = o.textColor, accent = o.accent,
    border = o.border, shadow = o.shadow, textShadow = o.textShadow,
)

private val textColors = listOf(0xFFFFFFFF, 0xFF202124, 0xFFFFD54F, 0xFF80DEEA, 0xFFA5D6A7, 0xFFF48FB1, 0xFFB39DDB, 0xFFFFAB91)
private val accentColors = listOf(0xFF8AB4FF, 0xFF1A73E8, 0xFFFF7043, 0xFF69F0AE, 0xFFFFD54F, 0xFFEC407A, 0xFFBA68C8, 0xFF4DD0E1, 0xFFFFFFFF)
private val backgroundColors = listOf(0xFF000000, 0xFFFFFFFF, 0xFF263238, 0xFF1E88E5, 0xFF43A047, 0xFFE53935, 0xFF8E24AA, 0xFFFFB300, 0xFF0D2847)

private val weights = listOf("Cienka" to 200, "Lekka" to 300, "Zwykła" to 400, "Gruba" to 600, "Czarna" to 800)
private val aligns = listOf("Do lewej" to 0, "Środek" to 1, "Do prawej" to 2)

/** Proporcje podglądu w oknie (szerokość / wysokość kafelka). */
private fun previewHeight(kind: GadgetKind) = when {
    kind.h >= 3 -> 210.dp
    kind.h == 1 -> 76.dp
    kind.w <= 2 -> 170.dp
    else -> 140.dp
}

/**
 * Dostosowanie wbudowanego widżetu: podgląd na żywo, zestawy, kolory, rogi, cienie, czcionka
 * i ustawienia właściwe dla rodzaju (format godziny, miasto, strefy, data odliczania…).
 */
@Composable
fun GadgetStyleDialog(kind: GadgetKind, initial: GadgetStyle, onDismiss: () -> Unit, onSave: (GadgetStyle) -> Unit) {
    var st by remember { mutableStateOf(initial) }
    DarkDialog(
        title = kind.label,
        onDismiss = onDismiss,
        buttons = {
            PillButton("Domyślne", primary = false) { st = kind.defaultStyle() }
            PillButton("Anuluj", primary = false, onClick = onDismiss)
            PillButton("Zapisz", onClick = { onSave(st) })
        },
    ) {
        // Podgląd na przejściu od jasnej do ciemnej tapety
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFF3EFE6), Color(0xFF7A8FA6), Color(0xFF0E1420))))
                .padding(8.dp),
        ) {
            GadgetWidget(kind, st, Modifier.fillMaxWidth().height(previewHeight(kind)))
        }

        KindOptions(kind, st) { st = it }

        Label("Wygląd")
        Chips(looks.map { it.first }, looks.indexOfFirst { st.withLook(it.second) == st }) { st = st.withLook(looks[it].second) }

        Label("Kolor tekstu")
        Swatches(textColors, st.textColor) { st = st.copy(textColor = it) }
        Label("Kolor akcentu")
        Swatches(accentColors, st.accent) { st = st.copy(accent = it) }
        Label("Kolor tła")
        Swatches(backgroundColors, st.background or 0xFF000000) { c ->
            val alpha = ((st.background ushr 24) and 0xFF).let { if (it == 0L) 0x66L else it }
            st = st.copy(background = (alpha shl 24) or (c and 0xFFFFFF))
        }
        SliderRow(Icons.Rounded.Opacity, "Krycie tła", ((st.background ushr 24) and 0xFF) / 255f * 100f, 0f..100f, 19, { "${it.toInt()}%" }) { a ->
            val alpha = (a / 100f * 255f).toLong().coerceIn(0, 255)
            st = st.copy(background = (alpha shl 24) or (st.background and 0xFFFFFF))
        }
        SliderRow(Icons.Rounded.RoundedCorner, "Zaokrąglenie rogów", st.radius.toFloat(), 0f..48f, 11, { "${it.toInt()} dp" }) {
            st = st.copy(radius = it.toInt())
        }
        SliderRow(Icons.Rounded.BlurOn, "Cień pod kartą", st.shadow * 100f, 0f..100f, 9, { "${it.toInt()}%" }) {
            st = st.copy(shadow = it / 100f)
        }
        SliderRow(Icons.Rounded.Padding, "Margines wewnętrzny", st.padding.toFloat(), 0f..32f, 7, { "${it.toInt()} dp" }) {
            st = st.copy(padding = it.toInt())
        }
        TileGrid(
            listOf(
                Tile(Icons.Rounded.Check, "Obrys", st.border) { v -> st = st.copy(border = v) },
                Tile(Icons.Rounded.BlurOn, "Cień tekstu", st.textShadow) { v -> st = st.copy(textShadow = v) },
            )
        )

        Label("Tekst")
        SliderRow(Icons.Rounded.TextFields, "Wielkość tekstu", st.textScale * 100f, 60f..160f, 9, { "${it.toInt()}%" }) {
            st = st.copy(textScale = it / 100f)
        }
        Chips(weights.map { it.first }, weights.indexOfFirst { it.second == st.weight }) { st = st.copy(weight = weights[it].second) }
        if (kind.usesAlign()) {
            Chips(aligns.map { it.first }, aligns.indexOfFirst { it.second == st.align }) { st = st.copy(align = aligns[it].second) }
        }
    }
}

private fun GadgetKind.usesAlign() = this !in setOf(GadgetKind.AnalogClock, GadgetKind.Forecast, GadgetKind.Calendar, GadgetKind.Progress, GadgetKind.Storage, GadgetKind.ClockWeather)

/** Ustawienia treści właściwe dla rodzaju widżetu. */
@Composable
private fun KindOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    val clock = Tile(Icons.Rounded.AccessTime, "24 h", st.use24h) { set(st.copy(use24h = it)) }
    val seconds = Tile(Icons.Rounded.Timer, "Sekundy", st.showSeconds) { set(st.copy(showSeconds = it)) }
    val date = Tile(Icons.Rounded.DateRange, "Data", st.showDate) { set(st.copy(showDate = it)) }
    fun details(label: String) = Tile(Icons.Rounded.Info, label, st.showDetails) { set(st.copy(showDetails = it)) }
    val units = Tile(Icons.Rounded.Thermostat, "°F", st.fahrenheit) { set(st.copy(fahrenheit = it)) }
    val monday = Tile(Icons.Rounded.ViewWeek, "Od pon.", st.mondayFirst) { set(st.copy(mondayFirst = it)) }

    when (kind) {
        GadgetKind.DigitalClock -> {
            Label("Zegar")
            TileGrid(listOf(clock, seconds, date, Tile(Icons.Rounded.Battery5Bar, "Bateria", st.showBattery) { set(st.copy(showBattery = it)) }))
        }
        GadgetKind.AnalogClock -> {
            Label("Tarcza")
            Chips(listOf("Kreski", "Kropki", "Obwódka"), st.mode.coerceIn(0, 2)) { set(st.copy(mode = it)) }
            TileGrid(listOf(seconds, details("Cyfry")))
        }
        GadgetKind.ClockWeather -> {
            Label("Zegar i pogoda")
            CityField(st, set)
            TileGrid(listOf(clock, seconds, date, details("Miasto"), units))
        }
        GadgetKind.Weather -> {
            Label("Pogoda")
            CityField(st, set)
            TileGrid(listOf(details("Szczegóły"), units))
        }
        GadgetKind.Forecast -> {
            Label("Prognoza")
            CityField(st, set)
            SliderRow(Icons.Rounded.CalendarMonth, "Liczba dni", st.days.toFloat(), 2f..7f, 4, { "${it.toInt()}" }) { set(st.copy(days = it.toInt())) }
            TileGrid(listOf(details("Nagłówek"), units))
        }
        GadgetKind.Calendar -> {
            Label("Kalendarz")
            TileGrid(listOf(monday))
        }
        GadgetKind.DateCard -> {
            Label("Kartka")
            TileGrid(listOf(details("Rok")))
        }
        GadgetKind.Battery -> {
            Label("Bateria")
            Chips(listOf("Pierścień", "Pasek"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
            TileGrid(listOf(details("Opis")))
        }
        GadgetKind.WorldClock -> {
            Label("Miasta (do 6)")
            ZonePicker(st, set)
            Chips(listOf("Obok siebie", "Lista"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
            TileGrid(listOf(clock, seconds, details("Różnica")))
        }
        GadgetKind.Countdown -> {
            Label("Odliczanie")
            DarkField(st.label, "Nazwa (np. Urlop)") { set(st.copy(label = it.take(40))) }
            DarkField(st.target, "Data RRRR-MM-DD") { set(st.copy(target = it.take(10))) }
            val today = LocalDate.now()
            Chips(listOf("Za tydzień", "Za miesiąc", "Nowy Rok", "Wigilia"), -1) { i ->
                val d = when (i) {
                    0 -> today.plusWeeks(1)
                    1 -> today.plusMonths(1)
                    2 -> LocalDate.of(today.year + 1, 1, 1)
                    else -> LocalDate.of(today.year, 12, 24).let { if (it.isBefore(today)) it.plusYears(1) else it }
                }
                set(st.copy(target = d.toString(), label = st.label.ifBlank { listOf("", "", "Nowy Rok", "Wigilia")[i] }))
            }
            Chips(listOf("Same dni", "Dni, godziny, minuty"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
            TileGrid(listOf(seconds, details("Data")))
        }
        GadgetKind.Note -> {
            Label("Notatka")
            DarkField(st.label, "Tytuł (opcjonalnie)") { set(st.copy(label = it.take(40))) }
            DarkField(st.text, "Treść", singleLine = false) { set(st.copy(text = it.take(500))) }
            Chips(listOf("Od góry", "Wyśrodkowana"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
        }
        GadgetKind.Greeting -> {
            Label("Powitanie")
            DarkField(st.label, "Twoje imię") { set(st.copy(label = it.take(30))) }
            TileGrid(listOf(date))
        }
        GadgetKind.Progress -> {
            Label("Okres")
            Chips(listOf("Dzień", "Tydzień", "Miesiąc", "Rok"), st.mode.coerceIn(0, 3)) { set(st.copy(mode = it)) }
            TileGrid(listOf(details("Dokładnie"), monday))
        }
        GadgetKind.Proverb -> {
            Label("Przysłowie")
            Chips(listOf("Kursywa", "Prosto"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
            TileGrid(listOf(details("Źródło")))
        }
        GadgetKind.Storage -> {
            Label("Pamięć")
            TileGrid(listOf(details("Ilość GB")))
        }
    }
}

@Composable
private fun CityField(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    DarkField(st.city, "Miasto") { set(st.copy(city = it.take(60))) }
}

@Composable
private fun ZonePicker(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        WorldZones.forEach { (id, name) ->
            val on = id in st.zones
            Chip(name, on) {
                val next = if (on) st.zones - id else (st.zones + id).takeLast(6)
                set(st.copy(zones = next))
            }
        }
    }
}

@Composable
private fun Chips(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, name -> Chip(name, i == selected) { onPick(i) } }
    }
}

@Composable
private fun Chip(name: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        name,
        color = if (selected) Palette.Ink else Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Palette.Accent else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun DarkField(value: String, label: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Palette.TextDim,
            cursorColor = Color.White,
        ),
    )
}
