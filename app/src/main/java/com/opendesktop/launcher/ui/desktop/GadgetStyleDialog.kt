package com.opendesktop.launcher.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LineWeight
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Padding
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.GadgetKind
import com.opendesktop.launcher.data.GadgetStyle
import com.opendesktop.launcher.data.WeatherMetric
import com.opendesktop.launcher.ui.settings.DarkDialog
import com.opendesktop.launcher.ui.settings.PillButton
import com.opendesktop.launcher.ui.settings.SliderRow
import com.opendesktop.launcher.ui.settings.Tile
import com.opendesktop.launcher.ui.settings.TileGrid
import com.opendesktop.launcher.ui.theme.Palette
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
    padding = o.padding, textScale = o.textScale, weight = o.weight, fontFamily = o.fontFamily,
    iconStyle = o.iconStyle, iconScale = o.iconScale, tempColors = o.tempColors, weatherBackground = o.weatherBackground,
    separators = o.separators,
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
private val iconStyles = listOf("Akcent", "Kolorowe", "Kolor tekstu", "Emoji")
private val quickCities = listOf("Warszawa", "Kraków", "Gdańsk", "Wrocław", "Poznań", "Łódź", "Szczecin", "Lublin", "Zakopane")

/** Proporcje podglądu w oknie (szerokość / wysokość kafelka). */
private fun previewHeight(kind: GadgetKind) = when {
    kind.h >= 4 -> 250.dp
    kind.h >= 3 -> 210.dp
    kind.h == 1 -> 76.dp
    kind.w <= 2 -> 170.dp
    else -> 140.dp
}

/**
 * Dostosowanie wbudowanego widżetu: stały podgląd na żywo i cztery karty – treść (ustawienia
 * właściwe dla rodzaju), wygląd karty, kolory (z własnym kolorem HSV/hex) i tekst.
 */
@Composable
fun GadgetStyleDialog(kind: GadgetKind, initial: GadgetStyle, onDismiss: () -> Unit, onSave: (GadgetStyle) -> Unit) {
    var st by remember { mutableStateOf(initial) }
    var tab by remember { mutableIntStateOf(0) }
    DarkDialog(
        title = kind.label,
        subtitle = kind.description,
        onDismiss = onDismiss,
        scrollable = false,
        buttons = {
            PillButton("Domyślne", primary = false) { st = kind.defaultStyle() }
            PillButton("Anuluj", primary = false, onClick = onDismiss)
            PillButton("Zapisz", onClick = { onSave(st) })
        },
    ) {
        // Podgląd na przejściu od jasnej do ciemnej tapety – zostaje na górze przy przewijaniu
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFF3EFE6), Color(0xFF7A8FA6), Color(0xFF0E1420))))
                .padding(8.dp),
        ) {
            GadgetWidget(kind, st, Modifier.fillMaxWidth().height(previewHeight(kind)))
        }
        Chips(listOf("Treść", "Wygląd", "Kolory", "Tekst"), tab) { tab = it }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (tab) {
                0 -> {
                    KindOptions(kind, st) { st = it }
                    if (kind.isWeather) WeatherLookOptions(kind, st) { st = it }
                }
                1 -> CardOptions(st) { st = it }
                2 -> ColorOptions(st) { st = it }
                else -> TextOptions(kind, st) { st = it }
            }
        }
    }
}

@Composable
private fun CardOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Label("Gotowe style")
    Chips(looks.map { it.first }, looks.indexOfFirst { st.withLook(it.second) == st }) { set(st.withLook(looks[it].second)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconChip(Icons.Rounded.ContentCopy, "Kopiuj wygląd") { copiedLook = st }
        copiedLook?.let { c -> IconChip(Icons.Rounded.ContentPaste, "Wklej wygląd") { set(st.withAppearance(c)) } }
    }

    Label("Tło")
    Chips(gradients, st.gradient.coerceIn(0, 4)) { set(st.copy(gradient = it)) }
    SliderRow(Icons.Rounded.Opacity, "Krycie tła", ((st.background ushr 24) and 0xFF) / 255f * 100f, 0f..100f, 19, { "${it.toInt()}%" }) { a ->
        val alpha = (a / 100f * 255f).toLong().coerceIn(0, 255)
        // Ta sama przezroczystość dla obu kolorów gradientu
        set(st.copy(background = (alpha shl 24) or (st.background and 0xFFFFFF), background2 = (alpha shl 24) or (st.background2 and 0xFFFFFF)))
    }
    SliderRow(Icons.Rounded.RoundedCorner, "Zaokrąglenie rogów", st.radius.toFloat(), 0f..48f, 11, { "${it.toInt()} dp" }) {
        set(st.copy(radius = it.toInt()))
    }
    SliderRow(Icons.Rounded.BlurOn, "Cień pod kartą", st.shadow * 100f, 0f..100f, 9, { "${it.toInt()}%" }) {
        set(st.copy(shadow = it / 100f))
    }
    SliderRow(Icons.Rounded.Padding, "Margines wewnętrzny", st.padding.toFloat(), 0f..32f, 7, { "${it.toInt()} dp" }) {
        set(st.copy(padding = it.toInt()))
    }

    Label("Obrys i cienie")
    TileGrid(
        listOf(
            Tile(Icons.Rounded.BorderStyle, "Obrys", st.border) { v -> set(st.copy(border = v)) },
            Tile(Icons.Rounded.BlurOn, "Cień tekstu", st.textShadow) { v -> set(st.copy(textShadow = v)) },
        )
    )
    if (st.border) {
        SliderRow(Icons.Rounded.LineWeight, "Grubość obrysu", st.borderWidth, 0.5f..4f, 6, { "%.1f dp".format(it) }) {
            set(st.copy(borderWidth = it))
        }
    }
}

@Composable
private fun ColorOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    ColorPicker("Kolor tekstu", st.textColor, textColors) { set(st.copy(textColor = it)) }
    ColorPicker("Kolor akcentu", st.accent, accentColors) { set(st.copy(accent = it)) }
    val alpha = ((st.background ushr 24) and 0xFF).let { if (it == 0L) 0x66L else it }
    ColorPicker(if (st.gradient > 0) "Tło – pierwszy kolor" else "Kolor tła", st.background or 0xFF000000, backgroundColors) { c ->
        set(st.copy(background = (alpha shl 24) or (c and 0xFFFFFF)))
    }
    if (st.gradient > 0) {
        ColorPicker("Tło – drugi kolor", st.background2 or 0xFF000000, backgroundColors) { c ->
            set(st.copy(background2 = (alpha shl 24) or (c and 0xFFFFFF)))
        }
    }
    if (st.border) {
        ColorPicker("Kolor obrysu", if (st.borderColor == 0L) st.textColor else st.borderColor, accentColors, auto = st.borderColor == 0L, onAuto = { set(st.copy(borderColor = 0)) }) {
            set(st.copy(borderColor = (0xCCL shl 24) or (it and 0xFFFFFF)))
        }
    }
    if (st.weatherBackground) DialogHint("Tło zależy teraz od pogody – kolory tła są używane, gdy wyłączysz „Tło wg pogody”.")
}

@Composable
private fun TextOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Label("Wielkość")
    SliderRow(Icons.Rounded.TextFields, "Wielkość tekstu", st.textScale * 100f, 60f..160f, 9, { "${it.toInt()}%" }) {
        set(st.copy(textScale = it / 100f))
    }
    Label("Grubość")
    Chips(weights.map { it.first }, weights.indexOfFirst { it.second == st.weight }) { set(st.copy(weight = weights[it].second)) }
    Label("Krój pisma")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        fonts.forEachIndexed { i, name ->
            val family = when (i) {
                1 -> FontFamily.Serif
                2 -> FontFamily.Monospace
                3 -> FontFamily.Cursive
                else -> FontFamily.SansSerif
            }
            Chip(name, st.fontFamily == i, family) { set(st.copy(fontFamily = i)) }
        }
    }
    if (kind.usesAlign()) {
        Label("Wyrównanie")
        Chips(aligns.map { it.first }, aligns.indexOfFirst { it.second == st.align }) { set(st.copy(align = aligns[it].second)) }
    }
}

private fun GadgetKind.usesAlign() = this !in setOf(
    GadgetKind.AnalogClock, GadgetKind.Forecast, GadgetKind.Calendar, GadgetKind.Progress, GadgetKind.Storage, GadgetKind.ClockWeather,
    GadgetKind.HourlyForecast, GadgetKind.WeatherFull, GadgetKind.WeatherDetails, GadgetKind.SunArc, GadgetKind.YearDots,
)

/** Ustawienia treści właściwe dla rodzaju widżetu. */
@Composable
private fun KindOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    val clock = Tile(Icons.Rounded.AccessTime, "24 h", st.use24h) { set(st.copy(use24h = it)) }
    val seconds = Tile(Icons.Rounded.Timer, "Sekundy", st.showSeconds) { set(st.copy(showSeconds = it)) }
    val date = Tile(Icons.Rounded.DateRange, "Data", st.showDate) { set(st.copy(showDate = it)) }
    fun details(label: String) = Tile(Icons.Rounded.Info, label, st.showDetails) { set(st.copy(showDetails = it)) }
    val monday = Tile(Icons.Rounded.ViewWeek, "Od pon.", st.mondayFirst) { set(st.copy(mondayFirst = it)) }
    val today = Tile(Icons.Rounded.Today, "Wyróżnij teraz", st.highlightToday) { set(st.copy(highlightToday = it)) }
    val condition = Tile(Icons.Rounded.Title, "Opis warunków", st.showCondition) { set(st.copy(showCondition = it)) }
    val tomorrow = Tile(Icons.Rounded.SkipNext, "Od jutra", st.startTomorrow) { set(st.copy(startTomorrow = it)) }
    val fill = Tile(Icons.Rounded.ShowChart, "Wypełnienie", st.chartFill) { set(st.copy(chartFill = it)) }
    val daysSlider: @Composable () -> Unit = {
        SliderRow(Icons.Rounded.CalendarMonth, "Liczba dni", st.days.toFloat().coerceIn(1f, 16f), 1f..16f, 14, { "${it.toInt()}" }) { set(st.copy(days = it.toInt())) }
    }
    val hoursSlider: @Composable () -> Unit = {
        SliderRow(Icons.Rounded.Schedule, "Liczba godzin", st.hours.toFloat().coerceIn(3f, 48f), 3f..48f, 44, { "${it.toInt()} h" }) { set(st.copy(hours = it.toInt())) }
        Chips(listOf("Co godzinę", "Co 2 h", "Co 3 h", "Co 6 h"), listOf(1, 2, 3, 6).indexOf(st.hourStep)) { set(st.copy(hourStep = listOf(1, 2, 3, 6)[it])) }
    }
    val dayNames: @Composable () -> Unit = {
        Label("Nazwy dni")
        Chips(listOf("pon.", "poniedziałek", "29.10", "pon. 29"), st.dayNames.coerceIn(0, 3)) { set(st.copy(dayNames = it)) }
    }

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
            TileGrid(listOf(clock, seconds, date, details("Miasto")))
            UnitOptions(st, set)
        }
        GadgetKind.Weather -> {
            Label("Pogoda teraz")
            CityField(st, set)
            Chips(listOf("Klasyczny", "Duża temperatura", "Jedna linia"), st.mode.coerceIn(0, 2)) { set(st.copy(mode = it)) }
            TileGrid(listOf(details("Szczegóły")))
            MetricOptions(st, WeatherMetric.all, set)
            UnitOptions(st, set)
        }
        GadgetKind.Forecast -> {
            Label("Prognoza na dni")
            CityField(st, set)
            Chips(listOf("Kolumny", "Lista z paskami", "Wykres", "Kompaktowy"), st.mode.coerceIn(0, 3)) { set(st.copy(mode = it)) }
            daysSlider()
            dayNames()
            TileGrid(listOf(details("Nagłówek"), tomorrow, today, condition) + if (st.mode == 2) listOf(fill) else emptyList())
            MetricOptions(st, listOf(WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind, WeatherMetric.Uv), set)
            UnitOptions(st, set)
        }
        GadgetKind.HourlyForecast -> {
            Label("Prognoza godzinowa")
            CityField(st, set)
            Chips(listOf("Wykres", "Kolumny", "Lista"), st.mode.coerceIn(0, 2)) { set(st.copy(mode = it)) }
            hoursSlider()
            TileGrid(listOf(details("Nagłówek"), clock, today, fill))
            MetricOptions(st, listOf(WeatherMetric.Precip, WeatherMetric.Wind), set)
            UnitOptions(st, set)
        }
        GadgetKind.WeatherFull -> {
            Label("Pełny panel")
            CityField(st, set)
            DialogHint("Panel sam chowa sekcje, które się nie mieszczą – powiększ widżet, żeby zobaczyć godziny i dni.")
            hoursSlider()
            daysSlider()
            dayNames()
            TileGrid(listOf(details("Odczuwalna"), clock, today, tomorrow, condition, fill))
            MetricOptions(st, WeatherMetric.all.filter { it != WeatherMetric.Feels }, set)
            UnitOptions(st, set)
        }
        GadgetKind.WeatherDetails -> {
            Label("Szczegóły pogody")
            CityField(st, set)
            Chips(listOf("Kafelki", "Lista"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
            TileGrid(listOf(details("Nagłówek"), clock))
            MetricOptions(st, WeatherMetric.all, set)
            UnitOptions(st, set)
        }
        GadgetKind.SunArc -> {
            Label("Słońce")
            CityField(st, set)
            Chips(listOf("Łuk", "Sam tekst"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
            TileGrid(listOf(clock, details("Szczegóły")))
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
            val now = LocalDate.now()
            Chips(listOf("Za tydzień", "Za miesiąc", "Nowy Rok", "Wigilia"), -1) { i ->
                val d = when (i) {
                    0 -> now.plusWeeks(1)
                    1 -> now.plusMonths(1)
                    2 -> LocalDate.of(now.year + 1, 1, 1)
                    else -> LocalDate.of(now.year, 12, 24).let { if (it.isBefore(now)) it.plusYears(1) else it }
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
        GadgetKind.MoonPhase -> {
            Label("Księżyc")
            Chips(listOf("Naturalny", "W kolorze akcentu", "W kolorze tekstu"), st.mode.coerceIn(0, 2)) { set(st.copy(mode = it)) }
            TileGrid(
                listOf(
                    details("Pełnia i nów"),
                    Tile(Icons.Rounded.Public, "Półkula płd.", st.southern) { set(st.copy(southern = it)) },
                )
            )
        }
        GadgetKind.YearDots -> {
            Label("Kropki")
            Chips(listOf("Rok", "Miesiąc"), st.mode.coerceIn(0, 1)) { set(st.copy(mode = it)) }
            DarkField(st.label, "Tytuł (opcjonalnie)") { set(st.copy(label = it.take(30))) }
            TileGrid(listOf(details("Licznik dni")))
        }
    }
}

/** Wygląd wspólny dla widżetów pogody: tło wg pogody, ikony, kolory temperatur. */
@Composable
private fun WeatherLookOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Label("Wygląd pogody")
    TileGrid(
        listOf(
            Tile(Icons.Rounded.Wallpaper, "Tło wg pogody", st.weatherBackground) { on ->
                // Włączenie na prawie przezroczystym tle nic by nie zmieniło – podnosimy krycie
                val alpha = (st.background ushr 24) and 0xFF
                val bg = if (on && alpha < 0x99) (0xD9L shl 24) or (st.background and 0xFFFFFF) else st.background
                set(st.copy(weatherBackground = on, background = bg))
            },
            Tile(Icons.Rounded.Thermostat, "Kolorowe temp.", st.tempColors) { set(st.copy(tempColors = it)) },
            Tile(Icons.Rounded.Numbers, "Dziesiąte części", st.decimals) { set(st.copy(decimals = it)) },
            Tile(Icons.Rounded.Update, "Godzina aktual.", st.showUpdated) { set(st.copy(showUpdated = it)) },
        ) + if (kind in setOf(GadgetKind.Forecast, GadgetKind.HourlyForecast, GadgetKind.WeatherFull, GadgetKind.WeatherDetails)) {
            listOf(Tile(Icons.Rounded.HorizontalRule, "Linie podziału", st.separators) { set(st.copy(separators = it)) })
        } else emptyList()
    )
    Label("Ikony")
    Chips(iconStyles, st.iconStyle.coerceIn(0, 3)) { set(st.copy(iconStyle = it)) }
    SliderRow(Icons.Rounded.ZoomIn, "Wielkość ikon", st.iconScale * 100f, 50f..200f, 14, { "${it.toInt()}%" }) {
        set(st.copy(iconScale = it / 100f))
    }
}

@Composable
private fun UnitOptions(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Label("Jednostki")
    Chips(listOf("°C", "°F"), if (st.fahrenheit) 1 else 0) { set(st.copy(fahrenheit = it == 1)) }
    Chips(listOf("km/h", "m/s", "mph", "węzły"), st.windUnit.coerceIn(0, 3)) { set(st.copy(windUnit = it)) }
    if (WeatherMetric.Pressure in st.metrics) {
        Chips(listOf("hPa", "mmHg", "inHg"), st.pressureUnit.coerceIn(0, 2)) { set(st.copy(pressureUnit = it)) }
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
private fun MetricOptions(st: GadgetStyle, available: List<String>, set: (GadgetStyle) -> Unit) {
    Label("Pokazywane wartości")
    TileGrid(
        available.mapNotNull { m ->
            val (icon, label) = metricTiles[m] ?: return@mapNotNull null
            Tile(icon, label, m in st.metrics) { on -> set(st.copy(metrics = if (on) st.metrics + m else st.metrics - m)) }
        }
    )
}

@Composable
private fun CityField(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    DarkField(st.city, "Miasto albo współrzędne (52.23, 21.01)") { set(st.copy(city = it.take(60))) }
    Chips(quickCities, quickCities.indexOfFirst { it.equals(st.city.trim(), ignoreCase = true) }) { set(st.copy(city = quickCities[it])) }
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

// ---------- Kolory ----------

private fun hsv(c: Long): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toInt(), it) }

private fun fromHsv(h: Float, s: Float, v: Float): Long = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFF

private fun hex(c: Long) = "#%06X".format(c and 0xFFFFFF)

/**
 * Wybór koloru: gotowe próbki i „własny” – suwaki odcienia, nasycenia i jasności oraz pole hex.
 * Zwraca kolor nieprzezroczysty; przezroczystość ustawia wywołujący.
 */
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
        Box(Modifier.weight(1f)) { Label(title) }
        Text(
            if (auto) "auto" else hex(value),
            color = Palette.TextDim,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 6.dp, end = 4.dp),
        )
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onAuto != null) Chip("Auto", auto, onClick = onAuto)
        presets.forEach { c ->
            val sel = !auto && (value and 0xFFFFFF) == (c and 0xFFFFFF)
            Swatch(Color(c or 0xFF000000), sel) { onPick(c or 0xFF000000) }
        }
        // Tęczowa próbka otwiera własny kolor
        Box(
            Modifier
                .size(36.dp)
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(value or 0xFF000000)).border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(10.dp))
            var text by remember(value) { mutableStateOf(hex(value)) }
            Box(Modifier.weight(1f)) {
                DarkField(text, "Hex (#RRGGBB)") { t ->
                    text = t.take(7)
                    val clean = t.trim().removePrefix("#")
                    if (clean.length == 6) clean.toLongOrNull(16)?.let { onPick(0xFF000000 or it) }
                }
            }
        }
        SliderRow(Icons.Rounded.ColorLens, "Odcień", hsv[0], 0f..360f, 0, { "${it.toInt()}°" }) { emit(it, hsv[1], hsv[2]) }
        SliderRow(Icons.Rounded.Colorize, "Nasycenie", hsv[1] * 100f, 0f..100f, 0, { "${it.toInt()}%" }) { emit(hsv[0], it / 100f, hsv[2]) }
        SliderRow(Icons.Rounded.LightMode, "Jasność", hsv[2] * 100f, 0f..100f, 0, { "${it.toInt()}%" }) { emit(hsv[0], hsv[1], it / 100f) }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, if (selected) Palette.Accent else Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
    }
}

// ---------- Drobne kontrolki ----------

@Composable
private fun DialogHint(text: String) {
    Text(text, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Chips(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
