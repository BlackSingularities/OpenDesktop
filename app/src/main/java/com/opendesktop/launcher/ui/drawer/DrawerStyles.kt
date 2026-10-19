package com.opendesktop.launcher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
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
        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
fun DrawerGrid(sections: List<DrawerSection>, columns: Int, cb: DrawerCallbacks, padding: PaddingValues) {
    LazyVerticalGrid(GridCells.Fixed(columns), contentPadding = padding, modifier = Modifier.fillMaxSize()) {
        sections.forEach { s ->
            if (s.title != null) item(key = "h_${s.key}", span = { GridItemSpan(maxLineSpan) }) { SectionHeader(s.title) }
            items(s.apps, key = { "${s.key}_${it.key}" }) { app ->
                DrawerCell(app, cb, if (columns >= 6) 46.dp else 54.dp, Modifier.padding(vertical = 10.dp))
            }
        }
    }
}

/**
 * Perspektywa 3D: zwykła siatka, ale rzędy przy górnej i dolnej krawędzi odchylają się w głąb,
 * maleją i ściskają — jakby lista była nawinięta na bęben. Liczone w warstwie graficznej.
 */
@Composable
fun DrawerPerspective(apps: List<AppInfo>, columns: Int, cb: DrawerCallbacks, padding: PaddingValues) {
    val rows = remember(apps, columns) { apps.chunked(columns) }
    val state = rememberLazyListState()
    val iconSize = if (columns >= 6) 50.dp else 58.dp
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(top = 24.dp, bottom = padding.calculateBottomPadding()),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(rows, key = { _, r -> r.first().key }) { _, row ->
            val rowKey = row.first().key
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        val info = state.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.key == rowKey } ?: return@graphicsLayer
                        val h = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1).toFloat()
                        val top = (item.offset - info.viewportStartOffset).toFloat()
                        val center = (top + item.size / 2f) / h
                        val zone = 0.2f
                        cameraDistance = 7f * density
                        when {
                            center > 1f - zone -> {
                                // Dół: rząd kładzie się w głąb (górna krawędź ucieka do tyłu)
                                val t = ((center - (1f - zone)) / zone).coerceIn(0f, 1.6f)
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                rotationX = 62f * t.coerceAtMost(1f)
                                val s = 1f - 0.32f * t.coerceAtMost(1.3f)
                                scaleX = s; scaleY = s
                                translationY = -item.size * 0.55f * t * t
                                alpha = (1.2f - 0.6f * t).coerceIn(0f, 1f)
                            }
                            center < zone -> {
                                val t = ((zone - center) / zone).coerceIn(0f, 1.6f)
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                rotationX = -62f * t.coerceAtMost(1f)
                                val s = 1f - 0.32f * t.coerceAtMost(1.3f)
                                scaleX = s; scaleY = s
                                translationY = item.size * 0.55f * t * t
                                alpha = (1.2f - 0.6f * t).coerceIn(0f, 1f)
                            }
                        }
                    },
            ) {
                for (c in 0 until columns) {
                    val app = row.getOrNull(c)
                    Box(Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerAlphabet(apps: List<AppInfo>, cb: DrawerCallbacks, padding: PaddingValues) {
    val groups = remember(apps) {
        apps.groupBy { a -> cb.label(a).firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#" }
            .toSortedMap(compareBy<String> { it == "#" }.thenBy { it })
    }
    val letters = groups.keys.toList()
    val indexOf = remember(groups) {
        var i = 0
        val m = HashMap<String, Int>()
        groups.forEach { (l, list) -> m[l] = i; i += 1 + list.size }
        m
    }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var scrubbing by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize().padding(end = 28.dp)) {
            groups.forEach { (letter, list) ->
                stickyHeader(key = "h_$letter") {
                    Box(Modifier.padding(vertical = 4.dp).size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                        Text(letter, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                items(list, key = { "a_${it.key}" }) { app -> ListRow(app, cb) }
            }
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .width(28.dp)
                .fillMaxHeight(0.86f)
                .pointerInput(letters) {
                    fun jump(y: Float) {
                        if (letters.isEmpty()) return
                        val i = (y / size.height * letters.size).toInt().coerceIn(0, letters.lastIndex)
                        val l = letters[i]
                        if (scrubbing != l) {
                            scrubbing = l
                            scope.launch { state.scrollToItem(indexOf[l] ?: 0) }
                        }
                    }
                    awaitEachGesture {
                        val d = awaitFirstDown()
                        d.consume()
                        jump(d.position.y)
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull() ?: break
                            if (!c.pressed) break
                            c.consume()
                            jump(c.position.y)
                        }
                        scrubbing = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { l ->
                Text(
                    l,
                    color = if (l == scrubbing) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        scrubbing?.let { l ->
            Box(
                Modifier.align(Alignment.Center).size(96.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Text(l, color = MaterialTheme.colorScheme.onPrimary, fontSize = 44.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun DrawerPages(apps: List<AppInfo>, columns: Int, cb: DrawerCallbacks, fixedRows: Int = 0) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // fixedRows = 0 → tyle wierszy, ile się zmieści; inaczej wiersze ściskane do zadanej liczby
        val rows = if (fixedRows > 0) fixedRows else ((maxHeight - 40.dp) / 104.dp).toInt().coerceAtLeast(3)
        val rowH = if (fixedRows > 0) (maxHeight - 28.dp) / fixedRows else 104.dp
        val iconSize = minOf(54.dp, rowH * 0.55f)
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
    cb: DrawerCallbacks,
    padding: PaddingValues,
) {
    val state = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        if (tabs.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = state.currentPage.coerceIn(0, tabs.lastIndex),
                edgePadding = 12.dp,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = {},
                indicator = { positions ->
                    val i = state.currentPage.coerceIn(0, positions.lastIndex)
                    if (positions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(positions[i]).clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            ) {
                tabs.forEachIndexed { i, (title, list) ->
                    Tab(
                        selected = state.currentPage == i,
                        onClick = { scope.launch { state.animateScrollToPage(i) } },
                        text = { Text("$title  ${list.size}", maxLines = 1) },
                    )
                }
            }
        }
        HorizontalPager(state, Modifier.weight(1f)) { p ->
            val list = tabs[p].second
            LazyVerticalGrid(GridCells.Fixed(columns), contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                items(list, key = { it.key }) { app -> DrawerCell(app, cb, 54.dp, Modifier.padding(vertical = 10.dp)) }
            }
        }
    }
}

@Composable
fun DrawerTiles(apps: List<AppInfo>, usage: Map<String, Int>, cb: DrawerCallbacks, padding: PaddingValues) {
    val top = remember(apps, usage) {
        apps.sortedByDescending { usage[it.key] ?: 0 }.filter { (usage[it.key] ?: 0) > 0 }.take(6).map { it.key }.toSet()
    }
    LazyVerticalGrid(
        GridCells.Fixed(4),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
    ) {
        items(apps, key = { it.key }, span = { GridItemSpan(if (it.key in top) 2 else 1) }) { app ->
            val wide = app.key in top
            DrawerTouchable(app, cb, Modifier.fillMaxWidth().height(96.dp)) { pressed ->
                val base = Color(app.color)
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(lerp(base, Color.Black, if (pressed) 0.55f else 0.35f))
                        .padding(10.dp),
                ) {
                    AppIcon(app.icon, "", if (wide) 48.dp else 40.dp, cb.shape, false, pressed = pressed, modifier = Modifier.align(if (wide) Alignment.CenterStart else Alignment.TopCenter))
                    if (cb.showLabels) {
                        Text(
                            cb.label(app),
                            color = Color.White,
                            fontSize = if (wide) 15.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(if (wide) Alignment.BottomEnd else Alignment.BottomCenter)
                                .then(if (wide) Modifier.offset(x = 0.dp) else Modifier),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerCompact(apps: List<AppInfo>, columns: Int, cb: DrawerCallbacks, padding: PaddingValues) {
    LazyVerticalGrid(
        GridCells.Fixed(columns),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(apps, key = { it.key }) { app ->
            DrawerCell(app, cb, 42.dp, Modifier.padding(4.dp))
        }
    }
}

/** Częste na górze: wyróżniony pasek najczęściej używanych, pod nim pełna siatka. */
@Composable
fun DrawerSearchBar(apps: List<AppInfo>, usage: Map<String, Int>, columns: Int, cb: DrawerCallbacks, padding: PaddingValues) {
    val frequent = remember(apps, usage, columns) {
        apps.filter { (usage[it.key] ?: 0) > 0 }.sortedByDescending { usage[it.key] ?: 0 }.take(columns)
    }
    LazyVerticalGrid(
        GridCells.Fixed(columns),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
    ) {
        if (frequent.isNotEmpty()) {
            item(key = "freq", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.08f)).padding(vertical = 8.dp),
                ) {
                    Text(
                        "NAJCZĘŚCIEJ UŻYWANE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp, modifier = Modifier.padding(start = 14.dp, bottom = 4.dp),
                    )
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until columns) {
                            val app = frequent.getOrNull(c)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (app != null) key("f_${app.key}") { DrawerCell(app, cb, 50.dp, Modifier.padding(vertical = 4.dp)) }
                            }
                        }
                    }
                }
            }
        }
        items(apps, key = { it.key }) { app ->
            DrawerCell(app, cb, 52.dp, Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun DrawerMaterial(apps: List<AppInfo>, cb: DrawerCallbacks, padding: PaddingValues) {
    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        items(apps, key = { it.key }) { app ->
            DrawerTouchable(app, cb, Modifier.fillMaxWidth()) { pressed ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (pressed) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.08f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.icon, "", 52.dp, cb.shape, false, pressed = pressed)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        if (cb.showLabels) Text(cb.label(app), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val cats = cb.categoriesOf(app)
                        if (cats.isNotEmpty()) {
                            Text(
                                cats.joinToString("  ") { (it.emoji + " " + it.name).trim() },
                                color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DraweriOS(apps: List<AppInfo>, columns: Int, cb: DrawerCallbacks, padding: PaddingValues) {
    LazyVerticalGrid(
        GridCells.Fixed(columns),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            DrawerCell(app, cb, 60.dp, Modifier.padding(vertical = 8.dp))
        }
    }
}

/** Pozioma siatka: ciągłe przewijanie w bok; liczba wierszy z ustawień (0 = ile się zmieści). */
@Composable
fun DrawerHorizontal(apps: List<AppInfo>, fixedRows: Int, cb: DrawerCallbacks) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = 24.dp)) {
        val rows = if (fixedRows > 0) fixedRows else (maxHeight / 100.dp).toInt().coerceAtLeast(3)
        val cellH = maxHeight / rows
        val iconSize = minOf(56.dp, cellH * 0.55f)
        LazyHorizontalGrid(
            GridCells.Fixed(rows),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.key }) { app ->
                DrawerCell(app, cb, iconSize, Modifier.width(maxWidth / 5).height(cellH))
            }
        }
    }
}

/**
 * Przepływ: mozaika w kolumnach o różnej wysokości. Rozmiar kafelka zależy od tego, jak często
 * aplikacja jest używana względem najczęściej używanej (trzy progi).
 */
@Composable
fun DrawerFlow(apps: List<AppInfo>, usage: Map<String, Int>, cb: DrawerCallbacks, padding: PaddingValues) {
    val maxUse = remember(usage) { (usage.values.maxOrNull() ?: 0).coerceAtLeast(1) }
    LazyVerticalStaggeredGrid(
        StaggeredGridCells.Fixed(3),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            val ratio = (usage[app.key] ?: 0).toFloat() / maxUse
            val level = when {
                ratio > 0.5f -> 2
                ratio > 0.15f -> 1
                else -> 0
            }
            DrawerTouchable(app, cb, Modifier.fillMaxWidth().height((88 + level * 34).dp)) { pressed ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(lerp(Color(app.color), Color.Black, if (pressed) 0.7f else 0.55f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(app.icon, cb.label(app), (40 + level * 14).dp, cb.shape, cb.showLabels, pressed = pressed, labelSize = cb.labelSize)
                }
            }
        }
    }
}
