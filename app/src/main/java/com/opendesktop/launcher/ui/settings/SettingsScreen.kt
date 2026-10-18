package com.opendesktop.launcher.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opendesktop.launcher.BuildConfig
import com.opendesktop.launcher.data.Config
import com.opendesktop.launcher.data.DrawerAnimation
import com.opendesktop.launcher.data.DrawerSort
import com.opendesktop.launcher.data.DrawerStyle
import com.opendesktop.launcher.data.GestureAction
import com.opendesktop.launcher.data.IconShape
import com.opendesktop.launcher.data.PageTransition
import com.opendesktop.launcher.data.SystemActions
import com.opendesktop.launcher.data.findActivity
import com.opendesktop.launcher.ui.LauncherViewModel
import com.opendesktop.launcher.ui.UpdateState
import com.opendesktop.launcher.ui.components.ChoiceDialog
import java.time.LocalDate

private val accents = listOf(
    0xFF8AB4FF, 0xFFC58AFF, 0xFF7EE0C3, 0xFFFFD27E, 0xFFFF8A80,
    0xFF80D8FF, 0xFFB9F6CA, 0xFFFFAB91, 0xFFF48FB1, 0xFFE6EE9C,
)

@Composable
fun SettingsScreen(vm: LauncherViewModel, onClose: () -> Unit, onOpenCategories: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var choice by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    var modelPicker by remember { mutableStateOf(false) }
    var repoSearch by remember { mutableStateOf(false) }
    var keyVisible by remember { mutableStateOf(false) }
    var keyStatus by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var manualUpdate by remember { mutableStateOf(false) }
    fun set(f: (Config) -> Config) = vm.updateConfig(f)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importFrom)
    }

    fun <T> pick(title: String, options: List<T>, current: T, label: (T) -> String, desc: ((T) -> String)? = null, onPick: (T) -> Unit) {
        choice = {
            ChoiceDialog(
                title = title,
                options = options.map(label),
                selected = options.indexOf(current),
                descriptions = desc?.let { d -> options.map(d) },
                onDismiss = { choice = null },
                onSelect = { onPick(options[it]) },
            )
        }
    }

    Surface(color = Color(0xFA0E121C), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", tint = Color.White) }
                Text("Ustawienia OpenDesktop", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!SystemActions.isDefaultLauncher(ctx)) {
                    item {
                        Card {
                            Text("OpenDesktop nie jest domyślnym ekranem głównym", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { ctx.findActivity()?.let { SystemActions.requestDefaultLauncher(it) } }) { Text("Ustaw jako domyślny") }
                        }
                    }
                }

                item {
                    Section("Pulpit") {
                        SliderRow("Kolumny", cfg.columns.toFloat(), 3f..10f, 6) { vm.setGrid(it.toInt(), cfg.rows) }
                        SliderRow("Wiersze", cfg.rows.toFloat(), 3f..12f, 8) { vm.setGrid(cfg.columns, it.toInt()) }
                        SwitchRow("Nakładanie się elementów", "Widżety i ikony mogą zachodzić na siebie", cfg.allowOverlap) { vm.setOverlap(it) }
                        ChoiceRow("Przejście między ekranami", cfg.transition.label) {
                            pick("Przejście między ekranami", PageTransition.entries, cfg.transition, { it.label }) { t -> set { it.copy(transition = t) } }
                        }
                        SwitchRow("Wskaźnik ekranów", null, cfg.showPageIndicator) { v -> set { it.copy(showPageIndicator = v) } }
                        SwitchRow("Przewijanie tapety", "Tapeta przesuwa się razem z ekranami", cfg.wallpaperScroll) { v -> set { it.copy(wallpaperScroll = v) } }
                        SwitchRow("Siatka w trybie edycji", null, cfg.editGridLines) { v -> set { it.copy(editGridLines = v) } }
                    }
                }

                item {
                    Section("Ikony") {
                        SliderRow("Rozmiar ikon", cfg.iconScale, 0.6f..1.5f, 17, format = { "${(it * 100).toInt()}%" }) { v -> set { it.copy(iconScale = v) } }
                        SwitchRow("Podpisy na pulpicie", null, cfg.showLabels) { v -> set { it.copy(showLabels = v) } }
                        SliderRow("Wielkość podpisów", cfg.labelSize, 9f..16f, 6, format = { "${it.toInt()} sp" }) { v -> set { it.copy(labelSize = v) } }
                        ChoiceRow("Kształt ikon", cfg.iconShape.label) {
                            pick("Kształt ikon", IconShape.entries, cfg.iconShape, { it.label }) { s -> set { it.copy(iconShape = s) } }
                        }
                    }
                }

                item {
                    Section("Dok") {
                        SwitchRow("Pokaż dok", null, cfg.dockEnabled) { v -> set { it.copy(dockEnabled = v) } }
                        SliderRow("Miejsca w doku", cfg.dockCount.toFloat(), 3f..7f, 3) { v -> set { it.copy(dockCount = v.toInt()) } }
                        SliderRow("Przezroczystość tła", cfg.dockOpacity, 0f..0.6f, 11, format = { "${(it * 100).toInt()}%" }) { v -> set { it.copy(dockOpacity = v) } }
                        SwitchRow("Podpisy w doku", null, cfg.dockLabels) { v -> set { it.copy(dockLabels = v) } }
                    }
                }

                item {
                    Section("Szuflada aplikacji") {
                        ChoiceRow("Motyw", cfg.drawerStyle.label) {
                            pick("Motyw szuflady", DrawerStyle.entries, cfg.drawerStyle, { it.label }, { it.description }) { s -> set { it.copy(drawerStyle = s) } }
                        }
                        ChoiceRow("Sortowanie", cfg.drawerSort.label) {
                            pick("Sortowanie", DrawerSort.entries, cfg.drawerSort, { it.label }) { s -> set { it.copy(drawerSort = s) } }
                        }
                        ChoiceRow("Animacja otwierania", cfg.drawerAnimation.label) {
                            pick("Animacja otwierania", DrawerAnimation.entries, cfg.drawerAnimation, { it.label }) { s -> set { it.copy(drawerAnimation = s) } }
                        }
                        SliderRow("Kolumny", cfg.drawerColumns.toFloat(), 3f..8f, 4) { v -> set { it.copy(drawerColumns = v.toInt()) } }
                        SliderRow("Krycie tła", cfg.drawerOpacity, 0.3f..1f, 13, format = { "${(it * 100).toInt()}%" }) { v -> set { it.copy(drawerOpacity = v) } }
                        if (Build.VERSION.SDK_INT >= 31) SwitchRow("Rozmycie pulpitu", null, cfg.blurBehindDrawer) { v -> set { it.copy(blurBehindDrawer = v) } }
                        SwitchRow("Filtr kategorii (chipy)", null, cfg.drawerCategoryChips) { v -> set { it.copy(drawerCategoryChips = v) } }
                        SwitchRow("Podpisy ikon", null, cfg.drawerLabels) { v -> set { it.copy(drawerLabels = v) } }
                        SwitchRow("Klawiatura od razu", "Po otwarciu szuflady aktywuj wyszukiwanie", cfg.searchAutoFocus) { v -> set { it.copy(searchAutoFocus = v) } }
                        ChoiceRow("Kategorie aplikacji", "Zarządzaj, przypisuj, generuj z AI", onClick = onOpenCategories)
                    }
                }

                item {
                    Section("Gesty") {
                        ChoiceRow("Przesunięcie w górę", cfg.swipeUp.label) {
                            pick("Przesunięcie w górę", GestureAction.entries, cfg.swipeUp, { it.label }) { g -> set { it.copy(swipeUp = g) } }
                        }
                        ChoiceRow("Przesunięcie w dół", cfg.swipeDown.label) {
                            pick("Przesunięcie w dół", GestureAction.entries, cfg.swipeDown, { it.label }) { g -> set { it.copy(swipeDown = g) } }
                        }
                        ChoiceRow("Podwójne dotknięcie", cfg.doubleTap.label) {
                            pick("Podwójne dotknięcie", GestureAction.entries, cfg.doubleTap, { it.label }) { g -> set { it.copy(doubleTap = g) } }
                        }
                        SwitchRow("Wibracje", null, cfg.haptics) { v -> set { it.copy(haptics = v) } }
                    }
                }

                item {
                    Section("Wygląd") {
                        if (Build.VERSION.SDK_INT >= 31) SwitchRow("Kolory z tapety (Material You)", null, cfg.dynamicColor) { v -> set { it.copy(dynamicColor = v) } }
                        if (!cfg.dynamicColor || Build.VERSION.SDK_INT < 31) {
                            Text("Kolor akcentu", color = Color.White, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(accents) { a ->
                                    Box(
                                        Modifier.size(38.dp).clip(CircleShape).background(Color(a))
                                            .then(if (cfg.accent == a) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                                            .clickable { set { it.copy(accent = a) } },
                                    )
                                }
                            }
                        }
                        ChoiceRow("Tapeta", "Systemowy wybór tapety") { SystemActions.openWallpaperPicker(ctx) }
                    }
                }

                item {
                    Section("Sztuczna inteligencja (OpenRouter)") {
                        Text(
                            "Klucz API z openrouter.ai/keys. Przechowywany tylko na tym urządzeniu i nie trafia do kopii zapasowej.",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = cfg.openRouterKey,
                            onValueChange = { v -> set { it.copy(openRouterKey = v.trim()) }; keyStatus = null },
                            label = { Text("Klucz API") },
                            placeholder = { Text("sk-or-v1-…") },
                            singleLine = true,
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = { keyStatus = "Sprawdzam…"; vm.testKey { keyStatus = it } }, enabled = cfg.openRouterKey.isNotBlank()) { Text("Sprawdź klucz") }
                            FilledTonalButton(onClick = { SystemActions.openUrl(ctx, "https://openrouter.ai/keys") }) { Text("Zdobądź klucz") }
                        }
                        keyStatus?.let { Text(it, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                        ChoiceRow("Model", cfg.aiModelName.ifBlank { cfg.aiModel.ifBlank { "Nie wybrano — dotknij, aby wybrać" } }) {
                            vm.loadModels()
                            modelPicker = true
                        }
                    }
                }

                item {
                    Section("Aktualizacje") {
                        Text("Zainstalowana wersja: ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        ChoiceRow("Repozytorium na GitHubie", cfg.updateRepo) { repoSearch = true }
                        SwitchRow("Sprawdzaj automatycznie", "Przy uruchomieniu launchera", cfg.autoUpdate) { v -> set { it.copy(autoUpdate = v) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { manualUpdate = true; vm.checkUpdates() }) { Text("Sprawdź teraz") }
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
                                color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                item {
                    Section("Kopia zapasowa") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { exportLauncher.launch("opendesktop-${LocalDate.now()}.json") }) { Text("Eksportuj układ") }
                            FilledTonalButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("Importuj") }
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { confirmReset = true }) { Text("Przywróć domyślny układ pulpitu", color = MaterialTheme.colorScheme.error) }
                    }
                }

                item {
                    Text(
                        "OpenDesktop ${BuildConfig.VERSION_NAME} · otwarty launcher na Androida",
                        color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    )
                }
            }
        }
    }

    choice?.invoke()
    if (modelPicker) ModelPickerDialog(vm, onDismiss = { modelPicker = false })
    if (repoSearch) RepoSearchDialog(vm, onDismiss = { repoSearch = false })
    if (manualUpdate) UpdateDialog(vm, update, onlyAuto = false, onClosed = { manualUpdate = false })
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Przywrócić domyślny układ?") },
            text = { Text("Ekrany, dok, foldery i widżety zostaną zastąpione układem startowym. Kategorie zostaną wygenerowane ponownie.") },
            confirmButton = { TextButton(onClick = { vm.resetLayout(); confirmReset = false }) { Text("Przywróć", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Anuluj") } },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
        }
        Switch(value, onChange)
    }
}

@Composable
private fun ChoiceRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(value, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String = { it.toInt().toString() },
    onChange: (Float) -> Unit,
) {
    // Lokalna wartość podczas przesuwania, zapis po puszczeniu (siatka przelicza się raz)
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
