package com.opendesktop.launcher.ui.desktop

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
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
    aiEnabled: Boolean,
    onDismiss: () -> Unit,
    onLaunch: (AppInfo, Rect) -> Unit,
    onRename: (String) -> Unit,
    onSuggestName: ((String) -> Unit) -> Unit,
    onRemoveApp: (String, Boolean) -> Unit,
    onAddApps: () -> Unit,
    onInfo: (AppInfo) -> Unit,
) {
    var name by remember(folder.id) { mutableStateOf(folder.label) }
    var menuKey by remember { mutableStateOf<String?>(null) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 600f)) }
    val list = folder.apps.mapNotNull { apps[it] }
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

    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { close() }) {
            val density = LocalDensity.current
            // Komórki i odstępy takie same jak siatka pulpitu
            val cell = cellH
            val footer = 34.dp
            val panelW = minOf(cellW * cols + 12.dp, maxWidth - 24.dp)
            val panelH = minOf(cell * rows + 6.dp + footer, maxHeight * 0.7f)
            val wPx = with(density) { panelW.toPx() }
            val hPx = with(density) { panelH.toPx() }
            val screenW = with(density) { maxWidth.toPx() }
            val screenH = with(density) { maxHeight.toPx() }
            val margin = with(density) { 12.dp.toPx() }
            val cx = if (anchor == Rect.Zero) screenW / 2 else anchor.center.x
            val cy = if (anchor == Rect.Zero) screenH / 2 else anchor.center.y
            val x = (cx - wPx / 2).coerceIn(margin, (screenW - wPx - margin).coerceAtLeast(margin))
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
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp),
            ) {
                LazyVerticalGrid(GridCells.Fixed(cols), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(list, key = { it.key }) { app ->
                        var bounds by remember { mutableStateOf(Rect.Zero) }
                        var pressed by remember { mutableStateOf(false) }
                        Box(Modifier.height(cell), contentAlignment = Alignment.Center) {
                            AppIcon(
                                app.icon, labelOf(app), iconSize, cfg.iconShape, cfg.showLabels,
                                pressed = pressed, labelSize = cfg.labelSize, labelColor = ink, shadow = !panel.light,
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
                            if (menuKey == app.key) {
                                Box(Modifier.align(Alignment.Center)) {
                                    ActionMenu(
                                        title = labelOf(app),
                                        onDismiss = { menuKey = null },
                                        actions = listOf(
                                            MenuAction("Wyjmij na pulpit", Icons.Rounded.Logout) { onRemoveApp(app.key, true) },
                                            MenuAction("Usuń z folderu", Icons.Rounded.Close) { onRemoveApp(app.key, false) },
                                            MenuAction("Informacje o aplikacji", Icons.Rounded.Info) { onInfo(app) },
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
                // Stopka: subtelna nazwa i mały plus
                Row(Modifier.fillMaxWidth().height(footer), verticalAlignment = Alignment.CenterVertically) {
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
                    if (aiEnabled) {
                        Icon(
                            Icons.Rounded.AutoAwesome, "Zaproponuj nazwę (AI)", tint = ink.copy(alpha = 0.45f),
                            modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onSuggestName { name = it } }.padding(6.dp),
                        )
                    }
                    Icon(
                        Icons.Rounded.Add, "Dodaj aplikacje", tint = ink.copy(alpha = 0.55f),
                        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onAddApps).padding(5.dp),
                    )
                }
            }
        }
    }
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
    val builtins = listOf(
        WidgetEntry(null, "clock", "Zegar z datą", "OpenDesktop", 4 to 2),
        WidgetEntry(null, "search", "Pasek wyszukiwania", "OpenDesktop", 5 to 1),
    )
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
                    item {
                        WidgetGroup("OpenDesktop", builtins) { e -> onPickBuiltin(e.builtin!!) }
                    }
                    val q = query.trim()
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
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(e.label, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontSize = 13.sp)
        Text("${e.span.first} × ${e.span.second}", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}
