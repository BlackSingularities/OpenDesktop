package com.opendesktop.launcher.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opendesktop.launcher.BuildConfig
import com.opendesktop.launcher.R
import com.opendesktop.launcher.data.*
import com.opendesktop.launcher.ui.LauncherViewModel
import com.opendesktop.launcher.ui.UpdateState
import com.opendesktop.launcher.ui.components.PanelStyle
import com.opendesktop.launcher.ui.theme.Palette
import java.time.LocalDate

/** Sekcje ustawień – każda ma własny widok. */
private enum class Section(val title: String, val icon: ImageVector, val summary: String) {
    Desktop("Pulpit", Icons.Rounded.Home, "Siatka, ikony, przejścia, wskaźnik ekranów"),
    Dock("Dok", Icons.Rounded.Dashboard, "Miejsca, rozmiar ikon, tło, podpisy"),
    Drawer("Szuflada aplikacji", Icons.Rounded.GridView, "Układ, ikony, sortowanie, kolumny i wiersze"),
    Gestures("Gesty", Icons.Rounded.Gesture, "Przesunięcia, dotknięcia, wibracje"),
    Look("Wygląd", Icons.Rounded.Palette, "Kolory, kształt ikon, menu, tapeta"),
    Effects("Efekty i animacje", Icons.Rounded.Animation, "Naciśnięcia, cząsteczki, parallax"),
    Ai("Sztuczna inteligencja", Icons.Rounded.AutoAwesome, "Dostawca (OpenRouter, OpenAI, Claude…), klucz i model"),
    Updates("Aktualizacje", Icons.Rounded.SystemUpdate, "Repozytorium i sprawdzanie"),
    Backup("Kopia zapasowa", Icons.Rounded.CloudSync, "Google Drive, eksport, import"),
}

private data class ChoiceState<T>(
    val title: String,
    val options: List<T>,
    val current: T,
    val label: (T) -> String,
    val desc: ((T) -> String)? = null,
    val onPick: (T) -> Unit,
)

@Composable
fun SettingsScreen(vm: LauncherViewModel, onClose: () -> Unit, onOpenCategories: () -> Unit = {}) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    var choice by remember { mutableStateOf<ChoiceState<Any?>?>(null) }
    val set: Setter = { t -> vm.updateConfig(t) }
    val pick: Picker = { title, options, current, label, desc, onPick -> choice = ChoiceState(title, options, current, label, desc, onPick) }

    BackHandler(enabled = section != null) { section = null }

    val current = section?.let { s -> Section.entries.firstOrNull { it.name == s } }
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            val forward = targetState != null
            (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
        },
        label = "settingsSection",
        modifier = Modifier.fillMaxSize().background(Palette.Panel),
    ) { s ->
        if (s == null) {
            SettingsHome(onClose, onOpenCategories) { section = it.name }
        } else {
            SettingsPage(s.title, onBack = { section = null }) {
                when (s) {
                    Section.Desktop -> DesktopSection(vm, cfg, set, pick)
                    Section.Dock -> DockSection(cfg, set)
                    Section.Drawer -> DrawerSection(cfg, set, pick)
                    Section.Gestures -> GesturesSection(cfg, set, pick)
                    Section.Look -> LookSection(cfg, set, pick)
                    Section.Effects -> EffectsSection(cfg, set)
                    Section.Ai -> AiSection(vm, cfg, set)
                    Section.Updates -> UpdatesSection(vm, cfg, set)
                    Section.Backup -> BackupSection(vm, cfg, set)
                }
            }
        }
    }

    choice?.let { c ->
        ChoiceDialog(
            title = c.title,
            options = c.options.map(c.label),
            selected = c.options.indexOf(c.current),
            descriptions = c.desc?.let { d -> c.options.map(d) },
            onDismiss = { choice = null },
            onSelect = { c.onPick(c.options[it]) },
        )
    }
}

// ------------------------------------------------------------------ strona główna

@Composable
private fun SettingsHome(onClose: () -> Unit, onOpenCategories: () -> Unit, onOpen: (Section) -> Unit) {
    val ctx = LocalContext.current
    SettingsPage("Ustawienia", onBack = onClose) {
        // Marka
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.opendesktop_mark), null, Modifier.size(48.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("OpenDesktop", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Wersja ${BuildConfig.VERSION_NAME}", color = Palette.TextDim, fontSize = 12.sp)
            }
        }
        if (!SystemActions.isDefaultLauncher(ctx)) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Danger.copy(alpha = 0.14f))
                    .border(1.dp, Palette.Danger.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("OpenDesktop nie jest domyślnym ekranem głównym", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                PillButton("Ustaw jako domyślny") { ctx.findActivity()?.let { SystemActions.requestDefaultLauncher(it) } }
            }
        }
        Group("Ekran główny") {
            listOf(Section.Desktop, Section.Dock, Section.Drawer, Section.Gestures).forEach { s ->
                ActionRow(s.icon, s.title, s.summary, chevron = true) { onOpen(s) }
            }
            ActionRow(Icons.Rounded.Category, "Kategorie aplikacji", "Grupy w szufladzie i filtry", chevron = true, onClick = onOpenCategories)
        }
        Group("Wygląd") {
            listOf(Section.Look, Section.Effects).forEach { s -> ActionRow(s.icon, s.title, s.summary, chevron = true) { onOpen(s) } }
        }
        Group("Usługi") {
            listOf(Section.Ai, Section.Updates, Section.Backup).forEach { s -> ActionRow(s.icon, s.title, s.summary, chevron = true) { onOpen(s) } }
        }
        Group("O aplikacji") {
            ActionRow(Icons.Rounded.Person, "Projekt i wykonanie", "redzikowski.com") { openUrl(ctx, "https://redzikowski.com") }
            ActionRow(Icons.Rounded.LocalCafe, "Postaw kawę", "Wesprzyj rozwój na buycoffee.to") { openUrl(ctx, "https://buycoffee.to/adamredzikowski") }
        }
        Text(
            "Otwarty launcher na Androida",
            color = Palette.TextFaint,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

// ------------------------------------------------------------------ sekcje

private typealias Setter = ((Config) -> Config) -> Unit
private typealias Picker = (String, List<Any?>, Any?, (Any?) -> String, ((Any?) -> String)?, (Any?) -> Unit) -> Unit

/** Wygodny wybór z listy enumów przez wspólny dialog. */
@Suppress("UNCHECKED_CAST")
private fun <T> Picker.choose(title: String, options: List<T>, current: T, label: (T) -> String, desc: ((T) -> String)? = null, onPick: (T) -> Unit) =
    this(title, options as List<Any?>, current, { label(it as T) }, desc?.let { d -> { v: Any? -> d(v as T) } }, { onPick(it as T) })

@Composable
private fun DesktopSection(vm: LauncherViewModel, cfg: Config, set: Setter, pick: Picker) {
    Group("Siatka") {
        SliderRow(Icons.Rounded.ViewColumn, "Kolumny", cfg.columns.toFloat(), 3f..10f, 6) { vm.setGrid(it.toInt(), cfg.rows) }
        SliderRow(Icons.Rounded.TableRows, "Wiersze", cfg.rows.toFloat(), 3f..16f, 12) { vm.setGrid(cfg.columns, it.toInt()) }
        SwitchRow(Icons.Rounded.ViewModule, "Nakładanie się elementów", "Widżety i ikony mogą zachodzić na siebie", cfg.allowOverlap) { vm.setOverlap(it) }
    }
    Group("Ikony") {
        SliderRow(Icons.Rounded.PhotoSizeSelectLarge, "Rozmiar ikon", cfg.desktopIconScale.coerceAtMost(1f), 0.5f..1f, 9, { "${(it * 100).toInt()}%" }) { v -> set { it.copy(desktopIconScale = v) } }
        SwitchRow(Icons.Rounded.Label, "Podpisy", null, cfg.showLabels) { v -> set { it.copy(showLabels = v) } }
        SliderRow(Icons.Rounded.TextFields, "Wielkość podpisów", cfg.labelSize, 9f..16f, 6, { "${it.toInt()} sp" }) { v -> set { it.copy(labelSize = v) } }
    }
    Group("Ekrany") {
        ActionRow(Icons.Rounded.SwapHoriz, "Przejście między ekranami", cfg.transition.label, chevron = true) {
            pick.choose("Przejście między ekranami", PageTransition.entries, cfg.transition, { it.label }) { t -> set { it.copy(transition = t) } }
        }
        SwitchRow(Icons.Rounded.Loop, "Przewijanie w pętli", "Za ostatnim ekranem wraca pierwszy, przed pierwszym ostatni", cfg.pageLoop) { v -> set { it.copy(pageLoop = v) } }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.FiberManualRecord, "Wskaźnik ekranów", cfg.showPageIndicator) { v -> set { it.copy(showPageIndicator = v) } },
            Tile(Icons.Rounded.PushPin, "Wskaźnik zawsze widoczny", cfg.pageIndicatorAlways) { v -> set { it.copy(pageIndicatorAlways = v) } },
            Tile(Icons.Rounded.Wallpaper, "Przewijanie tapety", cfg.wallpaperScroll) { v -> set { it.copy(wallpaperScroll = v) } },
            Tile(Icons.Rounded.GridOn, "Siatka w edycji", cfg.editGridLines) { v -> set { it.copy(editGridLines = v) } },
        ),
    )
}

@Composable
private fun DockSection(cfg: Config, set: Setter) {
    Group {
        SwitchRow(Icons.Rounded.ViewCarousel, "Pokaż dok", null, cfg.dockEnabled) { v -> set { it.copy(dockEnabled = v) } }
        SliderRow(Icons.Rounded.ViewWeek, "Miejsca w doku", cfg.dockCount.toFloat(), 3f..7f, 3) { v -> set { it.copy(dockCount = v.toInt()) } }
        SliderRow(
            Icons.Rounded.Height, "Położenie w pionie", cfg.dockOffset, -40f..60f, 19,
            { v -> when { v > 0.5f -> "${v.toInt()} dp niżej"; v < -0.5f -> "${-v.toInt()} dp wyżej"; else -> "Domyślne" } },
        ) { v -> set { it.copy(dockOffset = v) } }
        SliderRow(Icons.Rounded.PhotoSizeSelectLarge, "Rozmiar ikon", cfg.dockIconScale, 0.6f..1.3f, 13, { "${(it * 100).toInt()}%" }) { v -> set { it.copy(dockIconScale = v) } }
    }
    Group("Wygląd") {
        SwitchRow(Icons.Rounded.Square, "Tło doku", null, cfg.dockBackground) { v -> set { it.copy(dockBackground = v) } }
        if (cfg.dockBackground) {
            SliderRow(Icons.Rounded.Opacity, "Przezroczystość tła", cfg.dockOpacity, 0f..0.6f, 11, { "${(it * 100).toInt()}%" }) { v -> set { it.copy(dockOpacity = v) } }
        }
        SwitchRow(Icons.Rounded.Label, "Podpisy w doku", null, cfg.dockLabels) { v -> set { it.copy(dockLabels = v) } }
        SwitchRow(
            Icons.Rounded.Flip, "Odbicia ikon", if (cfg.dockLabels) "Niedostępne przy włączonych podpisach" else "Lustrzane odbicie pod każdą ikoną",
            cfg.dockReflection, enabled = !cfg.dockLabels,
        ) { v -> set { it.copy(dockReflection = v) } }
    }
    Text(
        "Skróty akcji (szuflada, wyszukiwanie, powiadomienia…) dodasz do doku w trybie edycji pulpitu: przycisk „Akcje”.",
        color = Palette.TextDim, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun DrawerSection(cfg: Config, set: Setter, pick: Picker) {
    Group("Układ") {
        ActionRow(Icons.Rounded.Style, "Układ", cfg.drawerStyle.label, chevron = true) {
            pick.choose("Układ szuflady", DrawerStyle.entries.filter { it != DrawerStyle.Categories }, cfg.drawerStyle, { it.label }, { it.description }) { s -> set { it.copy(drawerStyle = s) } }
        }
        ActionRow(Icons.AutoMirrored.Rounded.Sort, "Sortowanie", cfg.drawerSort.label, chevron = true) {
            pick.choose("Sortowanie", DrawerSort.entries, cfg.drawerSort, { it.label }) { s -> set { it.copy(drawerSort = s) } }
        }
        ActionRow(Icons.Rounded.Animation, "Animacja otwierania", cfg.drawerAnimation.label, chevron = true) {
            pick.choose("Animacja otwierania", DrawerAnimation.entries, cfg.drawerAnimation, { it.label }) { s -> set { it.copy(drawerAnimation = s) } }
        }
        SliderRow(Icons.Rounded.ViewColumn, "Kolumny", cfg.drawerColumns.toFloat(), 2f..12f, 9) { v -> set { it.copy(drawerColumns = v.toInt()) } }
        SliderRow(
            Icons.Rounded.TableRows, "Wiersze na ekranie", cfg.drawerRows.toFloat(), 0f..20f, 19,
            { if (it.toInt() == 0) "Auto" else it.toInt().toString() },
        ) { v -> set { it.copy(drawerRows = v.toInt()) } }
        SliderRow(Icons.Rounded.PhotoSizeSelectSmall, "Rozmiar ikon", cfg.iconScale, 0.6f..1.5f, 17, { "${(it * 100).toInt()}%" }) { v -> set { it.copy(iconScale = v) } }
    }
    Group("Tło") {
        SwitchRow(Icons.Rounded.Square, "Tło szuflady", null, cfg.drawerBackground) { v -> set { it.copy(drawerBackground = v) } }
        if (cfg.drawerBackground) {
            SliderRow(Icons.Rounded.Opacity, "Krycie tła", cfg.drawerOpacity, 0.3f..1f, 13, { "${(it * 100).toInt()}%" }) { v -> set { it.copy(drawerOpacity = v) } }
        }
    }
    TileGrid(
        listOfNotNull(
            Tile(Icons.Rounded.Rectangle, "Tło wyszukiwarki", cfg.drawerHeaderBackground) { v -> set { it.copy(drawerHeaderBackground = v) } },
            if (Build.VERSION.SDK_INT >= 31) Tile(Icons.Rounded.BlurOn, "Rozmycie pulpitu", cfg.blurBehindDrawer) { v -> set { it.copy(blurBehindDrawer = v) } } else null,
            Tile(Icons.Rounded.Category, "Filtr kategorii", cfg.drawerCategoryChips) { v -> set { it.copy(drawerCategoryChips = v) } },
            Tile(Icons.Rounded.ViewCarousel, "Karty kategorii", cfg.drawerCategoryTabs) { v -> set { it.copy(drawerCategoryTabs = v) } },
            Tile(Icons.Rounded.Label, "Podpisy ikon", cfg.drawerLabels) { v -> set { it.copy(drawerLabels = v) } },
            Tile(Icons.Rounded.LocalOffer, "Kategorie pod nazwą", cfg.drawerShowCategories) { v -> set { it.copy(drawerShowCategories = v) } },
            Tile(Icons.Rounded.Keyboard, "Klawiatura od razu", cfg.searchAutoFocus) { v -> set { it.copy(searchAutoFocus = v) } },
            Tile(Icons.Rounded.Contacts, "Kontakty w wynikach", cfg.drawerContacts) { v -> set { it.copy(drawerContacts = v) } },
            Tile(Icons.Rounded.Calculate, "Szybkie odpowiedzi", cfg.drawerQuickAnswers) { v -> set { it.copy(drawerQuickAnswers = v) } },
        ),
    )
}

@Composable
private fun GesturesSection(cfg: Config, set: Setter, pick: Picker) {
    Group("Na pustym pulpicie") {
        ActionRow(Icons.Rounded.ArrowUpward, "Przesunięcie w górę", cfg.swipeUp.label, chevron = true) {
            pick.choose("Przesunięcie w górę", GestureAction.entries, cfg.swipeUp, { it.label }) { g -> set { it.copy(swipeUp = g) } }
        }
        ActionRow(Icons.Rounded.ArrowDownward, "Przesunięcie w dół", cfg.swipeDown.label, chevron = true) {
            pick.choose("Przesunięcie w dół", GestureAction.entries, cfg.swipeDown, { it.label }) { g -> set { it.copy(swipeDown = g) } }
        }
        ActionRow(Icons.Rounded.TouchApp, "Podwójne dotknięcie", cfg.doubleTap.label, chevron = true) {
            pick.choose("Podwójne dotknięcie", GestureAction.entries, cfg.doubleTap, { it.label }) { g -> set { it.copy(doubleTap = g) } }
        }
    }
    Group("Dwa palce") {
        ActionRow(Icons.Rounded.ZoomIn, "Uszczypnięcie", cfg.pinchIn.label, chevron = true) {
            pick.choose("Uszczypnięcie (palce do siebie)", GestureAction.entries, cfg.pinchIn, { it.label }) { g -> set { it.copy(pinchIn = g) } }
        }
        ActionRow(Icons.Rounded.ZoomOut, "Rozsunięcie palców", cfg.pinchOut.label, chevron = true) {
            pick.choose("Rozsunięcie palców", GestureAction.entries, cfg.pinchOut, { it.label }) { g -> set { it.copy(pinchOut = g) } }
        }
        ActionRow(Icons.Rounded.KeyboardDoubleArrowUp, "Dwa palce w górę", cfg.twoFingerUp.label, chevron = true) {
            pick.choose("Dwa palce w górę", GestureAction.entries, cfg.twoFingerUp, { it.label }) { g -> set { it.copy(twoFingerUp = g) } }
        }
        ActionRow(Icons.Rounded.KeyboardDoubleArrowDown, "Dwa palce w dół", cfg.twoFingerDown.label, chevron = true) {
            pick.choose("Dwa palce w dół", GestureAction.entries, cfg.twoFingerDown, { it.label }) { g -> set { it.copy(twoFingerDown = g) } }
        }
    }
    Group {
        SwitchRow(Icons.Rounded.Vibration, "Wibracje", "Lekka wibracja przy przytrzymaniu i przeciąganiu", cfg.haptics) { v -> set { it.copy(haptics = v) } }
    }
}

private val accents = listOf(
    0xFFFFFFFF, 0xFF8AB4FF, 0xFFC58AFF, 0xFF7EE0C3, 0xFFFFD27E,
    0xFFFF8A80, 0xFF80D8FF, 0xFFB9F6CA, 0xFFF48FB1,
)

private val panelColors = listOf(
    0xFFFFFFFF, 0xFFE8EAED, 0xFF9AA0A6, 0xFF202124, 0xFF000000,
    0xFF8AB4FF, 0xFFC58AFF, 0xFF7EE0C3, 0xFFFFD27E, 0xFFFF8A80,
)

/** Kolor i krycie panelu (menu, folder) z małym podglądem w stylu tego panelu. */
@Composable
private fun PanelColorRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Long,
    opacity: Float,
    radius: Float,
    onColor: (Long) -> Unit,
    onOpacity: (Float) -> Unit,
    onRadius: (Float) -> Unit,
) {
    val panel = PanelStyle.of(color, opacity, radius)
    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontSize = 14.sp, color = Color.White)
                Text(subtitle, fontSize = 12.sp, color = Palette.TextDim)
            }
            // Podgląd na tle przypominającym tapetę
            Box(
                Modifier
                    .size(64.dp, 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4A6FA5), Color(0xFFD9A35B)))),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(46.dp, 26.dp)
                        .clip(panel.shape)
                        .background(panel.background)
                        .border(1.dp, panel.border, panel.shape),
                    contentAlignment = Alignment.Center,
                ) { Text("Aa", color = panel.content, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Row(
            Modifier.padding(start = 44.dp, top = 8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            panelColors.forEach { c ->
                val sel = (color and 0xFFFFFF) == (c and 0xFFFFFF)
                Box(
                    Modifier.size(30.dp).clip(CircleShape)
                        .background(if (sel) Color.White else Color.White.copy(alpha = 0.12f))
                        .clickable { onColor(c) }
                        .padding(if (sel) 3.dp else 2.dp)
                        .clip(CircleShape)
                        .background(Color(c)),
                )
            }
        }
    }
    SliderRow(Icons.Rounded.Opacity, "Krycie", opacity * 100f, 0f..100f, 19, { "${it.toInt()}%" }) { onOpacity(it / 100f) }
    SliderRow(Icons.Rounded.RoundedCorner, "Zaokrąglenie rogów", radius, 0f..28f, 13, { "${it.toInt()} dp" }) { onRadius(it) }
}

@Composable
private fun LookSection(cfg: Config, set: Setter, pick: Picker) {
    val ctx = LocalContext.current
    Group("Kolory") {
        if (Build.VERSION.SDK_INT >= 31) {
            SwitchRow(Icons.Rounded.ColorLens, "Kolory z tapety", "Material You zamiast czarno-białego motywu", cfg.dynamicColor) { v -> set { it.copy(dynamicColor = v) } }
        }
        if (!cfg.dynamicColor || Build.VERSION.SDK_INT < 31) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowIcon(Icons.Rounded.Brush)
                    Text("Kolor akcentu", fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
                }
                Row(
                    Modifier.padding(start = 44.dp, top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    accents.forEach { a ->
                        val sel = cfg.accent == a
                        Box(
                            Modifier.size(30.dp).clip(CircleShape)
                                .background(if (sel) Color.White else Color.White.copy(alpha = 0.12f))
                                .clickable { set { it.copy(accent = a) } }
                                .padding(if (sel) 3.dp else 2.dp)
                                .clip(CircleShape)
                                .background(Color(a)),
                        )
                    }
                }
            }
        }
    }
    Group("Ikony") {
        ActionRow(Icons.Rounded.Crop, "Kształt ikon", cfg.iconShape.label, chevron = true) {
            pick.choose("Kształt ikon", IconShape.entries, cfg.iconShape, { it.label }) { s -> set { it.copy(iconShape = s) } }
        }
    }
    Group("Menu i foldery") {
        PanelColorRow(
            icon = Icons.Rounded.Menu,
            title = "Menu kontekstowe",
            subtitle = "Przytrzymanie ikony, widżetu i aplikacji w szufladzie",
            color = cfg.menuColor,
            opacity = cfg.menuOpacity,
            radius = cfg.menuRadius,
            onColor = { c -> set { it.copy(menuColor = c) } },
            onOpacity = { o -> set { it.copy(menuOpacity = o) } },
            onRadius = { r -> set { it.copy(menuRadius = r) } },
        )
        PanelColorRow(
            icon = Icons.Rounded.Folder,
            title = "Tło folderów",
            subtitle = "Okienko z ikonami po otwarciu folderu",
            color = cfg.folderColor,
            opacity = cfg.folderOpacity,
            radius = cfg.folderRadius,
            onColor = { c -> set { it.copy(folderColor = c) } },
            onOpacity = { o -> set { it.copy(folderOpacity = o) } },
            onRadius = { r -> set { it.copy(folderRadius = r) } },
        )
        ActionRow(Icons.Rounded.RestartAlt, "Przywróć domyślny wygląd", "Biel, krycie 74% – dla menu i folderów") {
            set { it.copy(menuColor = 0xFFFFFFFF, menuOpacity = 0.74f, folderColor = 0xFFFFFFFF, folderOpacity = 0.74f, menuRadius = 6f, folderRadius = 6f) }
        }
    }
    Group {
        SwitchRow(Icons.Rounded.DarkMode, "Automatyczny motyw dzień/noc", "Ciemny motyw od 20:00 do 7:00", cfg.autoTheme) { v -> set { it.copy(autoTheme = v) } }
        ActionRow(Icons.Rounded.Wallpaper, "Tapeta", "Systemowy wybór tapety", chevron = true) { SystemActions.openWallpaperPicker(ctx) }
    }
}

@Composable
private fun EffectsSection(cfg: Config, set: Setter) {
    TileGrid(
        listOf(
            Tile(Icons.Rounded.TouchApp, "Naciśnięcie ikon", cfg.iconPressEffect) { v -> set { it.copy(iconPressEffect = v) } },
            Tile(Icons.Rounded.Vibration, "Wiggle w edycji", cfg.iconWiggleOnLongPress) { v -> set { it.copy(iconWiggleOnLongPress = v) } },
            Tile(Icons.Rounded.BubbleChart, "Cząsteczki", cfg.particleEffects) { v -> set { it.copy(particleEffects = v) } },
            Tile(Icons.Rounded.ControlCamera, "Odbijanie", cfg.iconBounceAnimation) { v -> set { it.copy(iconBounceAnimation = v) } },
            Tile(Icons.Rounded.FolderOpen, "Animacja folderów", cfg.folderOpenAnimation) { v -> set { it.copy(folderOpenAnimation = v) } },
            Tile(Icons.Rounded.Gesture, "Ślady przeciągania", cfg.swipeTrails) { v -> set { it.copy(swipeTrails = v) } },
            Tile(Icons.Rounded.Layers, "Parallax", cfg.parallaxEffect) { v -> set { it.copy(parallaxEffect = v) } },
        ),
    )
    Text(
        "Białe kafelki są włączone. Efekty animacji mogą zwiększać zużycie baterii.",
        color = Palette.TextDim, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun AiSection(vm: LauncherViewModel, cfg: Config, set: Setter) {
    val ctx = LocalContext.current
    val provider = cfg.aiProvider
    var keyVisible by remember { mutableStateOf(false) }
    var keyStatus by remember(provider) { mutableStateOf<String?>(null) }
    var modelPicker by remember { mutableStateOf(false) }
    var providerPicker by remember { mutableStateOf(false) }
    Group {
        ActionRow(Icons.Rounded.Dns, "Dostawca", provider.label, chevron = true) { providerPicker = true }
    }
    Text(
        when (provider) {
            AiProvider.Custom -> "Serwer zgodny z API OpenAI – np. Ollama (http://IP:11434/v1), LM Studio, vLLM. Klucz tylko, jeśli serwer go wymaga."
            else -> "Klucz API ${provider.label}. Każdy dostawca ma osobny klucz i model. Klucze są tylko na tym urządzeniu i nie trafiają do kopii zapasowej."
        },
        color = Palette.TextDim, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 6.dp),
    )
    Group {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (provider == AiProvider.Custom) {
                DialogTextField(
                    value = cfg.aiBaseUrl,
                    onValueChange = { v -> set { it.copy(aiBaseUrl = v.trim()) }; keyStatus = null },
                    label = "Adres serwera",
                    placeholder = "http://192.168.1.10:11434/v1",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
            DialogTextField(
                value = cfg.aiKey(),
                onValueChange = { v -> set { it.withAiKey(v.trim()) }; keyStatus = null },
                label = if (provider.needsKey) "Klucz API" else "Klucz API (opcjonalnie)",
                placeholder = provider.keyHint,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailing = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = Palette.TextDim)
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val canTest = (!provider.needsKey || cfg.aiKey().isNotBlank()) && (provider != AiProvider.Custom || cfg.aiBaseUrl.isNotBlank())
                PillButton(if (provider == AiProvider.Custom) "Sprawdź połączenie" else "Sprawdź klucz", enabled = canTest) {
                    keyStatus = "Sprawdzam…"; vm.testKey { keyStatus = it }
                }
                if (provider.keysUrl.isNotBlank()) PillButton("Zdobądź klucz", primary = false) { SystemActions.openUrl(ctx, provider.keysUrl) }
            }
            keyStatus?.let { Text(it, color = Palette.TextDim, fontSize = 13.sp) }
        }
    }
    Group {
        ActionRow(Icons.Rounded.Memory, "Model", cfg.aiModelLabel().ifBlank { "Nie wybrano – dotknij, aby wybrać" }, chevron = true) {
            vm.loadModels()
            modelPicker = true
        }
    }
    if (providerPicker) {
        val all = AiProvider.entries
        ChoiceDialog(
            title = "Dostawca AI",
            options = all.map { it.label },
            selected = all.indexOf(provider),
            descriptions = all.map { p ->
                val model = cfg.aiModelLabel(p)
                when {
                    p.needsKey && cfg.aiKey(p).isBlank() -> "Brak klucza"
                    model.isBlank() -> "Klucz zapisany · bez modelu"
                    else -> "Model: $model"
                }
            },
            onDismiss = { providerPicker = false },
            onSelect = { i -> set { it.copy(aiProvider = all[i]) } },
        )
    }
    if (modelPicker) ModelPickerDialog(vm, onDismiss = { modelPicker = false })
}

@Composable
private fun UpdatesSection(vm: LauncherViewModel, cfg: Config, set: Setter) {
    val ctx = LocalContext.current
    val update by vm.update.collectAsStateWithLifecycle()
    var repoSearch by remember { mutableStateOf(false) }
    var manualUpdate by remember { mutableStateOf(false) }
    Group {
        ActionRow(Icons.Rounded.Info, "Zainstalowana wersja", BuildConfig.VERSION_NAME) {}
        ActionRow(Icons.Rounded.RestartAlt, "Uruchom ponownie", "Zamyka i od razu włącza launcher") { SystemActions.restartApp(ctx) }
        ActionRow(Icons.Rounded.Code, "Repozytorium na GitHubie", cfg.updateRepo, chevron = true) { repoSearch = true }
        SwitchRow(Icons.Rounded.Autorenew, "Sprawdzaj automatycznie", "Przy uruchomieniu launchera", cfg.autoUpdate) { v -> set { it.copy(autoUpdate = v) } }
    }
    Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton("Sprawdź teraz") { manualUpdate = true; vm.checkUpdates() }
        Text(
            when (val u = update) {
                UpdateState.Checking -> "Sprawdzam…"
                UpdateState.UpToDate -> "Masz najnowszą wersję"
                is UpdateState.Available -> "Dostępna ${u.info.versionName}"
                is UpdateState.Downloading -> "Pobieranie ${(u.progress * 100).toInt()}%"
                is UpdateState.Ready -> "Gotowe do instalacji"
                is UpdateState.Error -> u.message
                UpdateState.Idle -> ""
            },
            color = Palette.TextDim, fontSize = 13.sp, maxLines = 2,
        )
    }
    if (repoSearch) RepoSearchDialog(vm, onDismiss = { repoSearch = false })
    if (manualUpdate) UpdateDialog(vm, update, onlyAuto = false, onClosed = { manualUpdate = false })
}

@Composable
private fun BackupSection(vm: LauncherViewModel, cfg: Config, set: Setter) {
    var confirmReset by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importFrom) }
    Group("Lokalnie") {
        ActionRow(Icons.Rounded.FileUpload, "Eksportuj ustawienia i układ", "Plik JSON") { exportLauncher.launch("opendesktop-${LocalDate.now()}.json") }
        ActionRow(Icons.Rounded.FileDownload, "Importuj z pliku") { importLauncher.launch(arrayOf("application/json", "*/*")) }
        ActionRow(Icons.Rounded.RestartAlt, "Przywróć domyślny układ", "Ekrany, dok, foldery i widżety", danger = true) { confirmReset = true }
    }
    if (confirmReset) {
        DarkDialog(
            title = "Przywrócić domyślny układ?",
            onDismiss = { confirmReset = false },
            buttons = {
                PillButton("Anuluj", primary = false) { confirmReset = false }
                PillButton("Przywróć", danger = true) { vm.resetLayout(); confirmReset = false }
            },
        ) {
            Text(
                "Ekrany, dok, foldery i widżety zostaną zastąpione układem startowym. Kategorie zostaną wygenerowane ponownie.",
                color = Palette.TextDim, fontSize = 14.sp,
            )
        }
    }
}

// ------------------------------------------------------------------ dialog wyboru

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    descriptions: List<String>? = null,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    DarkDialog(title = title, onDismiss = onDismiss, buttons = { PillButton("Zamknij", primary = false, onClick = onDismiss) }) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sel) Palette.Accent else Palette.Group)
                    .clickable { onSelect(i); onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text(o, color = if (sel) Palette.Ink else Color.White, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                if (descriptions != null) {
                    Text(descriptions[i], fontSize = 11.sp, lineHeight = 14.sp, color = if (sel) Palette.Ink.copy(alpha = 0.7f) else Palette.TextDim)
                }
            }
        }
    }
}
