package com.opendesktop.launcher.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

@Serializable
enum class PageTransition(val label: String) {
    Slide("Przesunięcie"),
    Cube("Sześcian"),
    CubeInside("Sześcian od środka"),
    Depth("Głębia"),
    Zoom("Oddalenie"),
    Fade("Przenikanie"),
    Rotate("Wachlarz"),
    Flip("Odwracana karta"),
    Stack("Stos kart"),
    Carousel("Karuzela 3D"),
    Accordion("Akordeon"),
    Wave("Fala"),
    Random("Losowe"),
}

@Serializable
enum class DrawerStyle(val label: String, val description: String) {
    Grid("Siatka", "Klasyczna pionowa siatka z sekcjami"),
    Perspective("Perspektywa 3D", "Siatka zawijająca się w głąb u góry i dołu, jak bęben"),
    List("Lista", "Duże wiersze z nazwą i kategoriami"),
    Pages("Strony poziome", "Siatka przewijana stronami w bok"),
    Categories("Zakładki kategorii", "Każda kategoria to osobna karta – przesuwaj w bok"),
    Helix("Helisa 4D", "Spirala aplikacji, kręci się przy przewijaniu"),
    Sphere("Kula 3D", "Aplikacje na obracającej się kuli"),
    Wheel("Koło", "Pokrętło pod kciukiem, do obsługi jedną ręką"),
    Bubbles("Bąbelki", "Plaster miodu z efektem soczewki jak w zegarku"),
    Cylinder("Cylinder 3D", "Aplikacje na obracającym się cylindrze"),
    Wave("Fala", "Ikony układające się w falę przy przewijaniu"),
    Stack("Stos", "Lista kart, które u góry chowają się jedna pod drugą"),
}

@Serializable
enum class DrawerSort(val label: String) {
    Name("Nazwa (A–Z)"),
    Category("Kategoria"),
    Usage("Najczęściej używane"),
    Installed("Ostatnio zainstalowane"),
}

@Serializable
enum class PowerSaver(val label: String, val description: String) {
    Off("Wyłączony", "Zawsze pełne efekty"),
    Auto("Automatycznie", "Gdy działa systemowe oszczędzanie baterii albo telefon ma mało pamięci"),
    On("Zawsze włączony", "Proste przejścia i szuflada, bez rozmycia i odbić"),
}

@Serializable
enum class DrawerAnimation(val label: String) {
    Slide("Wysunięcie z dołu"),
    Fade("Przenikanie"),
    Zoom("Powiększenie"),
}

@Serializable
enum class IconShape(val label: String) {
    System("Systemowy"),
    Circle("Koło"),
    Squircle("Squircle"),
    RoundedSquare("Zaokrąglony kwadrat"),
    Square("Kwadrat"),
    Teardrop("Kropla"),
}

@Serializable
enum class GestureAction(val label: String) {
    None("Nic"),
    Drawer("Szuflada aplikacji"),
    Search("Wyszukiwanie"),
    Notifications("Powiadomienia"),
    QuickSettings("Szybkie ustawienia"),
    EditMode("Tryb edycji pulpitu"),
    Settings("Ustawienia launchera"),
}

@Serializable
enum class ItemType { App, Folder, Widget, Clock, Search, Action, Gadget }

@Serializable
enum class SystemAction(val label: String) {
    OpenDrawer("Szuflada aplikacji"),
    OpenSearch("Wyszukiwanie"),
    OpenNotifications("Powiadomienia"),
    OpenQuickSettings("Szybkie ustawienia"),
    OpenSettings("Ustawienia launchera");

    /** Klucz akcji w doku (dok przechowuje listę kluczy obok kluczy aplikacji). */
    val dockKey: String get() = "$ACTION_PREFIX$name"

    companion object {
        private const val ACTION_PREFIX = "action:"
        fun fromDockKey(key: String): SystemAction? =
            if (key.startsWith(ACTION_PREFIX)) entries.firstOrNull { it.name == key.removePrefix(ACTION_PREFIX) } else null
    }
}

@Serializable
data class DesktopItem(
    val id: String = newId(),
    val type: ItemType,
    val x: Int,
    val y: Int,
    val w: Int = 1,
    val h: Int = 1,
    /** Klucz aplikacji (ComponentName.flattenToString) dla [ItemType.App]. */
    val app: String? = null,
    /** Zawartość folderu. */
    val apps: List<String> = emptyList(),
    val label: String = "",
    val widgetId: Int = -1,
    val provider: String? = null,
    /** Akcja systemowa dla [ItemType.Action]. */
    val systemAction: SystemAction? = null,
    /** Widżet systemowy: treść rozciągnięta na cały kafelek. */
    val stretch: Boolean = false,
    /** Główna aplikacja folderu - uruchamia się po kliknięciu. */
    val primaryApp: String? = null,
    /** Folder otwiera się jako jeden przewijany wiersz zamiast siatki. */
    val folderRow: Boolean = false,
    /** Wygląd wbudowanej wyszukiwarki ([ItemType.Search]). */
    val searchStyle: SearchStyle = SearchStyle(),
    /** Rodzaj wbudowanego widżetu dla [ItemType.Gadget]. */
    val gadget: GadgetKind? = null,
    /** Wygląd i ustawienia wbudowanego widżetu ([ItemType.Gadget]). */
    val gadgetStyle: GadgetStyle = GadgetStyle(),
)

/** Wbudowane widżety OpenDesktop – domyślny rozmiar w komórkach siatki. */
@Serializable
enum class GadgetKind(val label: String, val description: String, val w: Int, val h: Int) {
    DigitalClock("Zegar cyfrowy", "Godzina, data i bateria", 4, 2),
    AnalogClock("Zegar analogowy", "Tarcza ze wskazówkami", 2, 2),
    ClockWeather("Zegar z pogodą", "Godzina obok bieżącej pogody", 4, 2),
    Weather("Pogoda", "Temperatura i warunki teraz", 3, 2),
    Forecast("Prognoza", "Pogoda na kilka dni", 4, 2),
    Calendar("Kalendarz", "Siatka miesiąca z dzisiejszym dniem", 3, 3),
    DateCard("Kartka z kalendarza", "Dzień tygodnia i data", 2, 2),
    Battery("Bateria", "Poziom naładowania", 2, 2),
    WorldClock("Zegary świata", "Godzina w innych miastach", 4, 2),
    Countdown("Odliczanie", "Dni do wybranej daty", 3, 2),
    Note("Notatka", "Własny tekst na pulpicie", 3, 2),
    Greeting("Powitanie", "Dzień dobry o każdej porze", 4, 1),
    Progress("Postęp czasu", "Ile minęło z dnia, miesiąca, roku", 4, 1),
    Proverb("Przysłowie dnia", "Codziennie inne", 4, 2),
    Storage("Pamięć", "Miejsce na dane i RAM", 3, 2),
    HourlyForecast("Prognoza godzinowa", "Wykres temperatury i opadów na kolejne godziny", 4, 2),
    WeatherFull("Pogoda – pełny panel", "Teraz, najbliższe godziny i kolejne dni", 4, 4),
    WeatherDetails("Szczegóły pogody", "Wiatr, wilgotność, ciśnienie, UV, opady", 4, 2),
    SunArc("Słońce", "Wschód, zachód i długość dnia", 4, 2),
    MoonPhase("Faza Księżyca", "Dzisiejsza faza i najbliższa pełnia", 2, 2),
    YearDots("Rok w kropkach", "Każdy dzień roku albo miesiąca jako kropka", 4, 2),
    MoonRound("Księżyc", "Okrągły widżet z dzisiejszą fazą", 2, 2),
    SkyClock("Zegar ze słońcem i Księżycem", "Godzina, dzień i data; wschody i zachody słońca i Księżyca", 4, 2),
    Steps("Kroki i kalorie", "Dzisiejsza aktywność z czujnika telefonu albo Health Connect", 4, 2),
    ;

    /** Widżet korzystający z danych pogodowych (miasto, jednostki, tło zależne od pogody). */
    val isWeather: Boolean get() = this in setOf(ClockWeather, Weather, Forecast, HourlyForecast, WeatherFull, WeatherDetails, SunArc, SkyClock)

    fun defaultStyle(): GadgetStyle = when (this) {
        DigitalClock, AnalogClock, ClockWeather, Greeting, SkyClock -> GadgetStyle(background = 0x00000000, border = false, textShadow = true)
        MoonRound -> GadgetStyle(background = 0xF00A0F1E, border = false, shadow = 0.4f, showDetails = false)
        Steps -> GadgetStyle(background = 0xD9101418, border = false, shadow = 0.3f, accent = 0xFF5BE49B)
        Forecast -> GadgetStyle(metrics = listOf(WeatherMetric.Precip))
        HourlyForecast -> GadgetStyle(metrics = listOf(WeatherMetric.Precip, WeatherMetric.Wind, WeatherMetric.Feels), hours = 12)
        WeatherFull -> GadgetStyle(
            background = 0xD9000000, weatherBackground = true, border = false, shadow = 0.3f, textShadow = true, iconStyle = 1, tempColors = true,
            metrics = listOf(WeatherMetric.Precip, WeatherMetric.Wind, WeatherMetric.Humidity), hours = 12, days = 7,
        )
        WeatherDetails -> GadgetStyle(metrics = WeatherMetric.all)
        else -> GadgetStyle()
    }
}

/** Dodatkowe wartości pogodowe do pokazania w widżetach (lista w [GadgetStyle.metrics]). */
object WeatherMetric {
    const val Precip = "precip"
    const val Rain = "rain"
    const val Wind = "wind"
    const val Humidity = "humidity"
    const val Feels = "feels"
    const val Pressure = "pressure"
    const val Uv = "uv"
    const val Sun = "sun"
    const val Cloud = "cloud"

    val all = listOf(Feels, Wind, Humidity, Pressure, Uv, Precip, Rain, Cloud, Sun)
}

/**
 * Wygląd i opcje wbudowanego widżetu. Część pól dotyczy tylko wybranych rodzajów
 * (np. [city] – pogody, [zones] – zegarów świata). Kolory ARGB.
 */
@Serializable
data class GadgetStyle(
    val background: Long = 0x40000000,
    /** Zaokrąglenie rogów w dp. */
    val radius: Int = 24,
    val textColor: Long = 0xFFFFFFFF,
    val accent: Long = 0xFF8AB4FF,
    val border: Boolean = true,
    /** Siła cienia pod kartą 0–1. */
    val shadow: Float = 0f,
    val textShadow: Boolean = false,
    /** Cień pod całą treścią (tekst, ikony, wykresy, wskazówki) 0–1. */
    val contentShadow: Float = 0f,
    /** Rozmycie cienia treści w dp. */
    val shadowBlur: Float = 3f,
    /** Przesunięcie cienia w dół w dp. */
    val shadowOffset: Float = 1.5f,
    /** Kolor cieni (karty i treści); biały daje poświatę. */
    val shadowColor: Long = 0xFF000000,
    /** Mnożnik wielkości tekstu. */
    val textScale: Float = 1f,
    /** Dodatkowy mnożnik wielkości samej godziny (w widżetach z zegarem: głównej wartości). */
    val timeScale: Float = 1f,
    /** Odstęp pod godziną w dp; ujemny przysuwa datę bliżej. */
    val timeGap: Int = 0,
    /** Grubość głównego tekstu (100–900). */
    val weight: Int = 300,
    /** Wyrównanie: 0 = do lewej, 1 = środek, 2 = do prawej. */
    val align: Int = 1,
    /** Wewnętrzny margines w dp. */
    val padding: Int = 12,
    val use24h: Boolean = true,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showBattery: Boolean = false,
    /** Dodatkowe szczegóły (odczuwalna, wiatr, cyfry na tarczy…). */
    val showDetails: Boolean = true,
    /** Wariant – znaczenie zależy od rodzaju (tarcza, okres postępu, układ baterii…). */
    val mode: Int = 0,
    val city: String = "Warszawa",
    val fahrenheit: Boolean = false,
    val zones: List<String> = listOf("Europe/London", "America/New_York", "Asia/Tokyo"),
    /** Data docelowa odliczania (RRRR-MM-DD). */
    val target: String = "",
    /** Podpis: imię w powitaniu, nazwa odliczania, tytuł notatki. */
    val label: String = "",
    /** Treść notatki. */
    val text: String = "",
    val mondayFirst: Boolean = true,
    /** Liczba dni prognozy (1–16). */
    val days: Int = 5,

    // ----- Rozszerzone ustawienia wyglądu (wszystkie rodzaje) -----
    /** Krój: 0 = systemowy, 1 = szeryfowy, 2 = o stałej szerokości, 3 = odręczny, 4 = wąski. */
    val fontFamily: Int = 0,
    /** Drugi kolor tła do gradientu (ARGB); używany, gdy [gradient] > 0. */
    val background2: Long = 0x40000000,
    /** Gradient tła: 0 = brak, 1 = pionowy, 2 = poziomy, 3 = ukośny, 4 = promienisty. */
    val gradient: Int = 0,
    /** Grubość obrysu w dp. */
    val borderWidth: Float = 1f,
    /** Kolor obrysu; 0 = automatyczny (przygaszony kolor tekstu). */
    val borderColor: Long = 0,
    /** Ikony pogody: 0 = kolor akcentu, 1 = kolorowe wg warunków, 2 = kolor tekstu, 3 = emoji. */
    val iconStyle: Int = 0,
    /** Mnożnik wielkości ikon. */
    val iconScale: Float = 1f,

    // ----- Pogoda -----
    /** Tło karty zależne od pogody i pory dnia (zastępuje kolor tła). */
    val weatherBackground: Boolean = false,
    /** Kolorowanie temperatur od fioletu (mróz) do czerwieni (upał). */
    val tempColors: Boolean = false,
    /** Wiatr: 0 = km/h, 1 = m/s, 2 = mph, 3 = węzły. */
    val windUnit: Int = 0,
    /** Ciśnienie: 0 = hPa, 1 = mmHg, 2 = inHg. */
    val pressureUnit: Int = 0,
    /** Dodatkowe wartości ([WeatherMetric]). */
    val metrics: List<String> = emptyList(),
    /** Prognoza od jutra zamiast od dziś. */
    val startTomorrow: Boolean = false,
    /** Nazwy dni: 0 = skrót, 1 = pełna nazwa, 2 = data, 3 = skrót i data. */
    val dayNames: Int = 0,
    /** Liczba godzin prognozy godzinowej (3–48). */
    val hours: Int = 12,
    /** Co ile godzin pokazywać punkt prognozy godzinowej. */
    val hourStep: Int = 1,
    /** Temperatury z jedną cyfrą po przecinku. */
    val decimals: Boolean = false,
    /** Wyróżnienie dzisiejszego dnia / bieżącej godziny. */
    val highlightToday: Boolean = true,
    /** Linie oddzielające dni i sekcje. */
    val separators: Boolean = true,
    /** Wypełnienie pod krzywą wykresu. */
    val chartFill: Boolean = true,
    /** Godzina ostatniej aktualizacji w rogu. */
    val showUpdated: Boolean = false,
    /** Opis warunków słowami pod ikoną. */
    val showCondition: Boolean = false,
    /** Półkula południowa (lustrzane odbicie Księżyca). */
    val southern: Boolean = false,
    /** Księżyc ze zdjęcia zamiast rysowanej tarczy. */
    val realMoon: Boolean = false,
    /** Dzienny cel kroków. */
    val goal: Int = 8000,
    /** Źródło kroków: 0 = automatycznie, 1 = Health Connect, 2 = czujnik telefonu. */
    val stepSource: Int = 0,
    /** Współrzędne wybranego miejsca („lat, lon”); puste = szukanie po nazwie z [city]. */
    val coords: String = "",
    /** Pogoda dla bieżącego położenia telefonu zamiast [city]. */
    val useDeviceLocation: Boolean = false,
    /** Mały przycisk odświeżania w rogu widżetu pogody. */
    val showRefresh: Boolean = true,
    /** Podsumowanie prognozy godzinowej: najbliższe opady, najcieplej i najzimniej. */
    val showSummary: Boolean = true,
)

/**
 * Wygląd widżetu wyszukiwarki. Kolory ARGB; cień pola i cień tekstu pomagają, żeby pole było
 * czytelne na jasnej i na ciemnej tapecie.
 */
@Serializable
data class SearchStyle(
    /** Zaokrąglenie rogów: 0 = prostokąt, 50 = pigułka (procent wysokości). */
    val radius: Int = 50,
    val background: Long = 0x33FFFFFF,
    val textColor: Long = 0xFFFFFFFF,
    /** Siła cienia pod polem 0–1. */
    val shadow: Float = 0f,
    val border: Boolean = true,
    val textShadow: Boolean = false,
    val hint: String = "Szukaj aplikacji…",
)

@Serializable
data class Page(
    val id: String = newId(),
    val items: List<DesktopItem> = emptyList(),
)

@Serializable
data class Category(
    val id: String = newId(),
    val name: String,
    val emoji: String = "",
    val color: Long = 0xFF8AB4FF,
    val source: String = "manual",
)

@Serializable
data class Workspace(
    val pages: List<Page> = listOf(Page()),
    val dock: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** Klucz aplikacji → identyfikatory kategorii (aplikacja może mieć ich wiele). */
    val assignments: Map<String, List<String>> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val usage: Map<String, Int> = emptyMap(),
    val renamed: Map<String, String> = emptyMap(),
    val initialized: Boolean = false,
)

@Serializable
data class Config(
    // Pulpit
    val columns: Int = 5,
    val rows: Int = 6,
    val allowOverlap: Boolean = false,
    val transition: PageTransition = PageTransition.Cube,
    val defaultPage: Int = 0,
    val wallpaperScroll: Boolean = true,
    val showPageIndicator: Boolean = true,
    /** Przewijanie ekranów w pętli: za ostatnim jest pierwszy, przed pierwszym ostatni. */
    val pageLoop: Boolean = false,
    /** Po powrocie z aplikacji zostaje ekran, z którego ją uruchomiono (zamiast ekranu głównego). */
    val returnToLastPage: Boolean = false,
    /** Blokada układu: bez przeciągania, zmiany rozmiaru i usuwania elementów. */
    val layoutLocked: Boolean = false,
    /** Wskaźnik ekranów widoczny stale, a nie tylko podczas przewijania. */
    val pageIndicatorAlways: Boolean = false,
    val editGridLines: Boolean = true,
    // Ikony
    val iconScale: Float = 1f,
    /** Rozmiar ikon w doku (osobno od szuflady). */
    val dockIconScale: Float = 1f,
    /** Przesunięcie doku w pionie (dp): dodatnie = niżej, aż na pasek nawigacji. */
    val dockOffset: Float = 0f,
    val desktopIconScale: Float = 1f,
    val showLabels: Boolean = true,
    val labelSize: Float = 12f,
    val iconShape: IconShape = IconShape.Squircle,
    /** Plakietki z liczbą powiadomień na ikonach (wymaga dostępu do powiadomień). */
    val notificationBadges: Boolean = true,
    // Dok
    val dockEnabled: Boolean = true,
    val dockCount: Int = 5,
    val dockOpacity: Float = 0.16f,
    val dockLabels: Boolean = false,
    val dockBackground: Boolean = true,
    /** Odbicia ikon aplikacji pod dokiem. */
    val dockReflection: Boolean = false,
    // Szuflada
    val drawerStyle: DrawerStyle = DrawerStyle.Grid,
    val drawerSort: DrawerSort = DrawerSort.Name,
    val drawerColumns: Int = 5,
    /** Kategorie pod nazwą aplikacji we wszystkich układach szuflady. */
    val drawerShowCategories: Boolean = true,
    /** Wiersze w stylu „Strony”; 0 = automatycznie. */
    val drawerRows: Int = 0,
    val drawerOpacity: Float = 0.86f,
    val drawerAnimation: DrawerAnimation = DrawerAnimation.Slide,
    val drawerCategoryChips: Boolean = true,
    /** Karty kategorii u góry (przesuwane w bok) w każdym układzie szuflady. */
    val drawerCategoryTabs: Boolean = false,
    val drawerLabels: Boolean = true,
    val blurBehindDrawer: Boolean = true,
    val searchAutoFocus: Boolean = false,
    val drawerBackground: Boolean = true,
    val drawerHeaderBackground: Boolean = true,
    /** Kontakty i ekrany ustawień w wynikach wyszukiwania w szufladzie. */
    val drawerContacts: Boolean = true,
    /** Kalkulator i przeliczanie jednostek nad wynikami wyszukiwania. */
    val drawerQuickAnswers: Boolean = true,
    // Gesty
    val swipeUp: GestureAction = GestureAction.Drawer,
    val swipeDown: GestureAction = GestureAction.Notifications,
    val doubleTap: GestureAction = GestureAction.EditMode,
    // Gesty dwoma palcami
    val pinchIn: GestureAction = GestureAction.EditMode,
    val pinchOut: GestureAction = GestureAction.None,
    val twoFingerUp: GestureAction = GestureAction.None,
    val twoFingerDown: GestureAction = GestureAction.QuickSettings,
    val haptics: Boolean = true,
    // Wygląd
    // Domyślnie czarno-biały interfejs jak w OpenPDF
    val accent: Long = 0xFFFFFFFF,
    val dynamicColor: Boolean = false,
    val autoTheme: Boolean = false,
    val darkModeStartHour: Int = 20,
    val darkModeEndHour: Int = 7,
    /** Tło menu kontekstowych (ikony, widżety, szuflada): kolor RGB i krycie. */
    val menuColor: Long = 0xFFFFFFFF,
    val menuOpacity: Float = 0.74f,
    /** Tło okienek folderów: kolor RGB i krycie. */
    val folderColor: Long = 0xFFFFFFFF,
    val folderOpacity: Float = 0.74f,
    /** Zaokrąglenie rogów menu kontekstowych i okienek folderów (dp). */
    val menuRadius: Float = 6f,
    val folderRadius: Float = 6f,
    /** Tryb oszczędny: proste przejścia i szuflada, bez rozmycia i odbić. */
    val powerSaver: PowerSaver = PowerSaver.Auto,
    // Efekty (ustawienia → Efekty)
    val iconPressEffect: Boolean = true,
    val iconWiggleOnLongPress: Boolean = true,
    val particleEffects: Boolean = false,
    val iconBounceAnimation: Boolean = true,
    val folderOpenAnimation: Boolean = true,
    val swipeTrails: Boolean = false,
    val parallaxEffect: Boolean = false,
    // AI – dostawca i osobne klucze/modele dla każdego z nich (klucz: AiProvider.name).
    // openRouterKey/aiModel/aiModelName to dawne pola OpenRoutera – czytane jako wartości zapasowe.
    val aiProvider: AiProvider = AiProvider.OpenRouter,
    val aiKeys: Map<String, String> = emptyMap(),
    val aiModels: Map<String, String> = emptyMap(),
    val aiModelNames: Map<String, String> = emptyMap(),
    /** Adres serwera zgodnego z API OpenAI dla dostawcy „Własny” (np. Ollama, LM Studio). */
    val aiBaseUrl: String = "",
    val openRouterKey: String = "",
    val aiModel: String = "",
    val aiModelName: String = "",
    // Aktualizacje
    val updateRepo: String = DEFAULT_REPO,
    val autoUpdate: Boolean = true,
    val skippedVersion: Int = 0,
    val onboardingDone: Boolean = false,
) {
    companion object {
        const val DEFAULT_REPO = "BlackSingularities/OpenDesktop"
    }
}

@Serializable
data class Backup(
    val version: Int = 1,
    val config: Config,
    val workspace: Workspace,
)

/** Konfiguracja do rysowania w trybie oszczędnym: bez efektów 3D, rozmycia i odbić. */
fun Config.lite(): Config = copy(
    transition = if (transition == PageTransition.Slide || transition == PageTransition.Fade) transition else PageTransition.Slide,
    drawerStyle = if (drawerStyle in LIGHT_DRAWERS) drawerStyle else DrawerStyle.Grid,
    blurBehindDrawer = false,
    dockReflection = false,
)

private val LIGHT_DRAWERS = setOf(DrawerStyle.Grid, DrawerStyle.Categories, DrawerStyle.List, DrawerStyle.Pages)
