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
    Size("Rozmiar (największe)"),
    UpdateDate("Ostatnio aktualizowane"),
}

@Serializable
enum class AppFilter(val label: String) {
    All("Wszystkie"),
    LastWeek("Zainstalowane w tym tygodniu"),
    LastMonth("Zainstalowane w tym miesiącu"),
    Large("Duże aplikacje (>100MB)"),
    Small("Małe aplikacje (<10MB)"),
    System("Systemowe"),
    UserInstalled("Zainstalowane przez użytkownika"),
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
enum class ItemType { App, Folder, Widget, Clock, Search, Action }

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
    /** Wygląd wbudowanej wyszukiwarki ([ItemType.Search]). */
    val searchStyle: SearchStyle = SearchStyle(),
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
    val iconPadding: Float = 0.85f,
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
    // Efekty i animacje
    val iconPressEffect: Boolean = true,
    val iconWiggleOnLongPress: Boolean = true,
    val parallaxEffect: Boolean = true,
    val particleEffects: Boolean = false,
    val iconBounceAnimation: Boolean = true,
    val folderOpenAnimation: Boolean = true,
    /** Tło menu kontekstowych (ikony, widżety, szuflada): kolor RGB i krycie. */
    val menuColor: Long = 0xFFFFFFFF,
    val menuOpacity: Float = 0.74f,
    /** Tło okienek folderów: kolor RGB i krycie. */
    val folderColor: Long = 0xFFFFFFFF,
    val folderOpacity: Float = 0.74f,
    /** Zaokrąglenie rogów menu kontekstowych i okienek folderów (dp). */
    val menuRadius: Float = 6f,
    val folderRadius: Float = 6f,
    val swipeTrails: Boolean = false,
    // AI (OpenRouter)
    val openRouterKey: String = "",
    val aiModel: String = "",
    val aiModelName: String = "",
    // Aktualizacje
    val updateRepo: String = DEFAULT_REPO,
    val autoUpdate: Boolean = true,
    val skippedVersion: Int = 0,
    val onboardingDone: Boolean = false,
    // Google Drive
    val cloudAutoSync: Boolean = false,
    val cloudSyncInterval: Int = 5, // minuty
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
