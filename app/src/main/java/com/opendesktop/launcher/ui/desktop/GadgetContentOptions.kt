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

// ---------- Treść ----------

/** Ustawienia treści właściwe dla rodzaju widżetu. */
@Composable
internal fun KindOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
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
        ValueSlider("Liczba godzin", st.hours.toFloat().coerceIn(3f, 168f), 3f..168f, { hoursText(it.roundToInt()) }) { set(st.copy(hours = it.roundToInt())) }
        FlowChips(listOf("6 h", "12 h", "24 h", "48 h", "3 dni", "5 dni", "7 dni"), listOf(6, 12, 24, 48, 72, 120, 168).indexOf(st.hours)) {
            set(st.copy(hours = listOf(6, 12, 24, 48, 72, 120, 168)[it]))
        }
        SubLabel("Odstęp")
        Segmented(listOf("1 h", "2 h", "3 h", "6 h", "12 h"), listOf(1, 2, 3, 6, 12).indexOf(st.hourStep)) { set(st.copy(hourStep = listOf(1, 2, 3, 6, 12)[it])) }
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
            Section("Źródło kroków", Icons.Rounded.DirectionsWalk) {
                Segmented(listOf("Automatycznie", "Health Connect", "Czujnik telefonu"), st.stepSource.coerceIn(0, 2)) { set(st.copy(stepSource = it)) }
                Hint(
                    when (st.stepSource) {
                        1 -> "Dane z Health Connect (Google Fit, Samsung Health, opaski). Zdrowie Huawei tam nie zapisuje."
                        2 -> "Kroki liczy sam telefon. Kalorie i dystans są szacowane z liczby kroków, a historia zbiera się od dziś."
                        else -> "Najpierw Health Connect, a gdy nie ma stamtąd kroków – czujnik telefonu."
                    }
                )
            }
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
        GadgetKind.WeatherChart -> {
            layout(listOf("Godziny", "Dni"), 1)
            Section("Zakres", if (st.mode == 1) Icons.Rounded.DateRange else Icons.Rounded.AccessTime, hint = "Podpisy same się przerzedzają, żeby się nie nakładały – najcieplej i najzimniej są podpisane zawsze.") {
                if (st.mode == 1) {
                    daysSlider()
                    dayNames()
                } else hoursSlider()
            }
            Section("Pokazuj", Icons.Rounded.Info) {
                Toggles(listOf(details("Nagłówek"), Tile(Icons.Rounded.WbSunny, "Ikony warunków", st.showCondition) { set(st.copy(showCondition = it)) }, today, fill, clock) + if (st.mode == 1) listOf(tomorrow) else emptyList())
            }
            MetricOptions(
                st,
                if (st.mode == 1) listOf(WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind, WeatherMetric.Uv)
                else listOf(WeatherMetric.Feels, WeatherMetric.Precip, WeatherMetric.Rain, WeatherMetric.Wind, WeatherMetric.Humidity, WeatherMetric.Cloud, WeatherMetric.Uv, WeatherMetric.Pressure),
                set,
                hint = if (st.mode == 1) "W dniach druga, przerywana krzywa to temperatura minimalna. Wiatr i UV to pasy pod wykresem."
                else "Odczuwalna to druga, przerywana krzywa; opady to słupki (szansa albo suma mm). Wiatr, wilgotność, chmury, UV i ciśnienie to pasy pod wykresem – te, które się nie zmieszczą, chowają się.",
            )
        }
        GadgetKind.AirQuality -> {
            layout(listOf("Wskaźnik", "Lista", "Prognoza 24 h"), 2)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Nazwa miejsca"))) }
            Section("Zanieczyszczenia", Icons.Rounded.Air, hint = "Europejski indeks jakości powietrza (EAQI) z Open-Meteo. Pyłki są dostępne tylko w Europie i w sezonie pylenia.") {
                Toggles(AirMetrics.map { (k, name) -> Tile(Icons.Rounded.Air, name, k in st.metrics) { on -> set(st.copy(metrics = if (on) st.metrics + k else st.metrics - k)) } }, showIcons = false)
            }
        }
        GadgetKind.RainNow -> Section("Pokazuj", Icons.Rounded.Info, hint = "Prognoza co 15 minut na 6 godzin; najdokładniejsza w Europie i Ameryce Północnej.") {
            Toggles(listOf(details("Nazwa miejsca"), clock))
        }
        GadgetKind.Wind -> {
            layout(listOf("Sama róża", "Z opisem"), 1)
            Section("Pokazuj", Icons.Rounded.Info, hint = "„Z opisem” potrzebuje szerszego widżetu, np. 4 × 2.") { Toggles(listOf(details("Porywy i miejsce"))) }
        }
        GadgetKind.FlipClock -> Section("Zegar", Icons.Rounded.AccessTime, hint = "Karty mają kolor akcentu, cyfry – kolor tekstu (karta Kolory).") {
            Toggles(listOf(clock, seconds, date))
        }
        GadgetKind.WeekStrip -> Section("Tydzień", Icons.Rounded.ViewWeek, hint = "Soboty, niedziele i święta są przygaszone, święta – czerwone.") {
            Toggles(listOf(monday, details("Numer tygodnia")))
        }
        GadgetKind.Holidays -> {
            layout(listOf("Najbliższe + lista", "Sama lista"), 1)
            Section("Pokazuj", Icons.Rounded.Info, hint = "Dni wolne od pracy w Polsce, liczone w telefonie – także ruchome i mostki.") {
                Toggles(listOf(details("Także święta zwyczajowe")))
            }
        }
        GadgetKind.Agenda -> Section("Wydarzenia", Icons.Rounded.DateRange, hint = "Z kalendarzy widocznych w aplikacji Kalendarz (Google, Exchange, telefon).") {
            ValueSlider("Ile dni do przodu", st.days.toFloat().coerceIn(1f, 31f), 1f..31f, { "${it.toInt()}" }, steps = 29) { set(st.copy(days = it.toInt())) }
            Toggles(listOf(clock, details("Miejsce")))
        }
        GadgetKind.NextAlarm -> Section("Budzik", Icons.Rounded.AccessTime) { Toggles(listOf(clock, details("Za ile"))) }
        GadgetKind.DeviceStatus -> {
            layout(listOf("Kafelki", "Lista"), 1)
            Section("Pokazuj", Icons.Rounded.Info) { Toggles(listOf(details("Wolne GB zamiast %"))) }
        }
    }
}

/** Wygląd wspólny dla widżetów pogody: tło wg pogody, ikony, kolory temperatur. */
@Composable
internal fun WeatherLookOptions(kind: GadgetKind, st: GadgetStyle, set: (GadgetStyle) -> Unit) {
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
            ) + if (kind in setOf(GadgetKind.Forecast, GadgetKind.HourlyForecast, GadgetKind.WeatherFull, GadgetKind.WeatherDetails, GadgetKind.WeatherChart)) {
                listOf(Tile(Icons.Rounded.HorizontalRule, "Linie podziału", st.separators) { set(st.copy(separators = it)) })
            } else emptyList()
        )
        SubLabel("Ikony")
        Segmented(iconStyles, st.iconStyle.coerceIn(0, 3)) { set(st.copy(iconStyle = it)) }
        ValueSlider("Wielkość ikon", st.iconScale * 100f, 50f..200f, { "${it.toInt()}%" }) { set(st.copy(iconScale = it / 100f)) }
    }
}

internal val metricTiles = mapOf(
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
internal fun MetricOptions(st: GadgetStyle, available: List<String>, set: (GadgetStyle) -> Unit, hint: String? = null) {
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
internal fun ZonePicker(st: GadgetStyle, set: (GadgetStyle) -> Unit) {
    Toggles(
        WorldZones.map { (id, name) ->
            Tile(Icons.Rounded.Public, name, id in st.zones) { on -> set(st.copy(zones = if (on) (st.zones + id).takeLast(6) else st.zones - id)) }
        },
        showIcons = false,
    )
}
