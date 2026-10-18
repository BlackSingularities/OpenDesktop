package com.opendesktop.launcher.ui.effects

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Cząsteczki poruszające się w tle
class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    var life: Float = 1f,
)

@Composable
fun ParticleEffect(
    enabled: Boolean,
    particleCount: Int = 30,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val particles = remember {
        mutableStateListOf<Particle>().apply {
            repeat(particleCount) {
                add(
                    Particle(
                        x = Random.nextFloat() * 1080f,
                        y = Random.nextFloat() * 2400f,
                        vx = (Random.nextFloat() - 0.5f) * 2f,
                        vy = (Random.nextFloat() - 0.5f) * 2f,
                        size = Random.nextFloat() * 3f + 1f,
                        color = Color.White.copy(alpha = Random.nextFloat() * 0.3f + 0.1f),
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                particles.forEach { p ->
                    p.x += p.vx
                    p.y += p.vy

                    // Odbicie od krawędzi
                    if (p.x < 0 || p.x > 1080f) p.vx *= -1
                    if (p.y < 0 || p.y > 2400f) p.vy *= -1
                }
            }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(p.x, p.y),
            )
        }
    }
}

// Efekt parallax dla tapety
@Composable
fun ParallaxBackground(
    enabled: Boolean,
    offsetX: Float,
    offsetY: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

// Ślady za palcem podczas przesuwania
class SwipeTrail(
    val points: MutableList<Offset> = mutableListOf(),
    var alpha: Float = 1f,
)

@Composable
fun SwipeTrailEffect(
    enabled: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val trails = remember { mutableStateListOf<SwipeTrail>() }

    Canvas(modifier.fillMaxSize()) {
        trails.forEach { trail ->
            if (trail.points.size > 1) {
                val path = Path().apply {
                    moveTo(trail.points[0].x, trail.points[0].y)
                    for (i in 1 until trail.points.size) {
                        lineTo(trail.points[i].x, trail.points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = color.copy(alpha = trail.alpha * 0.5f),
                    style = Stroke(width = 4f),
                )
            }
        }
    }
}

// Efekt ripple przy dotknięciu ikony
@Composable
fun RippleEffect(
    triggered: Boolean,
    center: Offset,
    color: Color = Color.White,
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(triggered) {
        if (triggered) {
            animatable.snapTo(0f)
            animatable.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        if (animatable.value > 0f) {
            val radius = animatable.value * 200f
            val alpha = (1f - animatable.value) * 0.6f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 3f),
            )
        }
    }
}

// Animowane gwiazdki przy otwieraniu aplikacji
@Composable
fun SparkleEffect(
    triggered: Boolean,
    center: Offset,
    count: Int = 8,
) {
    val animatables = remember { List(count) { Animatable(0f) } }

    LaunchedEffect(triggered) {
        if (triggered) {
            animatables.forEachIndexed { index, anim ->
                kotlinx.coroutines.launch {
                    kotlinx.coroutines.delay(index * 30L)
                    anim.snapTo(0f)
                    anim.animateTo(1f, tween(500))
                }
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        animatables.forEachIndexed { index, anim ->
            if (anim.value > 0f) {
                val angle = (index.toFloat() / count) * 2f * Math.PI
                val distance = anim.value * 60f
                val x = center.x + cos(angle).toFloat() * distance
                val y = center.y + sin(angle).toFloat() * distance
                val alpha = 1f - anim.value

                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = alpha),
                    radius = 4f * (1f - anim.value * 0.5f),
                    center = Offset(x, y),
                )
            }
        }
    }
}

// Gradient animowany w tle
@Composable
fun AnimatedGradientBackground(enabled: Boolean, colors: List<Color>) {
    if (!enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offset",
    )

    Canvas(Modifier.fillMaxSize()) {
        val brush = Brush.verticalGradient(
            colors = colors,
            startY = size.height * offset,
            endY = size.height * (1f - offset),
        )
        drawRect(brush)
    }
}

// Efekt świecenia wokół wybranej ikony
fun DrawScope.drawGlowEffect(
    center: Offset,
    radius: Float,
    color: Color,
    intensity: Float = 1f,
) {
    for (i in 3 downTo 1) {
        val r = radius + i * 8f
        val alpha = (intensity * 0.15f * (4 - i) / 3f)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = r,
            center = center,
        )
    }
}

// Pulsujący efekt dla folderu
@Composable
fun rememberPulseAnimation(enabled: Boolean): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    return if (enabled) scale else 1f
}

// Efekt shake dla ikony
@Composable
fun rememberShakeAnimation(triggered: Boolean): Float {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(triggered) {
        if (triggered) {
            repeat(3) {
                animatable.animateTo(10f, tween(50))
                animatable.animateTo(-10f, tween(100))
                animatable.animateTo(0f, tween(50))
            }
        }
    }

    return animatable.value
}
