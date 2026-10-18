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
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.opendesktop.launcher.ui.components.AppIcon
import com.opendesktop.launcher.ui.components.FolderIcon
import com.opendesktop.launcher.ui.components.iconSizeFor
import com.opendesktop.launcher.ui.components.itemGestures
import com.opendesktop.launcher.widget.LauncherWidgetHost
import kotlin.math.roundToInt

interface DesktopCallbacks {
    fun onItemTap(pageId: String, item: DesktopItem, bounds: Rect)
    fun onItemLongPress(pageId: String, item: DesktopItem)
    fun onDragStart(pageId: String, item: DesktopItem, bounds: Rect)
    fun onDragEnd()
    fun onResize(pageId: String, item: DesktopItem, w: Int, h: Int)
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

        if (editMode && cfg.editGridLines) {
            val line = Color.White.copy(alpha = 0.16f)
            Canvas(Modifier.fillMaxSize()) {
                val inset = 4.dp.toPx()
                for (x in 0 until cols) for (y in 0 until rows) {
                    drawRoundRect(
                        line,
                        topLeft = Offset(x * cellPx.width + inset, y * cellPx.height + inset),
                        size = Size(cellPx.width - inset * 2, cellPx.height - inset * 2),
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(1.dp.toPx()),
                    )
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

        for (item in page.items) {
            key(item.id) {
                DesktopItemSlot(
                    item = item,
                    pageId = page.id,
                    cfg = cfg,
                    apps = apps,
                    labelOf = labelOf,
                    cw = cw,
                    ch = ch,
                    cellPx = cellPx,
                    editMode = editMode,
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
    }
}

@Composable
private fun DesktopItemSlot(
    item: DesktopItem,
    pageId: String,
    cfg: Config,
    apps: Map<String, AppInfo>,
    labelOf: (AppInfo) -> String,
    cw: Dp,
    ch: Dp,
    cellPx: Size,
    editMode: Boolean,
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
    val wiggle by animateFloatAsState(if (editMode) 0.94f else 1f, label = "edit")

    // Elementy, których aplikacja zniknęła (odinstalowana), pomijamy
    if (item.type == ItemType.App && apps[item.app] == null && apps.isNotEmpty()) return

    Box(
        Modifier
            .offset(cw * item.x, ch * item.y)
            .size(cw * resize.width, ch * resize.height)
            .zIndex(if (selected || showMenu) 2f else if (item.type == ItemType.Widget) 0f else 1f)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .graphicsLayer {
                alpha = ghost
                scaleX = wiggle; scaleY = wiggle
            }
            .then(
                if (selected && editMode) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                else Modifier
            ),
    ) {
        val gestures = Modifier.itemGestures(
            key = item.id + editMode,
            onPress = { pressed = it },
            onTap = { callbacks.onItemTap(pageId, item, bounds) },
            onLongPress = { callbacks.onItemLongPress(pageId, item) },
            onDragStart = { callbacks.onDragStart(pageId, item, bounds) },
            onDragEnd = { callbacks.onDragEnd() },
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
                FolderIcon(
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
            ItemType.Search -> SearchWidget(Modifier.fillMaxSize().then(gestures), aiEnabled)
        }
        if (editMode && selected) {
            ResizeHandle(
                drag = drag,
                onChange = { dx, dy ->
                    val nw = (item.w + (dx / cellPx.width).roundToInt()).coerceIn(1, cfg.columns - item.x)
                    val nh = (item.h + (dy / cellPx.height).roundToInt()).coerceIn(1, cfg.rows - item.y)
                    resize = IntSize(nw, nh)
                },
                onEnd = { callbacks.onResize(pageId, item, resize.width, resize.height) },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        if (showMenu) Box(Modifier.align(Alignment.Center)) { menu(item) }
    }
}

/** Uchwyt w rogu — mierzy przesunięcie palca w układzie okna, więc nie „skacze” przy przyciąganiu do siatki. */
@Composable
private fun ResizeHandle(
    drag: DragController,
    onChange: (Float, Float) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier,
) {
    val change by rememberUpdatedState(onChange)
    val end by rememberUpdatedState(onEnd)
    Box(
        modifier
            .offset(6.dp, 6.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val start = drag.pointer
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        c.consume()
                        val d = drag.pointer - start
                        change(d.x, d.y)
                    }
                    end()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.OpenInFull, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 90f })
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
            val visible = keys.mapNotNull { apps[it] }
            for (i in 0 until slots) {
                val app = visible.getOrNull(i)
                Box(Modifier.weight(1f).height(if (cfg.dockLabels) 78.dp else 60.dp), contentAlignment = Alignment.Center) {
                    if (app != null) key(app.key) {
                        var bounds by remember { mutableStateOf(Rect.Zero) }
                        var pressed by remember { mutableStateOf(false) }
                        AppIcon(
                            icon = app.icon,
                            label = labelOf(app),
                            iconSize = (52.dp * cfg.iconScale).coerceIn(36.dp, 64.dp),
                            shape = cfg.iconShape,
                            showLabel = cfg.dockLabels,
                            labelSize = cfg.labelSize,
                            pressed = pressed,
                            modifier = Modifier
                                .fillMaxSize()
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
                        if (menuKey == app.key) Box(Modifier.align(Alignment.Center)) { menu(app) }
                    } else if (editMode && i == visible.size) {
                        Box(
                            Modifier
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
