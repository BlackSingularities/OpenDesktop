package com.opendesktop.launcher.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.ui.components.AppIcon
import com.opendesktop.launcher.ui.components.LabelShadow
import com.opendesktop.launcher.ui.components.canvasGestures
import com.opendesktop.launcher.ui.components.LocalBadgeCounts
import com.opendesktop.launcher.ui.components.badgeCount
import com.opendesktop.launcher.ui.components.drawAppIcon
import com.opendesktop.launcher.ui.components.drawBadge
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private fun Modifier.rootPosition(f: (Offset) -> Unit) = onGloballyPositioned { f(it.positionInRoot()) }

/** Współrzędne osiowe (q, r) kolejnych komórek heksagonalnej spirali od środka. */
private fun hexSpiral(n: Int): List<Pair<Int, Int>> {
    val out = ArrayList<Pair<Int, Int>>(n)
    if (n == 0) return out
    out += 0 to 0
    val dirs = listOf(1 to 0, 1 to -1, 0 to -1, -1 to 0, -1 to 1, 0 to 1)
    var ring = 1
    while (out.size < n) {
        var q = -ring; var r = ring // start: kierunek 4 × ring
        for (side in 0 until 6) {
            repeat(ring) {
                if (out.size < n) out += q to r
                q += dirs[side].first; r += dirs[side].second
            }
        }
        ring++
    }
    return out
}

private fun hexToPx(q: Int, r: Int, cell: Float) = Offset(cell * (q + r / 2f), cell * 0.866f * r)

/** Punkt po rzutowaniu – używany do rysowania i trafiania palcem. */
private class Projected(val app: AppInfo, val x: Float, val y: Float, val size: Float, val depth: Float, val alpha: Float)

private class HitHolder {
    var items: List<Projected> = emptyList()
    fun hit(p: Offset): Projected? = items
        .filter { (it.x - p.x) * (it.x - p.x) + (it.y - p.y) * (it.y - p.y) < (it.size * 0.62f) * (it.size * 0.62f) }
        .maxByOrNull { it.depth }
}

/** Kotwica menu dla widoków rysowanych – menu pojawia się w miejscu ikony. */
@Composable
private fun CanvasMenuAnchor(anchor: Pair<AppInfo, Offset>?, cb: DrawerCallbacks, origin: Offset) {
    val a = anchor ?: return
    if (cb.menuKey != a.first.key) return
    Box(Modifier.offset { IntOffset((a.second.x - origin.x).roundToInt(), (a.second.y - origin.y).roundToInt()) }.size(1.dp)) {
        cb.menu(a.first)
    }
}

private fun DrawScope.labelUnder(
    layouts: HashMap<String, TextLayoutResult>,
    measure: (String) -> TextLayoutResult,
    p: Projected,
    label: String,
    alpha: Float,
) {
    val layout = layouts.getOrPut(p.app.key) { measure(label) }
    drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y + p.size / 2f + 4f), alpha = alpha)
}

// ------------------------------------------------------------------------- Karuzela

// ------------------------------------------------------------------------- Helisa

@Composable
fun DrawerHelix(apps: List<AppInfo>, cb: DrawerCallbacks) {
    val scroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val holder = remember { HitHolder() }
    var anchor by remember { mutableStateOf<Pair<AppInfo, Offset>?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val layouts = remember(apps) { HashMap<String, TextLayoutResult>() }
    val pitchPx = with(density) { 30.dp.toPx() }
    LaunchedEffect(apps.size) { scroll.updateBounds(0f, (apps.size - 1).coerceAtLeast(0).toFloat()) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .rootPosition { origin = it }
            .canvasGestures(
                onTap = { p -> holder.hit(p)?.let { h -> cb.onTap(h.app, Rect(Offset(h.x - h.size / 2, h.y - h.size / 2) + origin, androidx.compose.ui.geometry.Size(h.size, h.size))) } },
                onLongPress = { p -> holder.hit(p)?.let { h -> anchor = h.app to (Offset(h.x, h.y) + origin); cb.onLongPress(h.app) } },
                onDrag = { d -> scope.launch { scroll.snapTo(scroll.value - d.y / pitchPx) } },
                onFling = { v ->
                    scope.launch {
                        scroll.animateDecay(-v.y / pitchPx, exponentialDecay(frictionMultiplier = 1.4f))
                        scroll.animateTo(scroll.value.roundToInt().toFloat(), spring(dampingRatio = 0.8f))
                    }
                },
            ),
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val radius = wPx * 0.32f
        val iconPx = with(density) { 58.dp.toPx() } * cb.iconScale
        val style = TextStyle(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, shadow = LabelShadow)
        val badges = LocalBadgeCounts.current
        Canvas(Modifier.fillMaxSize()) {
            val s = scroll.value
            val perTurn = 7f
            val step = (2 * PI / perTurn).toFloat()
            val cx = size.width / 2
            val cy = size.height / 2
            val visible = (size.height / pitchPx / 2 + 2).toInt()
            val from = (s.toInt() - visible).coerceAtLeast(0)
            val to = (s.toInt() + visible).coerceAtMost(apps.lastIndex)
            val list = ArrayList<Projected>()
            for (i in from..to) {
                val d = i - s
                val theta = d * step
                val z = cos(theta)
                val x = cx + sin(theta) * radius
                val y = cy + d * pitchPx
                val k = 0.5f + 0.5f * (z + 1) / 2
                val fade = (1f - abs(d) / (visible + 1)).coerceIn(0f, 1f)
                list += Projected(apps[i], x, y, iconPx * k, z, (0.25f + 0.75f * (z + 1) / 2) * fade)
            }
            list.sortBy { it.depth }
            holder.items = list.filter { it.depth > -0.2f }
            for (p in list) {
                drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
                drawBadge(badges[p.app.packageName] ?: 0, Offset(p.x, p.y), p.size, p.alpha)
                if (p.depth > 0.75f && cb.showLabels) {
                    labelUnder(layouts, { t -> measurer.measure(t, style, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (iconPx * 2.2f).toInt())) }, p, cb.label(p.app), p.alpha)
                }
            }
        }
        CanvasMenuAnchor(anchor, cb, origin)
    }
}

// ------------------------------------------------------------------------- Kula

@Composable
fun DrawerSphere(apps: List<AppInfo>, cb: DrawerCallbacks) {
    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(-0.25f) }
    var velYaw by remember { mutableFloatStateOf(0.25f) }
    var velPitch by remember { mutableFloatStateOf(0f) }
    var touching by remember { mutableStateOf(false) }
    val holder = remember { HitHolder() }
    var anchor by remember { mutableStateOf<Pair<AppInfo, Offset>?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val layouts = remember(apps) { HashMap<String, TextLayoutResult>() }
    val points = remember(apps.size) {
        val n = apps.size.coerceAtLeast(1)
        val golden = (PI * (3 - sqrt(5.0))).toFloat()
        List(apps.size) { i ->
            val y = 1 - (i + 0.5f) / n * 2
            val r = sqrt(1 - y * y)
            val phi = i * golden
            floatArrayOf(cos(phi) * r, y, sin(phi) * r)
        }
    }
    // Bezwładność i powolny autoobrót, gdy nikt nie dotyka
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last != 0L && !touching) {
                    val dt = ((t - last) / 1e9f).coerceAtMost(0.05f)
                    yaw += velYaw * dt
                    pitch = (pitch + velPitch * dt).coerceIn(-1.2f, 1.2f)
                    val damp = exp(-2.2f * dt)
                    velYaw = 0.18f + (velYaw - 0.18f) * damp
                    velPitch *= damp
                }
                last = t
            }
        }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .rootPosition { origin = it }
            .canvasGestures(
                onTouch = { touching = it },
                onTap = { p -> holder.hit(p)?.let { h -> cb.onTap(h.app, Rect(Offset(h.x - h.size / 2, h.y - h.size / 2) + origin, androidx.compose.ui.geometry.Size(h.size, h.size))) } },
                onLongPress = { p -> holder.hit(p)?.let { h -> anchor = h.app to (Offset(h.x, h.y) + origin); cb.onLongPress(h.app) } },
                onDrag = { d ->
                    val k = 0.006f
                    yaw += d.x * k
                    pitch = (pitch - d.y * k).coerceIn(-1.2f, 1.2f)
                },
                onFling = { v ->
                    velYaw = v.x * 0.006f
                    velPitch = -v.y * 0.006f * 0.5f
                },
            ),
    ) {
        val minSide = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val radius = minSide * 0.48f
        val baseIcon = (radius * 3.2f / sqrt(apps.size.coerceAtLeast(1).toFloat())).coerceIn(
            with(density) { 28.dp.toPx() }, with(density) { 70.dp.toPx() },
        ) * cb.iconScale
        val style = TextStyle(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, shadow = LabelShadow)
        val badges = LocalBadgeCounts.current
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val cyaw = cos(yaw); val syaw = sin(yaw)
            val cp = cos(pitch); val sp = sin(pitch)
            val list = ArrayList<Projected>(apps.size)
            for (i in apps.indices) {
                val pt = points[i]
                val x1 = pt[0] * cyaw + pt[2] * syaw
                val z1 = -pt[0] * syaw + pt[2] * cyaw
                val y2 = pt[1] * cp - z1 * sp
                val z2 = pt[1] * sp + z1 * cp
                if (z2 < -0.35f) continue
                val persp = 1f / (1.9f - z2 * 0.6f)
                val k = 0.45f + 0.55f * (z2 + 1) / 2
                list += Projected(apps[i], cx + x1 * radius * persp * 1.3f, cy + y2 * radius * persp * 1.3f, baseIcon * k, z2, (0.2f + 0.8f * (z2 + 1) / 2).coerceIn(0f, 1f))
            }
            list.sortBy { it.depth }
            holder.items = list.filter { it.depth > 0f }
            for (p in list) {
                drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
                drawBadge(badges[p.app.packageName] ?: 0, Offset(p.x, p.y), p.size, p.alpha)
                if (p.depth > 0.88f && cb.showLabels) {
                    labelUnder(layouts, { t -> measurer.measure(t, style, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (baseIcon * 2.4f).toInt())) }, p, cb.label(p.app), p.alpha)
                }
            }
        }
        CanvasMenuAnchor(anchor, cb, origin)
    }
}

// ------------------------------------------------------------------------- Koło

@Composable
fun DrawerWheel(apps: List<AppInfo>, cb: DrawerCallbacks) {
    val scroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val holder = remember { HitHolder() }
    var anchor by remember { mutableStateOf<Pair<AppInfo, Offset>?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val stepDeg = 11f
    LaunchedEffect(apps.size) { scroll.updateBounds(0f, (apps.size - 1).coerceAtLeast(0).toFloat()) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .rootPosition { origin = it },
    ) {
        val hPx = with(density) { maxHeight.toPx() }
        val wPx = with(density) { maxWidth.toPx() }
        val radius = hPx * 0.46f
        val arcPx = radius * (stepDeg * PI.toFloat() / 180f)
        val iconPx = with(density) { 56.dp.toPx() } * cb.iconScale
        val focused = apps.getOrNull(scroll.value.roundToInt().coerceIn(0, (apps.size - 1).coerceAtLeast(0)))
        Box(
            Modifier
                .fillMaxSize()
                .canvasGestures(
                    onTap = { p -> holder.hit(p)?.let { h -> cb.onTap(h.app, Rect(Offset(h.x - h.size / 2, h.y - h.size / 2) + origin, androidx.compose.ui.geometry.Size(h.size, h.size))) } },
                    onLongPress = { p -> holder.hit(p)?.let { h -> anchor = h.app to (Offset(h.x, h.y) + origin); cb.onLongPress(h.app) } },
                    onDrag = { d -> scope.launch { scroll.snapTo(scroll.value - d.y / arcPx) } },
                    onFling = { v ->
                        scope.launch {
                            scroll.animateDecay(-v.y / arcPx, exponentialDecay(frictionMultiplier = 1.6f))
                            scroll.animateTo(scroll.value.roundToInt().toFloat(), spring(dampingRatio = 0.75f))
                        }
                    },
                ),
        ) {
            val badges = LocalBadgeCounts.current
            Canvas(Modifier.fillMaxSize()) {
                val ccx = wPx + radius * 0.42f
                val ccy = size.height / 2
                val s = scroll.value
                val list = ArrayList<Projected>()
                val span = (85f / stepDeg).toInt() + 1
                val from = (s.toInt() - span).coerceAtLeast(0)
                val to = (s.toInt() + span).coerceAtMost(apps.lastIndex)
                // Tarcza
                drawCircle(Color.White.copy(alpha = 0.05f), radius + iconPx * 0.9f, Offset(ccx, ccy))
                drawCircle(Color.White.copy(alpha = 0.04f), radius - iconPx * 0.9f, Offset(ccx, ccy))
                for (i in from..to) {
                    val deg = 180f + (i - s) * stepDeg
                    val rad = deg * PI.toFloat() / 180f
                    val x = ccx + cos(rad) * radius
                    val y = ccy + sin(rad) * radius
                    val dist = abs(i - s)
                    val k = (1.25f - dist * 0.09f).coerceIn(0.55f, 1.25f)
                    list += Projected(apps[i], x, y, iconPx * k, -dist, (1f - dist * 0.1f).coerceIn(0.2f, 1f))
                }
                holder.items = list
                for (p in list.sortedBy { it.depth }) {
                    drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
                    drawBadge(badges[p.app.packageName] ?: 0, Offset(p.x, p.y), p.size, p.alpha)
                }
            }
            if (focused != null && cb.showLabels) {
                val style = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, shadow = LabelShadow)
                Column(Modifier.align(Alignment.CenterStart).padding(start = 24.dp).fillMaxWidth(0.5f)) {
                    Text(cb.label(focused), style = style, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val cats = cb.categoriesOf(focused)
                    if (cats.isNotEmpty()) Text(cats.joinToString { (it.emoji + " " + it.name).trim() }, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                    Text("Dotknij ikony, aby otworzyć", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                }
            }
        }
        CanvasMenuAnchor(anchor, cb, origin)
    }
}

// ------------------------------------------------------------------------- Cylinder

@Composable
fun DrawerCylinder(apps: List<AppInfo>, cb: DrawerCallbacks) {
    val scroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val holder = remember { HitHolder() }
    var anchor by remember { mutableStateOf<Pair<AppInfo, Offset>?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val layouts = remember(apps) { HashMap<String, TextLayoutResult>() }
    // Cylinder: na każdym pierścieniu kilka aplikacji dookoła; poziomy gest obraca, pionowy przewija pierścienie
    val yaw = remember { Animatable(0f) }
    val perRing = 9
    val rings = (apps.size + perRing - 1) / perRing
    val rowPx = with(density) { 82.dp.toPx() }
    val yawPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() } * 0.42f
    LaunchedEffect(rings) { scroll.updateBounds(0f, (rings - 1).coerceAtLeast(0).toFloat()) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .rootPosition { origin = it }
            .canvasGestures(
                onTap = { p -> holder.hit(p)?.let { h -> cb.onTap(h.app, Rect(Offset(h.x - h.size / 2, h.y - h.size / 2) + origin, androidx.compose.ui.geometry.Size(h.size, h.size))) } },
                onLongPress = { p -> holder.hit(p)?.let { h -> anchor = h.app to (Offset(h.x, h.y) + origin); cb.onLongPress(h.app) } },
                onDrag = { d ->
                    scope.launch { yaw.snapTo(yaw.value + d.x / yawPx) }
                    scope.launch { scroll.snapTo(scroll.value - d.y / rowPx) }
                },
                onFling = { v ->
                    scope.launch { yaw.animateDecay(v.x / yawPx, exponentialDecay(frictionMultiplier = 2f)) }
                    scope.launch {
                        scroll.animateDecay(-v.y / rowPx, exponentialDecay(frictionMultiplier = 1.6f))
                        scroll.animateTo(scroll.value.roundToInt().toFloat(), spring(dampingRatio = 0.8f))
                    }
                },
            ),
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val radius = wPx * 0.40f
        val iconPx = with(density) { 50.dp.toPx() } * cb.iconScale
        val style = TextStyle(color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, shadow = LabelShadow)
        val step = (2 * PI / perRing).toFloat()

        val badges = LocalBadgeCounts.current
        Canvas(Modifier.fillMaxSize()) {
            val s = scroll.value
            val cx = size.width / 2
            val cy = size.height * 0.42f
            val list = ArrayList<Projected>()
            val visibleRings = (size.height / rowPx / 2 + 2).toInt()
            val fromRing = (s.toInt() - visibleRings).coerceAtLeast(0)
            val toRing = (s.toInt() + visibleRings).coerceAtMost(rings - 1)
            for (r in fromRing..toRing) {
                val dy = r - s
                // Pierścienie poza środkiem lekko się zwężają – złudzenie walca widzianego z boku
                val ringScale = 1f - 0.06f * abs(dy).coerceAtMost(4f)
                val fade = (1f - abs(dy) / (visibleRings + 0.5f)).coerceIn(0f, 1f)
                for (c in 0 until perRing) {
                    val i = r * perRing + c
                    if (i > apps.lastIndex) break
                    // Co drugi pierścień przesunięty o pół kroku – ciaśniejsze, „cegiełkowe” ułożenie
                    val theta = c * step + yaw.value + if (r % 2 == 1) step / 2 else 0f
                    val z = cos(theta)
                    val x = cx + sin(theta) * radius * ringScale
                    val y = cy + dy * rowPx
                    val k = 0.5f + 0.5f * (z + 1) / 2
                    list += Projected(apps[i], x, y, iconPx * k * ringScale, z, ((0.15f + 0.85f * (z + 1) / 2) * fade).coerceIn(0f, 1f))
                }
            }
            list.sortBy { it.depth }
            holder.items = list.filter { it.depth > 0f }
            for (p in list) {
                drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
                drawBadge(badges[p.app.packageName] ?: 0, Offset(p.x, p.y), p.size, p.alpha)
                if (p.depth > 0.55f && cb.showLabels) {
                    labelUnder(layouts, { t -> measurer.measure(t, style, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (iconPx * 1.7f).toInt())) }, p, cb.label(p.app), p.alpha)
                }
            }
        }
        CanvasMenuAnchor(anchor, cb, origin)
    }
}

// ------------------------------------------------------------------------- Wave

/**
 * Fala: przesunięcie wiersza w bok zależy od jego położenia na ekranie, więc przy przewijaniu
 * przez listę przechodzi płynna fala. Liczone w warstwie graficznej – bez rekompozycji.
 */
@Composable
fun DrawerWave(apps: List<AppInfo>, cb: DrawerCallbacks, padding: PaddingValues) {
    val state = rememberLazyListState()
    val density = LocalDensity.current
    val amp = with(density) { 22.dp.toPx() }
    LazyColumn(
        state = state,
        contentPadding = padding,
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            DrawerTouchable(
                app, cb,
                Modifier.fillMaxWidth().graphicsLayer {
                    val info = state.layoutInfo
                    val item = info.visibleItemsInfo.firstOrNull { it.key == app.key }
                    if (item != null) {
                        val h = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1).toFloat()
                        val y = (item.offset + item.size / 2f) / h
                        translationX = sin(y * 2f * PI.toFloat()) * amp
                        val edge = (minOf(y, 1f - y) * 6f).coerceIn(0f, 1f)
                        alpha = 0.35f + 0.65f * edge
                        val s = 0.9f + 0.1f * edge
                        scaleX = s; scaleY = s
                    }
                },
            ) { pressed ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = if (pressed) 0.14f else 0.06f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.icon, "", 48.dp * cb.iconScale, cb.shape, false, pressed = pressed, badgeCount = badgeCount(app))
                    Spacer(Modifier.width(14.dp))
                    if (cb.showLabels) {
                        Text(cb.label(app), color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------- Stack

/**
 * Stos kart: zwykła przewijana lista kart, ale karta wyjeżdżająca u góry zatrzymuje się,
 * maleje i blednie, a kolejne nasuwają się na nią – jak przekładane karty w portfelu.
 */
@Composable
fun DrawerStack(apps: List<AppInfo>, cb: DrawerCallbacks, padding: PaddingValues) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
    ) {
        itemsIndexed(apps, key = { _, a -> a.key }) { index, app ->
            DrawerTouchable(
                app, cb,
                Modifier
                    .fillMaxWidth()
                    .zIndex(index.toFloat())
                    .graphicsLayer {
                        val item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == app.key } ?: return@graphicsLayer
                        val top = item.offset.toFloat()
                        if (top < 0f) {
                            // Karta „przyklejona” u góry: kompensujemy przewinięcie i chowamy ją pod następne
                            val t = (-top / item.size).coerceIn(0f, 1f)
                            translationY = -top - t * 18f
                            val s = 1f - 0.08f * t
                            scaleX = s; scaleY = s
                            alpha = 1f - t
                        }
                    },
            ) { pressed ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1C1C1C))
                        .background(Color.White.copy(alpha = if (pressed) 0.12f else 0f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.icon, "", 52.dp * cb.iconScale, cb.shape, false, pressed = pressed, badgeCount = badgeCount(app))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        if (cb.showLabels) Text(cb.label(app), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    }
}

@Composable
fun DrawerBubbles(apps: List<AppInfo>, cb: DrawerCallbacks) {
    val pan = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val holder = remember { HitHolder() }
    var anchor by remember { mutableStateOf<Pair<AppInfo, Offset>?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val cell = with(density) { 60.dp.toPx() } * cb.iconScale
    // Plaster miodu jak w zegarku: spirala heksagonalna od środka – pierwsze aplikacje listy w centrum
    val coords = remember(apps.size) { hexSpiral(apps.size) }
    val extent = remember(coords) { coords.maxOfOrNull { (q, r) -> hexToPx(q, r, 1f).getDistance() } ?: 0f } * cell
    LaunchedEffect(extent) {
        pan.updateBounds(Offset(-extent, -extent), Offset(extent, extent))
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .rootPosition { origin = it }
            .canvasGestures(
                onTap = { p -> holder.hit(p)?.let { h -> cb.onTap(h.app, Rect(Offset(h.x - h.size / 2, h.y - h.size / 2) + origin, androidx.compose.ui.geometry.Size(h.size, h.size))) } },
                onLongPress = { p -> holder.hit(p)?.let { h -> anchor = h.app to (Offset(h.x, h.y) + origin); cb.onLongPress(h.app) } },
                onDrag = { d -> scope.launch { pan.snapTo(pan.value - d) } },
                onFling = { v -> scope.launch { pan.animateDecay(Offset(-v.x, -v.y), exponentialDecay(frictionMultiplier = 1.8f)) } },
            ),
    ) {
        val style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, shadow = LabelShadow)
        val badges = LocalBadgeCounts.current
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            // Łagodna soczewka: pełny rozmiar w środku, przy krawędziach ok. 40%
            val maxR = minOf(size.width, size.height) * 0.55f
            val list = ArrayList<Projected>()
            for (i in apps.indices) {
                val (q, r) = coords[i]
                val b = hexToPx(q, r, cell)
                val px = b.x - pan.value.x
                val py = b.y - pan.value.y
                val t = (sqrt(px * px + py * py) / maxR).coerceIn(0f, 1.6f)
                val k = (1f - 0.38f * t * t).coerceIn(0.32f, 1f)
                val pull = 0.86f + 0.14f * k
                val x = cx + px * pull
                val y = cy + py * pull
                if (x < -cell || x > size.width + cell || y < -cell || y > size.height + cell) continue
                list += Projected(apps[i], x, y, cell * 0.9f * k, k, (0.35f + 0.65f * k).coerceIn(0f, 1f))
            }
            holder.items = list
            for (p in list) {
                drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
                drawBadge(badges[p.app.packageName] ?: 0, Offset(p.x, p.y), p.size, p.alpha)
            }
            // Podpis tylko aplikacji najbliżej środka
            if (cb.showLabels) {
                list.minByOrNull { (it.x - cx) * (it.x - cx) + (it.y - cy) * (it.y - cy) }?.let { c ->
                    val layout = measurer.measure(cb.label(c.app), style, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (size.width * 0.6f).toInt()))
                    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, size.height - layout.size.height - 24f))
                }
            }
        }
        CanvasMenuAnchor(anchor, cb, origin)
    }
}
