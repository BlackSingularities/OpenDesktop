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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
                Text(cb.label(app), color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
fun DrawerPages(apps: List<AppInfo>, columns: Int, cb: DrawerCallbacks) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rows = ((maxHeight - 40.dp) / 104.dp).toInt().coerceAtLeast(3)
        val perPage = columns * rows
        val pages = remember(apps, perPage) { apps.chunked(perPage) }
        val state = rememberPagerState { pages.size }
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(state, Modifier.weight(1f), beyondViewportPageCount = 1) { p ->
                val list = pages[p]
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth().height(104.dp)) {
                            for (c in 0 until columns) {
                                val app = list.getOrNull(r * columns + c)
                                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    if (app != null) key(app.key) { DrawerCell(app, cb, 54.dp) }
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

@Composable
fun DrawerSearchBar(apps: List<AppInfo>, columns: Int, cb: DrawerCallbacks, padding: PaddingValues) {
    LazyVerticalGrid(
        GridCells.Fixed(columns),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            DrawerCell(app, cb, 52.dp, Modifier.padding(vertical = 6.dp))
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
                        Text(cb.label(app), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
fun DrawerHorizontal(apps: List<AppInfo>, rows: Int, cb: DrawerCallbacks) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = ((maxWidth - 32.dp) / 80.dp).toInt().coerceAtLeast(3)
        val perScreen = rows * columns
        val screens = remember(apps, perScreen) { apps.chunked(perScreen) }

        HorizontalPager(
            state = rememberPagerState { screens.size },
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        ) { page ->
            val screenApps = screens[page]
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (row in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 0 until columns) {
                            val app = screenApps.getOrNull(row * columns + col)
                            Box(Modifier.weight(1f).height(100.dp), contentAlignment = Alignment.Center) {
                                if (app != null) key(app.key) { DrawerCell(app, cb, 56.dp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerFlow(apps: List<AppInfo>, usage: Map<String, Int>, cb: DrawerCallbacks, padding: PaddingValues) {
    val sizes = remember(apps, usage) {
        apps.map { app ->
            val u = usage[app.key] ?: 0
            when {
                u > 50 -> 2
                u > 20 -> 1
                else -> 1
            }
        }
    }

    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        items(apps.chunked(3).size) { rowIndex ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val startIdx = rowIndex * 3
                for (i in 0 until 3) {
                    val idx = startIdx + i
                    if (idx < apps.size) {
                        val app = apps[idx]
                        val size = sizes[idx]
                        Box(
                            Modifier.weight(size.toFloat()).height((60 + size * 20).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            DrawerTouchable(app, cb, Modifier.fillMaxSize()) { pressed ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = if (pressed) 0.16f else 0.08f))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AppIcon(app.icon, if (cb.showLabels) cb.label(app) else "", (36 + size * 12).dp, cb.shape, cb.showLabels, pressed = pressed, labelSize = cb.labelSize)
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
