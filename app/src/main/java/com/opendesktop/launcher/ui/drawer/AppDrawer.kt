package com.opendesktop.launcher.ui.drawer

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.Category
import com.opendesktop.launcher.data.Config
import com.opendesktop.launcher.data.DrawerSort
import com.opendesktop.launcher.data.DrawerStyle
import com.opendesktop.launcher.data.SystemActions
import com.opendesktop.launcher.data.Workspace
import com.opendesktop.launcher.ui.LauncherViewModel

private const val NO_CATEGORY = "__none__"

@Composable
fun AppDrawer(
    vm: LauncherViewModel,
    cfg: Config,
    ws: Workspace,
    apps: List<AppInfo>,
    loaded: Boolean,
    focusRequest: Int,
    menuKey: String?,
    onMenuKey: (String?) -> Unit,
    onLaunch: (AppInfo, Rect) -> Unit,
    onDragStart: (AppInfo, Rect) -> Unit,
    onDragEnd: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit,
    closeConnection: NestedScrollConnection,
    headerModifier: Modifier,
    menu: @Composable (AppInfo) -> Unit,
) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<String?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var styleMenu by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val aiResults by vm.aiSearch.collectAsStateWithLifecycle()
    val aiBusy by vm.aiSearchBusy.collectAsStateWithLifecycle()

    LaunchedEffect(focusRequest) {
        if (focusRequest > 0) runCatching { focus.requestFocus(); keyboard?.show() }
    }
    LaunchedEffect(query) { if (query.isBlank()) vm.clearAiSearch() }

    val catById = remember(ws.categories) { ws.categories.associateBy { it.id } }
    val label: (AppInfo) -> String = { ws.renamed[it.key] ?: it.label }
    val categoriesOf: (AppInfo) -> List<Category> = { a -> ws.assignments[a.key].orEmpty().mapNotNull { catById[it] } }

    val visible = remember(apps, ws, query, filter, showHidden, cfg.drawerSort) {
        val q = query.trim()
        val base = apps.filter { showHidden || it.key !in ws.hidden }
            .filter {
                q.isEmpty() || label(it).contains(q, true) || it.packageName.contains(q, true) ||
                    categoriesOf(it).any { c -> c.name.contains(q, true) }
            }
            .filter { a ->
                when (filter) {
                    null -> true
                    NO_CATEGORY -> ws.assignments[a.key].isNullOrEmpty()
                    else -> ws.assignments[a.key]?.contains(filter) == true
                }
            }
        val sorted = when (cfg.drawerSort) {
            DrawerSort.Usage -> base.sortedWith(compareByDescending<AppInfo> { ws.usage[it.key] ?: 0 }.thenBy { label(it).lowercase() })
            DrawerSort.Installed -> base.sortedByDescending { it.installTime }
            else -> base.sortedBy { label(it).lowercase() }
        }
        if (q.isNotEmpty()) sorted.sortedByDescending { label(it).startsWith(q, true) } else sorted
    }

    val sections = remember(visible, cfg.drawerSort, ws.categories, query) {
        when {
            query.isNotBlank() -> listOf(DrawerSection("search", null, visible))
            cfg.drawerSort == DrawerSort.Category -> {
                val list = ws.categories.mapNotNull { c ->
                    val inCat = visible.filter { ws.assignments[it.key]?.contains(c.id) == true }
                    if (inCat.isEmpty()) null else DrawerSection(c.id, (c.emoji + "  " + c.name).trim(), inCat)
                }
                val none = visible.filter { ws.assignments[it.key].isNullOrEmpty() }
                list + if (none.isEmpty()) emptyList() else listOf(DrawerSection("none", "Bez kategorii", none))
            }
            cfg.drawerSort == DrawerSort.Name && visible.size > 30 ->
                visible.groupBy { label(it).firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() }?.toString() ?: "#" }
                    .map { (l, list) -> DrawerSection("l_$l", l, list) }
            cfg.drawerSort == DrawerSort.Usage -> listOf(DrawerSection("usage", "Najczęściej używane", visible))
            cfg.drawerSort == DrawerSort.Installed -> listOf(DrawerSection("inst", "Ostatnio zainstalowane", visible))
            else -> listOf(DrawerSection("all", null, visible))
        }
    }

    val cb = DrawerCallbacks(
        label = label,
        categoriesOf = categoriesOf,
        shape = cfg.iconShape,
        labelSize = cfg.labelSize,
        showLabels = cfg.drawerLabels,
        menuKey = menuKey,
        onTap = { app, r -> focusManager.clearFocus(); onLaunch(app, r) },
        onLongPress = { app -> focusManager.clearFocus(); onMenuKey(app.key) },
        onDragStart = { app, r -> onMenuKey(null); onDragStart(app, r) },
        onDragEnd = onDragEnd,
        menu = menu,
    )

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Nagłówek: uchwyt + wyszukiwarka + menu
        Column(headerModifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.45f)))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (cfg.drawerHeaderBackground) Modifier.background(Color.White.copy(alpha = 0.12f))
                            else Modifier
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Szukaj w ${apps.size} aplikacjach", color = Color.White.copy(alpha = 0.5f), maxLines = 1)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                val first = visible.firstOrNull()
                                if (first != null) onLaunch(first, Rect.Zero) else if (query.isNotBlank()) SystemActions.webSearch(ctx, query)
                            }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close, "Wyczyść", tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp).clip(CircleShape).clickable { query = "" },
                        )
                    }
                }
                // Jeden przycisk: sortowanie, ukryte, motyw, kategorie i ustawienia w jednym menu
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Rounded.MoreVert, "Menu szuflady", tint = Color.White) }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        Text("Sortowanie", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        DrawerSort.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.label) },
                                leadingIcon = { if (cfg.drawerSort == s) Icon(Icons.Rounded.Check, null) else Spacer(Modifier.size(24.dp)) },
                                onClick = { vm.updateConfig { it.copy(drawerSort = s) }; sortMenu = false },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (showHidden) "Ukryj ukryte" else "Pokaż ukryte (${ws.hidden.size})") },
                            leadingIcon = { Icon(if (showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) },
                            onClick = { showHidden = !showHidden; sortMenu = false },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Motyw szuflady…") },
                            leadingIcon = { Icon(Icons.Rounded.Dashboard, null) },
                            onClick = { sortMenu = false; styleMenu = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Kategorie") },
                            leadingIcon = { Icon(Icons.Rounded.Category, null) },
                            onClick = { sortMenu = false; onOpenCategories() },
                        )
                        DropdownMenuItem(
                            text = { Text("Ustawienia") },
                            leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                            onClick = { sortMenu = false; onOpenSettings() },
                        )
                    }
                    DropdownMenu(styleMenu, { styleMenu = false }) {
                        DrawerStyle.entries.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(s.label, fontWeight = if (cfg.drawerStyle == s) FontWeight.SemiBold else FontWeight.Normal)
                                        Text(s.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { if (cfg.drawerStyle == s) Icon(Icons.Rounded.Check, null) else Spacer(Modifier.size(24.dp)) },
                                onClick = { vm.updateConfig { it.copy(drawerStyle = s) }; styleMenu = false },
                            )
                        }
                    }
                }
            }

            // Filtr kategorii
            if (cfg.drawerCategoryChips && cfg.drawerStyle != DrawerStyle.Categories && ws.categories.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip("Wszystkie", filter == null) { filter = null }
                    ws.categories.forEach { c -> Chip((c.emoji + " " + c.name).trim(), filter == c.id) { filter = if (filter == c.id) null else c.id } }
                    Chip("Bez kategorii", filter == NO_CATEGORY) { filter = if (filter == NO_CATEGORY) null else NO_CATEGORY }
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // Wyszukiwanie z AI i w sieci
            AnimatedVisibility(query.isNotBlank()) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (vm.aiReady) {
                            ActionChip(if (aiBusy) "AI szuka…" else "Zapytaj AI", Icons.Rounded.AutoAwesome) { vm.aiFindApp(query) }
                            if (aiBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        }
                        ActionChip("Szukaj w sieci", Icons.Rounded.Public) { SystemActions.webSearch(ctx, query) }
                    }
                    val suggested = aiResults.mapNotNull { k -> apps.firstOrNull { it.key == k } }
                    if (suggested.isNotEmpty()) {
                        Text("AI poleca", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            suggested.forEach { app -> DrawerCell(app, cb, 48.dp, Modifier.width(76.dp).padding(vertical = 6.dp)) }
                        }
                    }
                }
            }
        }

        val padding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp)
        Box(Modifier.weight(1f).fillMaxWidth().nestedScroll(closeConnection).navigationBarsPadding()) {
            if (!loaded) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            } else if (visible.isEmpty()) {
                EmptyResult(query, ctx)
            } else when (cfg.drawerStyle) {
                DrawerStyle.Grid -> DrawerGrid(sections, cfg.drawerColumns, cb, padding)
                DrawerStyle.Perspective -> DrawerPerspective(visible, cfg.drawerColumns, cb, padding)
                DrawerStyle.List -> DrawerList(sections, cb, padding)
                DrawerStyle.Pages -> DrawerPages(visible, cfg.drawerColumns, cb, cfg.drawerRows)
                DrawerStyle.Alphabet -> DrawerAlphabet(visible, cb, padding)
                DrawerStyle.Categories -> {
                    val tabs = remember(visible, ws.categories) {
                        listOf("Wszystkie" to visible) +
                            ws.categories.mapNotNull { c ->
                                val l = visible.filter { ws.assignments[it.key]?.contains(c.id) == true }
                                if (l.isEmpty()) null else (c.emoji + " " + c.name).trim() to l
                            } +
                            visible.filter { ws.assignments[it.key].isNullOrEmpty() }.let { if (it.isEmpty()) emptyList() else listOf("Inne" to it) }
                    }
                    DrawerCategoryTabs(tabs, cfg.drawerColumns, cb, padding)
                }
                DrawerStyle.Tiles -> DrawerTiles(visible, ws.usage, cb, padding)
                DrawerStyle.Compact -> DrawerCompact(visible, cfg.drawerColumns.coerceAtLeast(6), cb, padding)
                DrawerStyle.SearchBar -> DrawerSearchBar(visible, ws.usage, cfg.drawerColumns, cb, padding)
                DrawerStyle.Material -> DrawerMaterial(visible, cb, padding)
                DrawerStyle.iOS -> DraweriOS(visible, cfg.drawerColumns, cb, padding)
                DrawerStyle.Horizontal -> DrawerHorizontal(visible, cfg.drawerRows, cb)
                DrawerStyle.Flow -> DrawerFlow(visible, ws.usage, cb, padding)
                DrawerStyle.Carousel -> DrawerCarousel(visible, cb)
                DrawerStyle.Helix -> DrawerHelix(visible, cb)
                DrawerStyle.Sphere -> DrawerSphere(visible, cb)
                DrawerStyle.Wheel -> DrawerWheel(visible, cb)
                DrawerStyle.Bubbles -> DrawerBubbles(visible, cb)
                DrawerStyle.Cylinder -> DrawerCylinder(visible, cb)
                DrawerStyle.Wave -> DrawerWave(visible, cb, padding)
                DrawerStyle.Stack -> DrawerStack(visible, cb, padding)
            }
        }
    }
}

@Composable
private fun EmptyResult(query: String, ctx: Context) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Brak aplikacji", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (query.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            ActionChip("Szukaj „$query” w sieci", Icons.Rounded.Public) { SystemActions.webSearch(ctx, query) }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = if (selected) 0f else 0.15f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}
