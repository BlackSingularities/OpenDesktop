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
import com.opendesktop.launcher.ui.components.drawAppIcon
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private fun Modifier.rootPosition(f: (Offset) -> Unit) = onGloballyPositioned { f(it.positionInRoot()) }

/** Punkt po rzutowaniu — używany do rysowania i trafiania palcem. */
private class Projected(val app: AppInfo, val x: Float, val y: Float, val size: Float, val depth: Float, val alpha: Float)

private class HitHolder {
    var items: List<Projected> = emptyList()
    fun hit(p: Offset): Projected? = items
        .filter { (it.x - p.x) * (it.x - p.x) + (it.y - p.y) * (it.y - p.y) < (it.size * 0.62f) * (it.size * 0.62f) }
        .maxByOrNull { it.depth }
}

/** Kotwica menu dla widoków rysowanych — menu pojawia się w miejscu ikony. */
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

@Composable
fun DrawerCarousel(apps: List<AppInfo>, cb: DrawerCallbacks) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val item = 150.dp
        val side = ((maxWidth - item) / 2).coerceAtLeast(0.dp)
        val state = rememberPagerState { apps.size }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state,
                pageSize = PageSize.Fixed(item),
                contentPadding = PaddingValues(horizontal = side),
                beyondViewportPageCount = 4,
                modifier = Modifier.fillMaxWidth().height(230.dp),
            ) { i ->
                val app = apps[i]
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        val o = (i - state.currentPage) - state.currentPageOffsetFraction
                        val ao = abs(o).coerceAtMost(3f)
                        rotationY = (-o * 48f).coerceIn(-70f, 70f)
                        val s = 1f - 0.16f * ao.coerceAtMost(2f)
                        scaleX = s; scaleY = s
                        alpha = (1f - 0.22f * ao).coerceIn(0.15f, 1f)
                        cameraDistance = 18f * density
                        translationX = -o * size.width * 0.18f
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    DrawerTouchable(app, cb) { pressed ->
                        com.opendesktop.launcher.ui.components.AppIcon(app.icon, "", 112.dp, cb.shape, false, pressed = pressed)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            val current = apps.getOrNull(state.currentPage)
            Text(
                current?.let(cb.label).orEmpty(),
                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                style = TextStyle(shadow = LabelShadow), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                "${state.currentPage + 1} / ${apps.size}",
                color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
            )
        }
    }
}

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
        val iconPx = with(density) { 58.dp.toPx() }
        val style = TextStyle(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, shadow = LabelShadow)
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
                if (p.depth > 0.75f) {
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
        )
        val style = TextStyle(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, shadow = LabelShadow)
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
                if (p.depth > 0.88f) {
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
        val iconPx = with(density) { 56.dp.toPx() }
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
                for (p in list.sortedBy { it.depth }) drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
            }
            if (focused != null) {
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
    val stepDeg = 18f
    LaunchedEffect(apps.size) { scroll.updateBounds(0f, (apps.size - 1).coerceAtLeast(0).toFloat()) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .rootPosition { origin = it }
            .canvasGestures(
                onTap = { p -> holder.hit(p)?.let { h -> cb.onTap(h.app, Rect(Offset(h.x - h.size / 2, h.y - h.size / 2) + origin, androidx.compose.ui.geometry.Size(h.size, h.size))) } },
                onLongPress = { p -> holder.hit(p)?.let { h -> anchor = h.app to (Offset(h.x, h.y) + origin); cb.onLongPress(h.app) } },
                onDrag = { d -> scope.launch { scroll.snapTo(scroll.value - d.y / 60f) } },
                onFling = { v ->
                    scope.launch {
                        scroll.animateDecay(-v.y / 60f, exponentialDecay(frictionMultiplier = 1.5f))
                        scroll.animateTo(scroll.value.roundToInt().toFloat(), spring(dampingRatio = 0.8f))
                    }
                },
            ),
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val radius = wPx * 0.35f
        val iconPx = with(density) { 54.dp.toPx() }
        val style = TextStyle(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, shadow = LabelShadow)

        Canvas(Modifier.fillMaxSize()) {
            val s = scroll.value
            val cx = size.width / 2
            val cy = size.height / 2
            val visible = 8
            val from = (s.toInt() - visible).coerceAtLeast(0)
            val to = (s.toInt() + visible).coerceAtMost(apps.lastIndex)
            val list = ArrayList<Projected>()

            for (i in from..to) {
                val d = i - s
                val theta = d * stepDeg * PI.toFloat() / 180f
                val z = cos(theta)
                val y = cy + d * 70f
                val x = cx + sin(theta) * radius
                val k = 0.6f + 0.4f * (z + 1) / 2
                val fade = (1f - abs(d) / (visible + 1)).coerceIn(0f, 1f)
                list += Projected(apps[i], x, y, iconPx * k, z, (0.3f + 0.7f * (z + 1) / 2) * fade)
            }
            list.sortBy { it.depth }
            holder.items = list.filter { it.depth > -0.3f }
            for (p in list) {
                drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
                if (p.depth > 0.7f && cb.showLabels) {
                    labelUnder(layouts, { t -> measurer.measure(t, style, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (iconPx * 2.2f).toInt())) }, p, cb.label(p.app), p.alpha)
                }
            }
        }
        CanvasMenuAnchor(anchor, cb, origin)
    }
}

// ------------------------------------------------------------------------- Wave

@Composable
fun DrawerWave(apps: List<AppInfo>, cb: DrawerCallbacks, padding: PaddingValues) {
    val scrollState = rememberLazyListState()
    LazyColumn(
        state = scrollState,
        contentPadding = padding,
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            val index = apps.indexOf(app)
            val offset = remember { Animatable(0f) }

            LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
                val visible = scrollState.firstVisibleItemIndex
                val distance = abs(index - visible)
                val targetOffset = sin(distance * 0.5f) * 20f
                offset.animateTo(targetOffset, spring(dampingRatio = 0.7f))
            }

            DrawerTouchable(app, cb, Modifier.fillMaxWidth().offset(x = offset.value.dp)) { pressed ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = if (pressed) 0.14f else 0.06f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.icon, "", 48.dp, cb.shape, false, pressed = pressed)
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

@Composable
fun DrawerStack(apps: List<AppInfo>, cb: DrawerCallbacks) {
    var currentIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardHeight = 400.dp
        val cardWidth = minOf(maxWidth - 48.dp, 360.dp)

        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            for (i in 0 until minOf(3, apps.size - currentIndex)) {
                val app = apps.getOrNull(currentIndex + i) ?: continue
                val offset = i * 12.dp
                val scale = 1f - i * 0.05f

                Box(
                    Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .offset(y = offset)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - i * 0.2f
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.1f - i * 0.02f))
                        .then(
                            if (i == 0) Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        val bounds = Rect(Offset.Zero, androidx.compose.ui.geometry.Size(cardWidth.toPx(), cardHeight.toPx()))
                                        cb.onTap(app, bounds)
                                    },
                                    onLongPress = { cb.onLongPress(app) },
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AppIcon(app.icon, "", 120.dp, cb.shape, false)
                        if (cb.showLabels) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                cb.label(app),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentIndex > 0) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { currentIndex-- },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("←", color = Color.White, fontSize = 24.sp)
                    }
                }
                if (currentIndex < apps.size - 1) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { currentIndex++ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("→", color = Color.White, fontSize = 24.sp)
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
    val cell = with(density) { 74.dp.toPx() }
    val perRow = 7
    val rowsCount = (apps.size + perRow - 1) / perRow
    val gridW = perRow * cell
    val gridH = rowsCount * cell * 0.87f
    LaunchedEffect(apps.size) {
        pan.updateBounds(Offset(-gridW / 2, -gridH / 2), Offset(gridW / 2, gridH / 2))
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
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = minOf(size.width, size.height) * 0.62f
            val list = ArrayList<Projected>()
            for (i in apps.indices) {
                val r = i / perRow
                val c = i % perRow
                val bx = (c - (perRow - 1) / 2f + if (r % 2 == 1) 0.5f else 0f) * cell
                val by = (r - (rowsCount - 1) / 2f) * cell * 0.87f
                val px = bx - pan.value.x
                val py = by - pan.value.y
                val d = sqrt(px * px + py * py)
                val t = (d / maxR).coerceIn(0f, 1.3f)
                val k = (1f - t * t * 0.62f).coerceIn(0.12f, 1f)
                // Soczewka: przyciąga dalsze bąbelki do środka
                val pull = 0.78f + 0.22f * k
                val x = cx + px * pull
                val y = cy + py * pull
                if (x < -cell || x > size.width + cell || y < -cell || y > size.height + cell) continue
                list += Projected(apps[i], x, y, cell * 0.84f * k, k, k.coerceIn(0.3f, 1f))
            }
            holder.items = list
            for (p in list) drawAppIcon(p.app.icon, Offset(p.x, p.y), p.size, p.alpha, cb.shape)
        }
        CanvasMenuAnchor(anchor, cb, origin)
    }
}
