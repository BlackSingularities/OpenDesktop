package com.opendesktop.launcher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.Category
import com.opendesktop.launcher.data.IconShape
import com.opendesktop.launcher.ui.components.AppIcon
import com.opendesktop.launcher.ui.components.LabelShadow
import com.opendesktop.launcher.ui.components.itemGestures
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

data class DrawerSection(val key: String, val title: String?, val apps: List<AppInfo>)

/** Wspólne zachowanie elementów szuflady (tap, menu, przeciągnięcie na pulpit). */
class DrawerCallbacks(
    val label: (AppInfo) -> String,
    val categoriesOf: (AppInfo) -> List<Category>,
    val shape: IconShape,
    val labelSize: Float,
    val showLabels: Boolean,
    val menuKey: String?,
    val onTap: (AppInfo, Rect) -> Unit,
    val onLongPress: (AppInfo) -> Unit,
    val onDragStart: (AppInfo, Rect) -> Unit,
    val onDragEnd: () -> Unit,
    val menu: @Composable (AppInfo) -> Unit,
)

@Composable
fun DrawerTouchable(
    app: AppInfo,
    cb: DrawerCallbacks,
    modifier: Modifier = Modifier,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .itemGestures(
                key = app.key,
                onPress = { pressed = it },
                onTap = { cb.onTap(app, bounds) },
                onLongPress = { cb.onLongPress(app) },
                onDragStart = { cb.onDragStart(app, bounds) },
                onDragEnd = { cb.onDragEnd() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(pressed)
        if (cb.menuKey == app.key) Box(Modifier.align(Alignment.Center)) { cb.menu(app) }
    }
}

@Composable
fun DrawerCell(app: AppInfo, cb: DrawerCallbacks, iconSize: Dp, modifier: Modifier = Modifier) {
    DrawerTouchable(app, cb, modifier) { pressed ->
        AppIcon(app.icon, cb.label(app), iconSize, cb.shape, cb.showLabels, pressed = pressed, labelSize = cb.labelSize, maxLines = 1)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        style = MaterialTheme.typography.titleSmall.copy(shadow = LabelShadow),
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp),
    )
}

/**
 * Rozmiar komórki siatki z ustawień: [rows] = 0 → ciasne, zwykłe przewijanie;
 * inaczej na ekranie mieści się dokładnie [rows] wierszy. Ikona dopasowuje się do komórki.
 */
@Composable
fun GridSizing(columns: Int, rows: Int, cb: DrawerCallbacks, content: @Composable (cell: Modifier, icon: Dp) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val colW = maxWidth / columns
        val cellH = if (rows > 0) maxHeight / rows else null
        val base = cellH ?: (colW * if (cb.showLabels) 1.3f else 1f)
        val icon = minOf(64.dp, base * (if (cb.showLabels) 0.58f else 0.74f), colW * 0.74f)
        content(if (cellH != null) Modifier.height(cellH) else Modifier.padding(vertical = 4.dp), icon)
    }
}

@Composable
fun DrawerGrid(sections: List<DrawerSection>, columns: Int, rows: Int, cb: DrawerCallbacks, padding: PaddingValues) {
    GridSizing(columns, rows, cb) { cell, icon ->
        LazyVerticalGrid(GridCells.Fixed(columns), contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            sections.forEach { s ->
                if (s.title != null) item(key = "h_${s.key}", span = { GridItemSpan(maxLineSpan) }) { SectionHeader(s.title) }
                items(s.apps, key = { "${s.key}_${it.key}" }) { app -> DrawerCell(app, cb, icon, cell) }
            }
        }
    }
}

/**
 * Perspektywa 3D: zwykła siatka, ale rzędy przy górnej i dolnej krawędzi odchylają się w głąb,
 * maleją i ściskają – jakby lista była nawinięta na bęben. Liczone w warstwie graficznej.
 */
@Composable
fun DrawerPerspective(apps: List<AppInfo>, columns: Int, rowCount: Int, cb: DrawerCallbacks) = GridSizing(columns, rowCount, cb) { cell, iconSize ->
    val rows = remember(apps, columns) { apps.chunked(columns) }
    val state = rememberLazyListState()
    // Bez marginesów: lista od krawędzi do krawędzi
    LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(rows, key = { _, r -> r.first().key }) { _, row ->
            val rowKey = row.first().key
            Row(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Bez obrotu: rzędy przy krawędziach tylko maleją i spłaszczają się, dosuwając do siebie
                        val info = state.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.key == rowKey } ?: return@graphicsLayer
                        val h = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1).toFloat()
                        val top = (item.offset - info.viewportStartOffset).toFloat()
                        val center = (top + item.size / 2f) / h
                        val zone = 0.2f
                        val t = when {
                            center > 1f - zone -> (center - (1f - zone)) / zone
                            center < zone -> (zone - center) / zone
                            else -> 0f
                        }.coerceIn(0f, 1.5f)
                        if (t > 0f) {
                            val down = center > 0.5f
                            transformOrigin = TransformOrigin(0.5f, if (down) 0f else 1f)
                            scaleX = 1f - 0.3f * t.coerceAtMost(1.2f)
                            scaleY = (1f - 0.55f * t).coerceAtLeast(0.2f)
                            translationY = (if (down) -1f else 1f) * item.size * 0.5f * t * t
                            alpha = (1.2f - 0.6f * t).coerceIn(0f, 1f)
                        }
                    },
            ) {
                for (c in 0 until columns) {
                    val app = row.getOrNull(c)
                    Box(Modifier.weight(1f).then(cell), contentAlignment = Alignment.Center) {
                        if (app != null) key(app.key) { DrawerCell(app, cb, iconSize) }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerList(sections: List<DrawerSection>, cb: DrawerCallbacks, padding: PaddingValues) {
    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
        sections.forEach { s ->
            if (s.title != null) item(key = "h_${s.key}") { SectionHeader(s.title) }
            items(s.apps, key = { "${s.key}_${it.key}" }) { app -> ListRow(app, cb) }
        }
    }
}

@Composable
private fun ListRow(app: AppInfo, cb: DrawerCallbacks) {
    DrawerTouchable(app, cb, Modifier.fillMaxWidth()) { pressed ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = if (pressed) 0.14f else 0.05f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app.icon, "", 44.dp, cb.shape, false, pressed = pressed)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (cb.showLabels) Text(cb.label(app), color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val cats = cb.categoriesOf(app)
                if (cats.isNotEmpty()) {
                    Text(
                        cats.joinToString("  ") { (it.emoji + " " + it.name).trim() },
                        color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerPages(apps: List<AppInfo>, columns: Int, cb: DrawerCallbacks, fixedRows: Int = 0) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // fixedRows = 0 → tyle wierszy, ile się zmieści; inaczej wiersze ściskane do zadanej liczby
        val avail = maxHeight - 28.dp
        val rows = if (fixedRows > 0) fixedRows else (avail / 100.dp).toInt().coerceAtLeast(3)
        // Rzędy wypełniają całą wysokość; ikona rośnie do komórki (ograniczona szerokością kolumny)
        val rowH = avail / rows
        val colW = maxWidth / columns
        val iconSize = minOf(68.dp, rowH * (if (cb.showLabels) 0.6f else 0.75f), colW * 0.72f)
        val perPage = columns * rows
        val pages = remember(apps, perPage) { apps.chunked(perPage) }
        val state = rememberPagerState { pages.size }
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(state, Modifier.weight(1f), beyondViewportPageCount = 1) { p ->
                val list = pages[p]
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth().height(rowH)) {
                            for (c in 0 until columns) {
                                val app = list.getOrNull(r * columns + c)
                                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    if (app != null) key(app.key) { DrawerCell(app, cb, iconSize) }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(pages.size) { i ->
                    val sel = i == state.currentPage
                    Box(Modifier.padding(3.dp).size(if (sel) 9.dp else 6.dp).clip(CircleShape).background(Color.White.copy(alpha = if (sel) 1f else 0.4f)))
                }
            }
        }
    }
}

@Composable
fun DrawerCategoryTabs(
    tabs: List<Pair<String, List<AppInfo>>>,
    columns: Int,
    rows: Int,
    cb: DrawerCallbacks,
    padding: PaddingValues,
) {
    val state = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val tabsScroll = rememberLazyListState()
    LaunchedEffect(state.currentPage) { if (tabs.isNotEmpty()) tabsScroll.animateScrollToItem(state.currentPage) }
    Column(Modifier.fillMaxSize()) {
        // Ciasne zakładki-pigułki (styl OpenPDF) zamiast wysokiego paska kart
        if (tabs.isNotEmpty()) {
            LazyRow(
                state = tabsScroll,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(tabs, key = { i, _ -> i }) { i, (title, list) ->
                    val sel = state.currentPage == i
                    Text(
                        "$title  ${list.size}",
                        color = if (sel) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        style = if (sel) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodySmall.copy(shadow = LabelShadow),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) Color.White else Color.White.copy(alpha = 0.1f))
                            .clickable { scope.launch { state.animateScrollToPage(i) } }
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
            }
        }
        HorizontalPager(state, Modifier.weight(1f)) { p ->
            val list = tabs[p].second
            GridSizing(columns, rows, cb) { cell, icon ->
                LazyVerticalGrid(GridCells.Fixed(columns), contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                    items(list, key = { it.key }) { app -> DrawerCell(app, cb, icon, cell) }
                }
            }
        }
    }
}

