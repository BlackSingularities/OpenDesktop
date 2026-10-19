package com.opendesktop.launcher.ui.desktop

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import com.opendesktop.launcher.ui.components.iconShapeOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.Config
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.ItemType
import com.opendesktop.launcher.data.Page
import com.opendesktop.launcher.data.SystemAction
import com.opendesktop.launcher.ui.components.ActionIcon
import com.opendesktop.launcher.ui.components.AppIcon
import com.opendesktop.launcher.ui.components.FolderIcon
import com.opendesktop.launcher.ui.components.iconSizeFor
import com.opendesktop.launcher.ui.components.itemGestures
import com.opendesktop.launcher.widget.LauncherWidgetHost
import kotlin.math.roundToInt

interface DesktopCallbacks {
    fun onItemTap(pageId: String, item: DesktopItem, bounds: Rect)
    fun onItemLongPress(pageId: String, item: DesktopItem)
    fun onOpenFolder(pageId: String, item: DesktopItem, bounds: Rect)
    fun onDragStart(pageId: String, item: DesktopItem, bounds: Rect)
    fun onDragEnd()
    fun onResize(pageId: String, item: DesktopItem, x: Int, y: Int, w: Int, h: Int)
    fun onEmptyLongPress()
    fun onEmptyDoubleTap()
    fun onEmptyTap()
}

@Composable
fun DesktopGrid(
    page: Page,
    cfg: Config,
    apps: Map<String, AppInfo>,
    labelOf: (AppInfo) -> String,
    editMode: Boolean,
    selectedId: String?,
    menuItemId: String?,
    drag: DragController,
    dropHighlight: DropTarget.Cell?,
    host: LauncherWidgetHost,
    aiEnabled: Boolean,
    callbacks: DesktopCallbacks,
    menu: @Composable (DesktopItem) -> Unit,
    onCellSize: (Size) -> Unit,
    modifier: Modifier = Modifier,
    /** Postęp wejścia w edycję 0–1 (siatka pojawia się płynnie). */
    editProgress: Float = if (editMode) 1f else 0f,
) {
    val cols = cfg.columns
    val rows = cfg.rows
    val items by rememberUpdatedState(page.items)
    val cb by rememberUpdatedState(callbacks)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { drag.gridBounds[page.id] = it.boundsInRoot() }
            .pointerInput(cols, rows) {
                fun hit(pos: Offset): DesktopItem? {
                    val cx = (pos.x / (size.width / cols.toFloat())).toInt()
                    val cy = (pos.y / (size.height / rows.toFloat())).toInt()
                    return items.lastOrNull { cx >= it.x && cx < it.x + it.w && cy >= it.y && cy < it.y + it.h }
                }
                detectTapGestures(
                    onTap = { if (hit(it) == null) cb.onEmptyTap() },
                    onDoubleTap = { if (hit(it) == null) cb.onEmptyDoubleTap() },
                    onLongPress = { if (hit(it) == null) cb.onEmptyLongPress() },
                )
            },
    ) {
        val cw = maxWidth / cols
        val ch = maxHeight / rows
        val density = LocalDensity.current
        val cellPx = with(density) { Size(cw.toPx(), ch.toPx()) }
        onCellSize(cellPx)

        if (editProgress > 0.001f && cfg.editGridLines) {
            // Wolne komórki: delikatne wypełnienie i obrys; zajęte zostają czyste, żeby było widać,
            // gdzie jest miejsce. Kropki na przecięciach ułatwiają celowanie przy przeciąganiu.
            val occupied = remember(page.items, cols, rows) {
                BooleanArray(cols * rows).also { occ ->
                    page.items.forEach { o ->
                        for (x in o.x.coerceAtLeast(0) until (o.x + o.w).coerceAtMost(cols)) {
                            for (y in o.y.coerceAtLeast(0) until (o.y + o.h).coerceAtMost(rows)) occ[y * cols + x] = true
                        }
                    }
                }
            }
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = editProgress }) {
                val inset = 4.dp.toPx()
                val radius = CornerRadius(14.dp.toPx())
                for (x in 0 until cols) for (y in 0 until rows) {
                    if (occupied[y * cols + x]) continue
                    val tl = Offset(x * cellPx.width + inset, y * cellPx.height + inset)
                    val sz = Size(cellPx.width - inset * 2, cellPx.height - inset * 2)
                    drawRoundRect(Color.White.copy(alpha = 0.05f), topLeft = tl, size = sz, cornerRadius = radius)
                    drawRoundRect(Color.White.copy(alpha = 0.14f), topLeft = tl, size = sz, cornerRadius = radius, style = Stroke(1.dp.toPx()))
                }
                val dot = Color.White.copy(alpha = 0.35f)
                for (x in 1 until cols) for (y in 1 until rows) {
                    drawCircle(dot, radius = 1.5.dp.toPx(), center = Offset(x * cellPx.width, y * cellPx.height))
                }
            }
        }

        if (dropHighlight != null && dropHighlight.pageId == page.id) {
            val color = when {
                dropHighlight.mergeWith != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                dropHighlight.valid -> Color.White.copy(alpha = 0.18f)
                else -> Color(0x55FF5252)
            }
            val x by animateDpAsState(cw * dropHighlight.x, spring(stiffness = 900f), label = "dx")
            val y by animateDpAsState(ch * dropHighlight.y, spring(stiffness = 900f), label = "dy")
            Box(
                Modifier
                    .offset(x, y)
                    .size(cw * dropHighlight.w, ch * dropHighlight.h)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            )
        }

        // Kolejność na liście = kolejność warstw (ostatni na wierzchu); tak samo liczy trafienia dotyku
        page.items.forEachIndexed { layer, item ->
            key(item.id) {
                DesktopItemSlot(
                    item = item,
                    layer = layer,
                    pageId = page.id,
                    cfg = cfg,
                    apps = apps,
                    labelOf = labelOf,
                    cw = cw,
                    ch = ch,
                    cellPx = cellPx,
                    editMode = editMode,
                    editProgress = editProgress,
                    selected = selectedId == item.id,
                    showMenu = menuItemId == item.id,
                    drag = drag,
                    mergeTarget = dropHighlight?.mergeWith == item.id,
                    host = host,
                    aiEnabled = aiEnabled,
                    callbacks = callbacks,
                    menu = menu,
                )
            }
        }
        // Ramka zmiany rozmiaru zaznaczonego elementu – nad wszystkim
        val sel = if (editMode) page.items.firstOrNull { it.id == selectedId } else null
        if (sel != null) key("resize_${sel.id}") {
            ResizeFrame(sel, page.id, cw, ch, cols, rows, drag) { x, y, w, h -> callbacks.onResize(page.id, sel, x, y, w, h) }
        }
    }
}

@Composable
private fun DesktopItemSlot(
    item: DesktopItem,
    layer: Int,
    pageId: String,
    cfg: Config,
    apps: Map<String, AppInfo>,
    labelOf: (AppInfo) -> String,
    cw: Dp,
    ch: Dp,
    cellPx: Size,
    editMode: Boolean,
    editProgress: Float,
    selected: Boolean,
    showMenu: Boolean,
    drag: DragController,
    mergeTarget: Boolean,
    host: LauncherWidgetHost,
    aiEnabled: Boolean,
    callbacks: DesktopCallbacks,
    menu: @Composable (DesktopItem) -> Unit,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var pressed by remember { mutableStateOf(false) }
    var resize by remember(item.w, item.h) { mutableStateOf(IntSize(item.w, item.h)) }
    val isDragged = drag.payload?.item?.id == item.id
    val ghost by animateFloatAsState(if (isDragged) 0.25f else 1f, label = "ghost")
    // Elementy w edycji lekko mniejsze – między nimi widać siatkę
    val wiggle = 1f - 0.05f * editProgress

    // Elementy, których aplikacja zniknęła (odinstalowana), pomijamy
    if (item.type == ItemType.App && apps[item.app] == null && apps.isNotEmpty()) return

    Box(
        Modifier
            .offset(cw * item.x, ch * item.y)
            .size(cw * resize.width, ch * resize.height)
            // Widżety zawsze pod ikonami (inaczej widżet systemowy przechwytuje dotyk ikon nad nim);
            // „Na wierzch/Na spód” ustala kolejność osobno wśród widżetów i wśród pozostałych elementów
            .zIndex(
                when {
                    // Zaznaczony widżet idzie na wierzch tylko wśród widżetów – inaczej jego nakładka
                    // w trybie edycji przykrywa i blokuje ikony leżące na nim
                    item.type == ItemType.Widget -> layer.toFloat() + if (selected || showMenu) 5_000f else 0f
                    selected || showMenu -> 100_000f
                    else -> 10_000f + layer
                }
            )
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .graphicsLayer {
                alpha = ghost
                scaleX = wiggle; scaleY = wiggle
            }
    ) {
        val gestures = Modifier.itemGestures(
            // Klucz bez trybu edycji: wejście w edycję w trakcie przytrzymania nie przerywa gestu,
            // więc po przytrzymaniu można od razu przeciągać
            key = item.id,
            holdMillis = if (editMode) 250L else null,
            onPress = { pressed = it },
            onTap = { callbacks.onItemTap(pageId, item, bounds) },
            onLongPress = { callbacks.onItemLongPress(pageId, item) },
            onDragStart = { callbacks.onDragStart(pageId, item, bounds) },
            onDragEnd = { callbacks.onDragEnd() },
            // Przesunięcie w górę na folderze otwiera folder (także gdy dotknięcie uruchamia główną aplikację)
            onSwipeUp = if (item.type == ItemType.Folder && !editMode) {
                { callbacks.onOpenFolder(pageId, item, bounds) }
            } else null,
        )
        val cellW = cw * resize.width
        val cellH = ch * resize.height
        when (item.type) {
            ItemType.App -> {
                val app = apps[item.app]
                AppIcon(
                    icon = app?.icon,
                    label = app?.let(labelOf) ?: "",
                    iconSize = iconSizeFor(cellW, cellH, cfg.desktopIconScale, cfg.showLabels, desktopMode = true),
                    shape = cfg.iconShape,
                    showLabel = cfg.showLabels,
                    labelSize = cfg.labelSize,
                    pressed = pressed || mergeTarget,
                    modifier = Modifier.fillMaxSize().then(gestures),
                )
            }
            ItemType.Folder -> {
                val primary = item.primaryApp?.let { apps[it] }
                if (primary != null) {
                    // Folder z główną aplikacją: jej ikona + znaczek folderu w prawym dolnym rogu
                    val size = iconSizeFor(cellW, cellH, cfg.desktopIconScale, cfg.showLabels, desktopMode = true)
                    AppIcon(
                        icon = primary.icon,
                        label = item.label.ifBlank { labelOf(primary) },
                        iconSize = size,
                        shape = cfg.iconShape,
                        showLabel = cfg.showLabels,
                        labelSize = cfg.labelSize,
                        pressed = pressed || mergeTarget,
                        modifier = Modifier.fillMaxSize().then(gestures),
                        badge = {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = size * 0.1f, y = size * 0.1f)
                                    .size(size * 0.42f)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C1C1C))
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Folder, "Folder", tint = Color.White, modifier = Modifier.size(size * 0.24f)) }
                        },
                    )
                } else FolderIcon(
                    icons = item.apps.take(4).map { apps[it]?.icon },
                    label = item.label,
                    iconSize = iconSizeFor(cellW, cellH, cfg.desktopIconScale, cfg.showLabels, desktopMode = true),
                    shape = cfg.iconShape,
                    showLabel = cfg.showLabels,
                    labelSize = cfg.labelSize,
                    pressed = pressed,
                    highlight = mergeTarget,
                    modifier = Modifier.fillMaxSize().then(gestures),
                )
            }
            ItemType.Widget -> {
                SystemWidget(host, item, onLongPress = { callbacks.onItemLongPress(pageId, item) }, modifier = Modifier.fillMaxSize())
                if (editMode) Box(Modifier.matchParentSize().then(gestures))
            }
            ItemType.Clock -> ClockWidget(Modifier.fillMaxSize().then(gestures))
            ItemType.Search -> SearchWidget(Modifier.fillMaxSize().then(gestures), aiEnabled, item.searchStyle)
            ItemType.Action -> ActionIcon(
                action = item.systemAction,
                label = item.label,
                iconSize = iconSizeFor(cellW, cellH, cfg.desktopIconScale, cfg.showLabels, desktopMode = true),
                showLabel = cfg.showLabels,
                labelSize = cfg.labelSize,
                pressed = pressed,
                modifier = Modifier.fillMaxSize().then(gestures),
            )
        }
        if (showMenu) Box(Modifier.align(Alignment.Center)) { menu(item) }
    }
}

/** Uchwyt ramki: które krawędzie przesuwa (-1 = lewa/górna, 1 = prawa/dolna, 0 = żadna). */
private enum class Handle(val h: Int, val v: Int, val align: Alignment) {
    Left(-1, 0, Alignment.CenterStart),
    Top(0, -1, Alignment.TopCenter),
    Right(1, 0, Alignment.CenterEnd),
    Bottom(0, 1, Alignment.BottomCenter),
    TopLeft(-1, -1, Alignment.TopStart),
    TopRight(1, -1, Alignment.TopEnd),
    BottomLeft(-1, 1, Alignment.BottomStart),
    BottomRight(1, 1, Alignment.BottomEnd),
}

/**
 * Ramka zmiany rozmiaru zaznaczonego elementu: okrągła kropka na środku każdej krawędzi i w każdym
 * rogu – przeciągnięcie kropki przesuwa tę krawędź (róg – dwie naraz). Rysowana nad wszystkim
 * w siatce, z podglądem nowego rozmiaru w komórkach. Rozmiar liczony od przesunięcia palca
 * w układzie okna – z rzeczywistej (przeskalowanej w edycji) szerokości komórki.
 */
@Composable
private fun ResizeFrame(
    item: DesktopItem,
    pageId: String,
    cw: Dp,
    ch: Dp,
    cols: Int,
    rows: Int,
    drag: DragController,
    onDone: (x: Int, y: Int, w: Int, h: Int) -> Unit,
) {
    // Prostokąt w komórkach: [left, right) × [top, bottom)
    var l by remember(item.x, item.y, item.w, item.h) { mutableIntStateOf(item.x) }
    var t by remember(item.x, item.y, item.w, item.h) { mutableIntStateOf(item.y) }
    var r by remember(item.x, item.y, item.w, item.h) { mutableIntStateOf(item.x + item.w) }
    var b by remember(item.x, item.y, item.w, item.h) { mutableIntStateOf(item.y + item.h) }
    var active by remember { mutableStateOf<Handle?>(null) }
    val done by rememberUpdatedState(onDone)
    val accent = MaterialTheme.colorScheme.primary
    // Płynne przejście ramki między komórkami
    val ox by animateDpAsState(cw * l, spring(stiffness = 1200f), label = "fx")
    val oy by animateDpAsState(ch * t, spring(stiffness = 1200f), label = "fy")
    val fw by animateDpAsState(cw * (r - l), spring(stiffness = 1200f), label = "fw")
    val fh by animateDpAsState(ch * (b - t), spring(stiffness = 1200f), label = "fh")
    Box(Modifier.offset(ox, oy).size(fw, fh).zIndex(200_000f)) {
        Box(
            Modifier
                .matchParentSize()
                .padding(3.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = if (active != null) 0.14f else 0.06f))
                .border(2.dp, Color.White, RoundedCornerShape(16.dp)),
        )
        // Rozmiar w komórkach podczas przeciągania
        if (active != null) {
            Text(
                "${r - l} × ${b - t}",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Handle.entries.forEach { handle ->
            Box(
                Modifier
                    .align(handle.align)
                    // Kropka na linii ramki; pole dotyku większe niż sama kropka
                    .offset(x = 14.dp * handle.h, y = 14.dp * handle.v)
                    .size(40.dp)
                    .pointerInput(handle, cols, rows) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            active = handle
                            val start = drag.pointer
                            val l0 = l; val t0 = t; val r0 = r; val b0 = b
                            val grid = drag.gridBounds[pageId]
                            val cellW = grid?.let { it.width / cols } ?: cw.toPx()
                            val cellH = grid?.let { it.height / rows } ?: ch.toPx()
                            try {
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!c.pressed) break
                                    c.consume()
                                    val d = drag.pointer - start
                                    val dx = (d.x / cellW).roundToInt()
                                    val dy = (d.y / cellH).roundToInt()
                                    when (handle.h) {
                                        -1 -> l = (l0 + dx).coerceIn(0, r0 - 1)
                                        1 -> r = (r0 + dx).coerceIn(l0 + 1, cols)
                                    }
                                    when (handle.v) {
                                        -1 -> t = (t0 + dy).coerceIn(0, b0 - 1)
                                        1 -> b = (b0 + dy).coerceIn(t0 + 1, rows)
                                    }
                                }
                            } finally {
                                active = null
                            }
                            if (l != l0 || t != t0 || r != r0 || b != b0) done(l, t, r - l, b - t)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val on = active == handle
                Box(
                    Modifier
                        .size(if (on) 20.dp else 15.dp)
                        .clip(CircleShape)
                        .background(if (on) accent else Color.White)
                        .border(1.5.dp, if (on) Color.White else Color.Black.copy(alpha = 0.35f), CircleShape),
                )
            }
        }
    }
}

@Composable
fun Dock(
    keys: List<String>,
    apps: Map<String, AppInfo>,
    labelOf: (AppInfo) -> String,
    cfg: Config,
    editMode: Boolean,
    drag: DragController,
    menuKey: String?,
    onTap: (AppInfo, Rect) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onDragStart: (AppInfo, Rect) -> Unit,
    onDragEnd: () -> Unit,
    onAdd: () -> Unit,
    menu: @Composable (AppInfo) -> Unit,
    onActionTap: (SystemAction) -> Unit = {},
    onActionLongPress: (SystemAction) -> Unit = {},
    actionMenu: @Composable (SystemAction) -> Unit = {},
) {
    val slots = cfg.dockCount
    val dragKey = drag.payload?.takeIf { it.source == DragSource.Dock }?.item?.app
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .onGloballyPositioned { drag.dockBounds = it.boundsInRoot() }
            .then(
                if (cfg.dockBackground) Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = cfg.dockOpacity))
                else Modifier
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Wpisy doku: aplikacje albo skróty akcji systemowych
            val visible = keys.filter { it in apps || SystemAction.fromDockKey(it) != null }
            for (i in 0 until slots) {
                val entry = visible.getOrNull(i)
                val app = entry?.let { apps[it] }
                val action = entry?.let { SystemAction.fromDockKey(it) }
                val iconSize = (52.dp * cfg.iconScale).coerceIn(36.dp, 64.dp)
                val areaH = if (cfg.dockLabels) 78.dp else 60.dp
                // Odbicia tylko bez podpisów – podpis stałby między ikoną a odbiciem
                val reflect = cfg.dockReflection && !cfg.dockLabels
                Box(Modifier.weight(1f).height(areaH + if (reflect) iconSize * 0.45f else 0.dp), contentAlignment = Alignment.TopCenter) {
                    if (action != null) key(entry) {
                        var pressed by remember { mutableStateOf(false) }
                        ActionIcon(
                            action = action,
                            label = action.label,
                            iconSize = iconSize,
                            showLabel = cfg.dockLabels,
                            labelSize = cfg.labelSize,
                            pressed = pressed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(areaH)
                                .itemGestures(
                                    key = entry,
                                    onPress = { pressed = it },
                                    onTap = { if (!editMode) onActionTap(action) },
                                    onLongPress = { onActionLongPress(action) },
                                    onDragStart = {},
                                    onDragEnd = {},
                                ),
                        )
                        if (menuKey == entry) Box(Modifier.align(Alignment.Center)) { actionMenu(action) }
                    } else if (app != null) key(app.key) {
                        var bounds by remember { mutableStateOf(Rect.Zero) }
                        var pressed by remember { mutableStateOf(false) }
                        AppIcon(
                            icon = app.icon,
                            label = labelOf(app),
                            iconSize = iconSize,
                            shape = cfg.iconShape,
                            showLabel = cfg.dockLabels,
                            labelSize = cfg.labelSize,
                            pressed = pressed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(areaH)
                                .graphicsLayer { alpha = if (dragKey == app.key) 0.25f else 1f }
                                .onGloballyPositioned { bounds = it.boundsInRoot() }
                                .itemGestures(
                                    key = app.key,
                                    onPress = { pressed = it },
                                    onTap = { onTap(app, bounds) },
                                    onLongPress = { onLongPress(app) },
                                    onDragStart = { onDragStart(app, bounds) },
                                    onDragEnd = onDragEnd,
                                ),
                        )
                        if (reflect && dragKey != app.key) {
                            // Odbicie: dolna część ikony odwrócona w pionie i zanikająca ku dołowi
                            val shape = iconShapeOf(cfg.iconShape)
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .offset(y = areaH / 2 + iconSize / 2 + 2.dp)
                                    .size(iconSize, iconSize * 0.45f)
                                    .graphicsLayer { scaleY = -1f; compositingStrategy = CompositingStrategy.Offscreen }
                                    .then(if (shape != null) Modifier.clip(shape) else Modifier)
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    },
                            )
                        }
                        if (menuKey == app.key) Box(Modifier.align(Alignment.Center)) { menu(app) }
                    } else if (editMode && i == visible.size) {
                        Box(
                            Modifier
                                .padding(top = (areaH - 46.dp) / 2)
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .pointerInput(Unit) { detectTapGestures { onAdd() } },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Add, "Dodaj do doku", tint = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
fun PageIndicator(count: Int, current: Int, fraction: Float, homeIndex: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val dist = kotlin.math.abs(i - (current + fraction)).coerceIn(0f, 1f)
            val w = 6.dp + 14.dp * (1f - dist)
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f + 0.6f * (1f - dist)))
                    .then(if (i == homeIndex) Modifier.border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape) else Modifier),
            )
        }
    }
}

/** Podgląd przeciąganego elementu rysowany nad wszystkim. */
@Composable
fun DragPreview(
    payload: DragPayload,
    drag: DragController,
    apps: Map<String, AppInfo>,
    cfg: Config,
) {
    val density = LocalDensity.current
    val wDp = with(density) { payload.size.width.toDp() }
    val hDp = with(density) { payload.size.height.toDp() }
    val lift by animateFloatAsState(1.1f, spring(dampingRatio = 0.5f), label = "lift")
    Box(
        Modifier
            .offset { androidx.compose.ui.unit.IntOffset((drag.pointer.x - drag.grab.x).roundToInt(), (drag.pointer.y - drag.grab.y).roundToInt()) }
            .size(wDp, hDp)
            .graphicsLayer { scaleX = lift; scaleY = lift; alpha = 0.95f; shadowElevation = 18f },
    ) {
        val item = payload.item
        when (item.type) {
            ItemType.App -> {
                val app = apps[item.app]
                AppIcon(app?.icon, "", iconSizeFor(wDp / item.w.coerceAtLeast(1), hDp / item.h.coerceAtLeast(1), cfg.desktopIconScale, cfg.showLabels, desktopMode = true), cfg.iconShape, false, Modifier.fillMaxSize())
            }
            ItemType.Folder -> FolderIcon(
                item.apps.take(4).map { apps[it]?.icon }, item.label,
                iconSizeFor(wDp / item.w, hDp / item.h, cfg.desktopIconScale, cfg.showLabels, desktopMode = true), cfg.iconShape, false, Modifier.fillMaxSize(),
            )
            ItemType.Action -> ActionIcon(
                item.systemAction, "",
                iconSizeFor(wDp / item.w.coerceAtLeast(1), hDp / item.h.coerceAtLeast(1), cfg.desktopIconScale, cfg.showLabels, desktopMode = true),
                showLabel = false, labelSize = cfg.labelSize, pressed = false, modifier = Modifier.fillMaxSize(),
            )
            else -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            )
        }
    }
}
