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
}

@Serializable
enum class DrawerStyle(val label: String, val description: String) {
    Grid("Siatka", "Klasyczna pionowa siatka z sekcjami"),
    List("Lista", "Duże wiersze z nazwą i kategoriami"),
    Pages("Strony poziome", "Siatka przewijana stronami w bok"),
    Alphabet("Alfabet", "Lista z literami i szybkim przewijaniem"),
    Categories("Zakładki kategorii", "Każda kategoria to osobna karta — przesuwaj w bok"),
    Tiles("Kafelki", "Kolorowe kafelki, najczęściej używane są większe"),
    Carousel("Karuzela 3D", "Okładki obracające się w przestrzeni"),
    Helix("Helisa 4D", "Spirala aplikacji, kręci się przy przewijaniu"),
    Sphere("Kula 3D", "Aplikacje na obracającej się kuli"),
    Wheel("Koło", "Pokrętło pod kciukiem, do obsługi jedną ręką"),
    Bubbles("Bąbelki", "Plaster miodu z efektem soczewki jak w zegarku"),
}

@Serializable
enum class DrawerSort(val label: String) {
    Name("Nazwa (A–Z)"),
    Category("Kategoria"),
    Usage("Najczęściej używane"),
    Installed("Ostatnio zainstalowane"),
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
    EditMode("Tryb edycji pulpitu"),
    Settings("Ustawienia launchera"),
}

@Serializable
enum class ItemType { App, Folder, Widget, Clock, Search }

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
    val editGridLines: Boolean = true,
    // Ikony
    val iconScale: Float = 1f,
    val showLabels: Boolean = true,
    val labelSize: Float = 12f,
    val iconShape: IconShape = IconShape.Squircle,
    // Dok
    val dockEnabled: Boolean = true,
    val dockCount: Int = 5,
    val dockOpacity: Float = 0.16f,
    val dockLabels: Boolean = false,
    // Szuflada
    val drawerStyle: DrawerStyle = DrawerStyle.Grid,
    val drawerSort: DrawerSort = DrawerSort.Name,
    val drawerColumns: Int = 5,
    val drawerOpacity: Float = 0.86f,
    val drawerAnimation: DrawerAnimation = DrawerAnimation.Slide,
    val drawerCategoryChips: Boolean = true,
    val drawerLabels: Boolean = true,
    val blurBehindDrawer: Boolean = true,
    val searchAutoFocus: Boolean = false,
    // Gesty
    val swipeUp: GestureAction = GestureAction.Drawer,
    val swipeDown: GestureAction = GestureAction.Notifications,
    val doubleTap: GestureAction = GestureAction.EditMode,
    val haptics: Boolean = true,
    // Wygląd
    val accent: Long = 0xFF8AB4FF,
    val dynamicColor: Boolean = true,
    // AI (OpenRouter)
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
