package com.opendesktop.launcher.ui.desktop

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import com.opendesktop.launcher.ui.theme.Palette
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import com.opendesktop.launcher.data.GadgetCategory
import com.opendesktop.launcher.data.GadgetKind
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.Config
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.ui.components.ActionMenu
import com.opendesktop.launcher.ui.components.AppIcon
import com.opendesktop.launcher.ui.components.MenuAction
import com.opendesktop.launcher.ui.components.PanelStyle
import com.opendesktop.launcher.ui.components.badgeCount
import com.opendesktop.launcher.ui.components.itemGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Folder jako małe okienko przy swojej ikonie: lekkie „szkło”, małe zaokrąglenie, siatka ikon,
 * a na dole subtelna (edytowalna) nazwa i mały plus. [anchor] = Rect.Zero → na środku ekranu.
 */
@Composable
fun FolderDialog(
    folder: DesktopItem,
    anchor: Rect,
    apps: Map<String, AppInfo>,
    labelOf: (AppInfo) -> String,
    cfg: Config,
    iconSize: Dp,
    cellW: Dp,
    cellH: Dp,
    onDismiss: () -> Unit,
    onLaunch: (AppInfo, Rect) -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (String, Boolean) -> Unit,
    onAddApps: () -> Unit,
    onReorder: (List<String>) -> Unit,
    appMenu: @Composable (app: AppInfo, extra: List<MenuAction>, onDismiss: () -> Unit) -> Unit,
    onRowChange: (Boolean) -> Unit = {},
) {
    var name by remember(folder.id) { mutableStateOf(folder.label) }
    var menuKey by remember { mutableStateOf<String?>(null) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 600f)) }
    val list = folder.apps.mapNotNull { apps[it] }
    // Układ zapamiętany dla folderu; lokalny stan, żeby przełączenie było natychmiastowe
    var row by remember(folder.id) { mutableStateOf(folder.folderRow) }
    val cols = when {
        list.size <= 4 -> 2
        list.size <= 9 -> 3
        else -> 4
    }
    val rows = ((list.size + cols - 1) / cols).coerceAtLeast(1)
    // Tło okienka z ustawień; tekst ciemny na jasnym tle, biały na ciemnym
    val panel = PanelStyle.of(cfg.folderColor, cfg.folderOpacity, cfg.folderRadius)
    val ink = panel.content
    fun close() { onRename(name); onDismiss() }
    // Nowa kolejność kluczy po zamianie dwóch widocznych aplikacji
    fun move(from: Int, to: Int): List<String> =
        list.map { it.key }.toMutableList().apply { add(to, removeAt(from)) }

    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { close() }) {
            val density = LocalDensity.current
            // Komórki i odstępy takie same jak siatka pulpitu
            val cell = cellH
            val footer = 34.dp
            // Wiersz: okienko na całą szerokość ekranu z tymi samymi marginesami (6 dp) i komórkami
            // co pulpit – ikony stoją dokładnie w kolumnach pulpitu; nadmiar się przewija, a pasek
            // przewijania leży na samym dole, wtopiony w krawędź. Siatka jak dotąd.
            // Rozmiar okienka przechodzi płynnie między układami
            val rowBar = if (cellW * list.size > maxWidth - 12.dp) 3.dp else 0.dp
            val targetW = if (row) maxWidth else minOf(cellW * cols + 12.dp, maxWidth - 24.dp)
            val targetH = if (row) cell + footer + 6.dp + rowBar
            else minOf(cell * rows + 6.dp + footer, maxHeight * 0.7f)
            val rowState = rememberLazyListState()
            val sizeSpec = spring<Dp>(dampingRatio = 0.82f, stiffness = 520f)
            val panelW by animateDpAsState(targetW, sizeSpec, label = "folderW")
            val panelH by animateDpAsState(targetH, sizeSpec, label = "folderH")
            val wPx = with(density) { panelW.toPx() }
            val hPx = with(density) { panelH.toPx() }
            val screenW = with(density) { maxWidth.toPx() }
            val screenH = with(density) { maxHeight.toPx() }
            val margin = with(density) { 12.dp.toPx() }
            val side = if (row) 0f else margin
            val cx = if (anchor == Rect.Zero) screenW / 2 else anchor.center.x
            val cy = if (anchor == Rect.Zero) screenH / 2 else anchor.center.y
            val x = (cx - wPx / 2).coerceIn(side, (screenW - wPx - side).coerceAtLeast(side))
            val y = (cy - hPx / 2).coerceIn(margin * 3, (screenH - hPx - margin * 3).coerceAtLeast(margin * 3))
            val shape = panel.shape
            Column(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(panelW, panelH)
                    .graphicsLayer {
                        // Wyrasta z ikony folderu
                        transformOrigin = TransformOrigin(((cx - x) / wPx).coerceIn(0f, 1f), ((cy - y) / hPx).coerceIn(0f, 1f))
                        val s = 0.6f + 0.4f * appear.value
                        scaleX = s; scaleY = s
                        alpha = appear.value.coerceIn(0f, 1f)
                    }
                    .clip(shape)
                    .background(panel.background)
                    .border(1.dp, panel.border, shape)
                    .clickable(interactionSource = null, indication = null) {}
                    .padding(top = 6.dp),
            ) {
                // Pojedyncza komórka – wspólna dla siatki i wiersza
                val appCell: @Composable (AppInfo, Modifier) -> Unit = { app, cellModifier ->
                    var bounds by remember { mutableStateOf(Rect.Zero) }
                    var pressed by remember { mutableStateOf(false) }
                    Box(cellModifier.height(cell), contentAlignment = Alignment.Center) {
                        AppIcon(
                            app.icon, labelOf(app), iconSize, cfg.iconShape, cfg.showLabels,
                            pressed = pressed, labelSize = cfg.labelSize, labelColor = ink, shadow = !panel.light,
                            badgeCount = badgeCount(app),
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { bounds = it.boundsInRoot() }
                                .itemGestures(
                                    key = app.key,
                                    onPress = { pressed = it },
                                    onTap = { onLaunch(app, bounds) },
                                    onLongPress = { menuKey = app.key },
                                ),
                        )
                        val idx = list.indexOf(app)
                        if (menuKey == app.key) {
                            Box(Modifier.align(Alignment.Center)) {
                                // To samo menu co dla aplikacji na pulpicie, plus akcje folderu
                                appMenu(
                                    app,
                                    listOfNotNull(
                                        if (idx > 0) MenuAction("Przesuń wcześniej", Icons.AutoMirrored.Rounded.ArrowBack) { onReorder(move(idx, idx - 1)) } else null,
                                        if (idx < list.lastIndex) MenuAction("Przesuń dalej", Icons.AutoMirrored.Rounded.ArrowForward) { onReorder(move(idx, idx + 1)) } else null,
                                        MenuAction("Wyjmij na pulpit", Icons.Rounded.Logout) { onRemoveApp(app.key, true) },
                                        MenuAction("Usuń z folderu", Icons.Rounded.Close) { onRemoveApp(app.key, false) },
                                    ),
                                ) { menuKey = null }
                            }
                        }
                    }
                }
                Crossfade(row, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp), animationSpec = tween(220), label = "folderLayout") { asRow ->
                    if (asRow) FolderRow(list, cellW, rowState, appCell)
                    else LazyVerticalGrid(GridCells.Fixed(cols), modifier = Modifier.fillMaxSize()) {
                        items(list, key = { it.key }) { app -> appCell(app, Modifier) }
                    }
                }
                // Stopka: subtelna nazwa, przełącznik układu i mały plus
                Row(Modifier.fillMaxWidth().height(footer).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(30) },
                        singleLine = true,
                        textStyle = TextStyle(color = ink.copy(alpha = 0.55f), fontSize = 13.sp),
                        cursorBrush = SolidColor(ink),
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text("Nazwa folderu", color = ink.copy(alpha = 0.35f), fontSize = 13.sp)
                            inner()
                        },
                    )
                    // Malutki przełącznik: jeden przewijany wiersz ↔ siatka, zapamiętany dla folderu
                    val toggleBg by animateColorAsState(if (row) ink.copy(alpha = 0.16f) else Color.Transparent, label = "rowToggle")
                    Box(
                        Modifier
                            .padding(end = 2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(toggleBg)
                            .clickable {
                                row = !row
                                onRowChange(row)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Crossfade(row, label = "rowIcon") { r ->
                            Icon(
                                if (r) Icons.Rounded.GridView else Icons.Rounded.ViewColumn,
                                if (r) "Pokaż jako siatkę" else "Pokaż w jednym wierszu",
                                tint = ink.copy(alpha = if (r) 0.8f else 0.5f),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.Add, "Dodaj aplikacje", tint = ink.copy(alpha = 0.55f),
                        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onAddApps).padding(5.dp),
                    )
                }
                // Pasek przewijania wiersza: pod nazwą, na całą szerokość, wtopiony w dolną krawędź
                if (row && rowBar > 0.dp) FolderScrollBar(rowState, list.size, cellW, ink, Modifier.fillMaxWidth().height(rowBar))
            }
        }
    }
}

/**
 * Folder w jednym wierszu: komórki tej samej szerokości co na pulpicie, przewijanie w poziomie
 * z dociąganiem do pełnych ikon; krawędzie zanikają tam, gdzie jest jeszcze coś do przewinięcia.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    list: List<AppInfo>,
    cellW: Dp,
    state: LazyListState,
    appCell: @Composable (AppInfo, Modifier) -> Unit,
) {
    val fadeL by animateFloatAsState(if (state.canScrollBackward) 1f else 0f, label = "fadeL")
    val fadeR by animateFloatAsState(if (state.canScrollForward) 1f else 0f, label = "fadeR")
    LazyRow(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // Maska krawędzi: przezroczystość tylko po stronie, w którą można przewinąć
                val f = (28.dp.toPx() / size.width).coerceAtMost(0.3f)
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 1f - fadeL),
                        f to Color.Black,
                        1f - f to Color.Black,
                        1f to Color.Black.copy(alpha = 1f - fadeR),
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        items(list, key = { it.key }) { app -> appCell(app, Modifier.width(cellW)) }
    }
}

/**
 * Pasek przewijania wiersza folderu na samym dole okienka: delikatny tor na całą szerokość
 * i suwak proporcjonalny do widocznej części, przycinany razem z rogami okienka.
 */
@Composable
private fun FolderScrollBar(state: LazyListState, count: Int, cellW: Dp, ink: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.drawBehind {
            drawRect(ink.copy(alpha = 0.08f))
            val itemPx = cellW.toPx()
            val total = (itemPx * count).coerceAtLeast(1f)
            val view = state.layoutInfo.viewportSize.width.toFloat().coerceAtLeast(1f)
            if (total <= view) return@drawBehind
            val scrolled = state.firstVisibleItemIndex * itemPx + state.firstVisibleItemScrollOffset
            val thumbW = (size.width * view / total).coerceAtLeast(size.height * 8)
            val thumbX = (size.width - thumbW) * (scrolled / (total - view)).coerceIn(0f, 1f)
            drawRoundRect(ink.copy(alpha = 0.5f), Offset(thumbX, 0f), Size(thumbW, size.height), CornerRadius(size.height / 2))
        },
    )
}

private class WidgetEntry(
    val info: AppWidgetProviderInfo?,
    val builtin: String?,
    val label: String,
    val appLabel: String,
    val span: Pair<Int, Int>,
)

@Composable
fun WidgetPicker(
    providers: List<AppWidgetProviderInfo>,
    spanOf: (AppWidgetProviderInfo) -> Pair<Int, Int>,
    onDismiss: () -> Unit,
    onPickBuiltin: (String) -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    var query by remember { mutableStateOf("") }
    val groups by produceState<List<Pair<String, List<WidgetEntry>>>>(emptyList(), providers) {
        value = withContext(Dispatchers.Default) {
            providers.map { info ->
                val appLabel = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, PackageManager.GET_META_DATA)).toString()
                }.getOrDefault(info.provider.packageName)
                WidgetEntry(info, null, runCatching { info.loadLabel(pm) }.getOrNull() ?: appLabel, appLabel, spanOf(info))
            }.groupBy { it.appLabel }.toList().sortedBy { it.first.lowercase() }
        }
    }
    // Wbudowane widżety w grupach – przy kilkudziesięciu jeden wiersz byłby nie do przejrzenia
    val builtins: List<Pair<String, List<WidgetEntry>>> = listOf(
        "Podstawowe" to listOf(
            WidgetEntry(null, "clock", "Zegar z datą", "OpenDesktop", 4 to 2),
            WidgetEntry(null, "search", "Pasek wyszukiwania", "OpenDesktop", 5 to 1),
        ),
    ) + GadgetCategory.entries.map { c ->
        c.label to GadgetKind.entries.filter { it.category == c }.map { WidgetEntry(null, "gadget:${it.name}", it.label, it.description, it.w to it.h) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Palette.Panel, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", tint = Color.White) }
                    Text("Widżety", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                OutlinedTextField(
                    query, { query = it }, singleLine = true, placeholder = { Text("Szukaj widżetów") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(50),
                )
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    val q = query.trim()
                    builtins.forEach { (title, list) ->
                        val shown = if (q.isEmpty() || title.contains(q, true) || "opendesktop".contains(q, true)) list
                        else list.filter { it.label.contains(q, true) || it.appLabel.contains(q, true) }
                        if (shown.isNotEmpty()) item(key = "builtin:$title") {
                            WidgetGroup("OpenDesktop · $title", shown) { e -> onPickBuiltin(e.builtin!!) }
                        }
                    }
                    val filtered = groups.mapNotNull { (app, list) ->
                        val l = if (q.isEmpty() || app.contains(q, true)) list else list.filter { it.label.contains(q, true) }
                        if (l.isEmpty()) null else app to l
                    }
                    items(filtered, key = { it.first }) { (app, list) ->
                        WidgetGroup(app, list) { e -> e.info?.let(onPick) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetGroup(title: String, list: List<WidgetEntry>, onPick: (WidgetEntry) -> Unit) {
    Column {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { e -> WidgetCard(e) { onPick(e) } }
        }
    }
}

@Composable
private fun WidgetCard(e: WidgetEntry, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val preview by produceState<ImageBitmap?>(null, e.info) {
        value = withContext(Dispatchers.IO) {
            val info = e.info ?: return@withContext null
            val density = ctx.resources.displayMetrics.densityDpi
            val d = runCatching { info.loadPreviewImage(ctx, density) }.getOrNull()
                ?: runCatching { info.loadIcon(ctx, density) }.getOrNull()
            d?.let { dr ->
                val w = dr.intrinsicWidth.takeIf { it > 0 } ?: 256
                val h = dr.intrinsicHeight.takeIf { it > 0 } ?: 256
                val scale = minOf(1f, 480f / maxOf(w, h))
                runCatching { dr.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap() }.getOrNull()
            }
        }
    }
    Column(
        Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            val img = preview
            when {
                img != null -> Image(img, null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                e.builtin == "clock" -> Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(48.dp))
                e.builtin == "search" -> Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(48.dp))
                e.builtin?.startsWith("gadget:") == true -> {
                    // Żywy podgląd wbudowanego widżetu w proporcjach jego domyślnego rozmiaru
                    val kind = GadgetKind.valueOf(e.builtin!!.removePrefix("gadget:"))
                    val ratio = (kind.w.toFloat() / kind.h).coerceIn(0.8f, 4f)
                    Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                        GadgetWidget(kind, kind.defaultStyle().copy(textScale = 0.9f, padding = 8), Modifier.fillMaxHeight().aspectRatio(ratio, matchHeightConstraintsFirst = true))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(e.label, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontSize = 13.sp)
        // Wbudowane widżety: krótki opis zamiast nazwy aplikacji
        if (e.builtin?.startsWith("gadget:") == true) {
            Text(e.appLabel, color = Color.White.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 13.sp)
        }
        Text("${e.span.first} × ${e.span.second}", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}
