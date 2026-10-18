package com.opendesktop.launcher.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opendesktop.launcher.BuildConfig
import com.opendesktop.launcher.R
import com.opendesktop.launcher.data.*
import com.opendesktop.launcher.ui.LauncherViewModel

@Composable
fun SettingsScreen(vm: LauncherViewModel, onClose: () -> Unit, onOpenCategories: () -> Unit = {}) {
    val ctx = LocalContext.current
    val cfg by vm.config.collectAsStateWithLifecycle()
    fun set(t: (Config) -> Config) = vm.updateConfig(t)

    var choice by remember { mutableStateOf<ChoiceState<*>?>(null) }

    data class ChoiceState<T>(
        val title: String,
        val options: List<T>,
        val current: T,
        val label: (T) -> String,
        val desc: ((T) -> String)? = null,
        val onPick: (T) -> Unit,
    )

    fun <T> pick(title: String, options: List<T>, current: T, label: (T) -> String, desc: ((T) -> String)? = null, onPick: (T) -> Unit) {
        choice = ChoiceState(title, options, current, label, desc, onPick)
    }

    choice?.let { c ->
        @Suppress("UNCHECKED_CAST")
        val c2 = c as ChoiceState<Any>
        ChoiceDialog(
            title = c2.title,
            options = c2.options.map(c2.label),
            selected = c2.options.indexOf(c2.current),
            descriptions = c2.desc?.let { d: (Any) -> String -> c2.options.map(d) },
            onDismiss = { choice = null },
            onSelect = { c2.onPick(c2.options[it]) },
        )
    }

    Surface(color = Color(0xFF0A0E1A), modifier = Modifier.fillMaxSize()) {
        Box {
            // Gradient tło jak w OpenPDF
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1F3A),
                            Color(0xFF0A0E1A),
                            Color(0xFF0A0E1A),
                        ),
                    ),
                )
            }

            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // Nagłówek
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                ),
                            ),
                        )
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", tint = Color.White)
                        }
                        Image(
                            painter = painterResource(R.drawable.opendesktop_mark),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Ustawienia",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "OpenDesktop",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ostrzeżenie jeśli nie jest domyślnym launcherem
                    if (!SystemActions.isDefaultLauncher(ctx)) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "OpenDesktop nie jest domyślnym ekranem głównym",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = {
                                        ctx.findActivity()?.let { SystemActions.requestDefaultLauncher(it) }
                                    }) {
                                        Text("Ustaw jako domyślny")
                                    }
                                }
                            }
                        }
                    }

                    // Sekcja: Pulpit
                    item { SectionHeader(Icons.Rounded.Home, "Pulpit") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SliderRow(
                                "Kolumny",
                                cfg.columns.toFloat(),
                                3f..10f,
                                6
                            ) { vm.setGrid(it.toInt(), cfg.rows) }
                            SliderRow(
                                "Wiersze",
                                cfg.rows.toFloat(),
                                3f..12f,
                                8
                            ) { vm.setGrid(cfg.columns, it.toInt()) }
                            SettingTile(
                                icon = Icons.Rounded.ViewModule,
                                label = "Nakładanie się elementów",
                                description = "Widżety i ikony mogą zachodzić na siebie",
                                value = cfg.allowOverlap,
                                onChange = { vm.setOverlap(it) }
                            )
                            ActionTile(
                                icon = Icons.Rounded.SwapHoriz,
                                label = "Przejście między ekranami",
                                description = cfg.transition.label,
                                onClick = {
                                    pick(
                                        "Przejście między ekranami",
                                        PageTransition.entries,
                                        cfg.transition,
                                        { it.label }) { t -> set { it.copy(transition = t) } }
                                }
                            )
                            SettingTile(
                                icon = Icons.Rounded.FiberManualRecord,
                                label = "Wskaźnik ekranów",
                                value = cfg.showPageIndicator,
                                onChange = { v -> set { it.copy(showPageIndicator = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.Wallpaper,
                                label = "Przewijanie tapety",
                                description = "Tapeta przesuwa się razem z ekranami",
                                value = cfg.wallpaperScroll,
                                onChange = { v -> set { it.copy(wallpaperScroll = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.GridOn,
                                label = "Siatka w trybie edycji",
                                value = cfg.editGridLines,
                                onChange = { v -> set { it.copy(editGridLines = v) } }
                            )
                        }
                    }

                    // Sekcja: Ikony
                    item { SectionHeader(Icons.Rounded.Apps, "Ikony") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Pulpit",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            SliderRow(
                                "Rozmiar ikon na pulpicie",
                                cfg.desktopIconScale,
                                0.8f..1.8f,
                                19,
                                format = { "${(it * 100).toInt()}%" }
                            ) { v -> set { it.copy(desktopIconScale = v) } }
                            SettingTile(
                                icon = Icons.Rounded.Label,
                                label = "Podpisy na pulpicie",
                                value = cfg.showLabels,
                                onChange = { v -> set { it.copy(showLabels = v) } }
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Szuflada i dok",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            SliderRow(
                                "Rozmiar ikon",
                                cfg.iconScale,
                                0.6f..1.5f,
                                17,
                                format = { "${(it * 100).toInt()}%" }
                            ) { v -> set { it.copy(iconScale = v) } }
                            SliderRow(
                                "Wielkość podpisów",
                                cfg.labelSize,
                                9f..16f,
                                6,
                                format = { "${it.toInt()} sp" }
                            ) { v -> set { it.copy(labelSize = v) } }
                            ActionTile(
                                icon = Icons.Rounded.Crop,
                                label = "Kształt ikon",
                                description = cfg.iconShape.label,
                                onClick = {
                                    pick("Kształt ikon", IconShape.entries, cfg.iconShape, { it.label }) { s ->
                                        set { it.copy(iconShape = s) }
                                    }
                                }
                            )
                        }
                    }

                    // Sekcja: Dok
                    item { SectionHeader(Icons.Rounded.Dashboard, "Dok") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingTile(
                                icon = Icons.Rounded.ViewCarousel,
                                label = "Pokaż dok",
                                value = cfg.dockEnabled,
                                onChange = { v -> set { it.copy(dockEnabled = v) } }
                            )
                            SliderRow(
                                "Miejsca w doku",
                                cfg.dockCount.toFloat(),
                                3f..7f,
                                3
                            ) { v -> set { it.copy(dockCount = v.toInt()) } }
                            SettingTile(
                                icon = Icons.Rounded.Square,
                                label = "Tło doku",
                                value = cfg.dockBackground,
                                onChange = { v -> set { it.copy(dockBackground = v) } }
                            )
                            if (cfg.dockBackground) {
                                SliderRow(
                                    "Przezroczystość tła",
                                    cfg.dockOpacity,
                                    0f..0.6f,
                                    11,
                                    format = { "${(it * 100).toInt()}%" }
                                ) { v -> set { it.copy(dockOpacity = v) } }
                            }
                            SettingTile(
                                icon = Icons.Rounded.Label,
                                label = "Podpisy w doku",
                                value = cfg.dockLabels,
                                onChange = { v -> set { it.copy(dockLabels = v) } }
                            )
                        }
                    }

                    // Sekcja: Szuflada aplikacji
                    item { SectionHeader(Icons.Rounded.Apps, "Szuflada aplikacji") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionTile(
                                icon = Icons.Rounded.Style,
                                label = "Motyw",
                                description = cfg.drawerStyle.label,
                                onClick = {
                                    pick(
                                        "Motyw szuflady",
                                        DrawerStyle.entries,
                                        cfg.drawerStyle,
                                        { it.label },
                                        { it.description }
                                    ) { s -> set { it.copy(drawerStyle = s) } }
                                }
                            )
                            ActionTile(
                                icon = Icons.Rounded.Sort,
                                label = "Sortowanie",
                                description = cfg.drawerSort.label,
                                onClick = {
                                    pick("Sortowanie", DrawerSort.entries, cfg.drawerSort, { it.label }) { s ->
                                        set { it.copy(drawerSort = s) }
                                    }
                                }
                            )
                            ActionTile(
                                icon = Icons.Rounded.Animation,
                                label = "Animacja otwierania",
                                description = cfg.drawerAnimation.label,
                                onClick = {
                                    pick(
                                        "Animacja otwierania",
                                        DrawerAnimation.entries,
                                        cfg.drawerAnimation,
                                        { it.label }
                                    ) { s -> set { it.copy(drawerAnimation = s) } }
                                }
                            )
                            SliderRow(
                                "Kolumny",
                                cfg.drawerColumns.toFloat(),
                                3f..8f,
                                4
                            ) { v -> set { it.copy(drawerColumns = v.toInt()) } }
                            SettingTile(
                                icon = Icons.Rounded.Square,
                                label = "Tło szuflady",
                                value = cfg.drawerBackground,
                                onChange = { v -> set { it.copy(drawerBackground = v) } }
                            )
                            if (cfg.drawerBackground) {
                                SliderRow(
                                    "Krycie tła",
                                    cfg.drawerOpacity,
                                    0.3f..1f,
                                    13,
                                    format = { "${(it * 100).toInt()}%" }
                                ) { v -> set { it.copy(drawerOpacity = v) } }
                            }
                            SettingTile(
                                icon = Icons.Rounded.Rectangle,
                                label = "Tło paska wyszukiwania",
                                value = cfg.drawerHeaderBackground,
                                onChange = { v -> set { it.copy(drawerHeaderBackground = v) } }
                            )
                            if (Build.VERSION.SDK_INT >= 31) {
                                SettingTile(
                                    icon = Icons.Rounded.BlurOn,
                                    label = "Rozmycie pulpitu",
                                    value = cfg.blurBehindDrawer,
                                    onChange = { v -> set { it.copy(blurBehindDrawer = v) } }
                                )
                            }
                            SettingTile(
                                icon = Icons.Rounded.Category,
                                label = "Filtr kategorii (chipy)",
                                value = cfg.drawerCategoryChips,
                                onChange = { v -> set { it.copy(drawerCategoryChips = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.Label,
                                label = "Podpisy ikon",
                                value = cfg.drawerLabels,
                                onChange = { v -> set { it.copy(drawerLabels = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.Keyboard,
                                label = "Klawiatura od razu",
                                description = "Po otwarciu szuflady aktywuj wyszukiwanie",
                                value = cfg.searchAutoFocus,
                                onChange = { v -> set { it.copy(searchAutoFocus = v) } }
                            )
                        }
                    }

                    // Sekcja: Gesty
                    item { SectionHeader(Icons.Rounded.Gesture, "Gesty") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionTile(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "Przesunięcie w górę",
                                description = cfg.swipeUp.label,
                                onClick = {
                                    pick(
                                        "Przesunięcie w górę",
                                        GestureAction.entries,
                                        cfg.swipeUp,
                                        { it.label }
                                    ) { g -> set { it.copy(swipeUp = g) } }
                                }
                            )
                            ActionTile(
                                icon = Icons.Rounded.ArrowDownward,
                                label = "Przesunięcie w dół",
                                description = cfg.swipeDown.label,
                                onClick = {
                                    pick(
                                        "Przesunięcie w dół",
                                        GestureAction.entries,
                                        cfg.swipeDown,
                                        { it.label }
                                    ) { g -> set { it.copy(swipeDown = g) } }
                                }
                            )
                            ActionTile(
                                icon = Icons.Rounded.TouchApp,
                                label = "Podwójne dotknięcie",
                                description = cfg.doubleTap.label,
                                onClick = {
                                    pick(
                                        "Podwójne dotknięcie",
                                        GestureAction.entries,
                                        cfg.doubleTap,
                                        { it.label }
                                    ) { g -> set { it.copy(doubleTap = g) } }
                                }
                            )
                            SettingTile(
                                icon = Icons.Rounded.Vibration,
                                label = "Wibracje",
                                value = cfg.haptics,
                                onChange = { v -> set { it.copy(haptics = v) } }
                            )
                        }
                    }

                    // Sekcja: Wygląd
                    item { SectionHeader(Icons.Rounded.Palette, "Wygląd") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (Build.VERSION.SDK_INT >= 31) {
                                SettingTile(
                                    icon = Icons.Rounded.ColorLens,
                                    label = "Kolory z tapety (Material You)",
                                    value = cfg.dynamicColor,
                                    onChange = { v -> set { it.copy(dynamicColor = v) } }
                                )
                            }
                            if (!cfg.dynamicColor || Build.VERSION.SDK_INT < 31) {
                                Text(
                                    "Kolor akcentu",
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(accents) { a ->
                                        Box(
                                            Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(a))
                                                .then(
                                                    if (cfg.accent == a) Modifier.border(
                                                        3.dp,
                                                        Color.White,
                                                        CircleShape
                                                    ) else Modifier
                                                )
                                                .clickable { set { it.copy(accent = a) } },
                                        )
                                    }
                                }
                            }
                            SettingTile(
                                icon = Icons.Rounded.DarkMode,
                                label = "Automatyczny motyw dzień/noc",
                                description = "Ciemny motyw od 20:00 do 7:00",
                                value = cfg.autoTheme,
                                onChange = { v -> set { it.copy(autoTheme = v) } }
                            )
                            ActionTile(
                                icon = Icons.Rounded.Wallpaper,
                                label = "Tapeta",
                                description = "Systemowy wybór tapety",
                                onClick = { SystemActions.openWallpaperPicker(ctx) }
                            )
                        }
                    }

                    // Sekcja: Efekty i Animacje
                    item { SectionHeader(Icons.Rounded.Animation, "Efekty i Animacje") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingTile(
                                icon = Icons.Rounded.TouchApp,
                                label = "Efekt naciśnięcia ikon",
                                description = "Ikony kurczą się przy dotknięciu",
                                value = cfg.iconPressEffect,
                                onChange = { v -> set { it.copy(iconPressEffect = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.Vibration,
                                label = "Wiggle przy przytrzymaniu",
                                description = "Ikony się trzęsą w trybie edycji",
                                value = cfg.iconWiggleOnLongPress,
                                onChange = { v -> set { it.copy(iconWiggleOnLongPress = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.BubbleChart,
                                label = "Cząsteczki w tle",
                                description = "Animowane cząsteczki poruszające się po ekranie",
                                value = cfg.particleEffects,
                                onChange = { v -> set { it.copy(particleEffects = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.ControlCamera,
                                label = "Animacja odbijania",
                                description = "Ikony odbijają się przy uruchomieniu",
                                value = cfg.iconBounceAnimation,
                                onChange = { v -> set { it.copy(iconBounceAnimation = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.FolderOpen,
                                label = "Animacja folderów",
                                description = "Płynne otwieranie i zamykanie folderów",
                                value = cfg.folderOpenAnimation,
                                onChange = { v -> set { it.copy(folderOpenAnimation = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.Gesture,
                                label = "Ślady przy przeciąganiu",
                                description = "Wizualne ślady podczas drag & drop",
                                value = cfg.swipeTrails,
                                onChange = { v -> set { it.copy(swipeTrails = v) } }
                            )
                            SettingTile(
                                icon = Icons.Rounded.Layers,
                                label = "Efekt parallax",
                                description = "Tapeta przesuwa się z większym opóźnieniem",
                                value = cfg.parallaxEffect,
                                onChange = { v -> set { it.copy(parallaxEffect = v) } }
                            )
                        }
                    }

                    // Sekcja: O aplikacji
                    item { SectionHeader(Icons.Rounded.Info, "O aplikacji") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionTile(
                                icon = Icons.Rounded.Info,
                                label = "Wersja",
                                description = "OpenDesktop ${BuildConfig.VERSION_NAME}",
                                onClick = { }
                            )
                        }
                    }

                    // TODO: Więcej sekcji będzie dodane dalej (AI, Backup, Aktualizacje)
                }
            }
        }
    }
}

// Lista kolorów akcentu
private val accents = listOf(
    0xFF2196F3, 0xFF9C27B0, 0xFFE91E63, 0xFFF44336,
    0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF00BCD4,
)

// Komponenty pomocnicze
@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String = { it.toInt().toString() },
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(title, color = Color.White, modifier = Modifier.weight(1f))
            Text(format(local), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    descriptions: List<String>? = null,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { i ->
                    val isSelected = i == selected
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(i)
                                onDismiss()
                            }
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(12.dp)
                    ) {
                        Text(
                            options[i],
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                        )
                        if (descriptions != null) {
                            Text(
                                descriptions[i],
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zamknij") }
        }
    )
}

