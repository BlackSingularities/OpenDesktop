package com.opendesktop.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.IconShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

val SquircleShape: Shape = GenericShape { size, _ ->
    val n = 4.0
    val a = size.width / 2.0
    val b = size.height / 2.0
    val steps = 64
    for (i in 0..steps) {
        val t = i / steps.toDouble() * 2 * PI
        val c = cos(t)
        val s = sin(t)
        val x = a + a * sign(c) * abs(c).pow(2 / n)
        val y = b + b * sign(s) * abs(s).pow(2 / n)
        if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
    }
    close()
}

fun iconShapeOf(shape: IconShape): Shape? = when (shape) {
    IconShape.System -> null
    IconShape.Circle -> CircleShape
    IconShape.Squircle -> SquircleShape
    IconShape.RoundedSquare -> RoundedCornerShape(24)
    IconShape.Square -> RoundedCornerShape(8)
    IconShape.Teardrop -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 14, bottomStartPercent = 50)
}

val LabelShadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 1.5f), 6f)

@Composable
fun AppIcon(
    icon: ImageBitmap?,
    label: String,
    iconSize: Dp,
    shape: IconShape,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    labelColor: Color = Color.White,
    labelSize: Float = 12f,
    maxLines: Int = 1,
    shadow: Boolean = true,
    /** Plakietka rysowana na ikonie (np. znaczek folderu w rogu). */
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "press")
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(iconSize)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            if (icon != null) {
                val s = iconShapeOf(shape)
                Image(
                    bitmap = icon,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize().then(if (s != null) Modifier.clip(s) else Modifier),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.Medium,
                )
            } else {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.15f)))
            }
            badge?.invoke(this)
        }
        if (showLabel) {
            Text(
                label,
                color = labelColor,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontSize = labelSize.sp,
                    lineHeight = (labelSize + 2).sp,
                    fontWeight = FontWeight.Medium,
                    shadow = if (shadow) LabelShadow else null,
                ),
                modifier = Modifier.padding(top = 5.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

@Composable
fun FolderIcon(
    icons: List<ImageBitmap?>,
    label: String,
    iconSize: Dp,
    shape: IconShape,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    highlight: Boolean = false,
    labelSize: Float = 12f,
) {
    val scale by animateFloatAsState(
        if (pressed) 0.88f else if (highlight) 1.12f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 600f), label = "folder",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .size(iconSize)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(30))
                // Subtelne tło bez obramowania – widać głównie cztery miniatury 2×2
                .background(Color.White.copy(alpha = if (highlight) 0.3f else 0.12f))
                .padding(iconSize * 0.1f),
        ) {
            val mini = (iconSize * 0.8f - 4.dp) / 2
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until 2) {
                    Row(Modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0 until 2) {
                            val ic = icons.getOrNull(r * 2 + c)
                            Box(Modifier.size(mini).padding(1.dp)) {
                                if (ic != null) {
                                    val s = iconShapeOf(shape)
                                    Image(
                                        ic, null,
                                        Modifier.fillMaxSize().then(if (s != null) Modifier.clip(s) else Modifier),
                                        filterQuality = FilterQuality.Medium,
                                    )
                                }
                            }
                            if (c == 0) Spacer(Modifier.size(2.dp))
                        }
                    }
                }
            }
        }
        if (showLabel) {
            Text(
                label.ifBlank { "Folder" },
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(fontSize = labelSize.sp, fontWeight = FontWeight.Medium, shadow = LabelShadow),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

/** Rysowanie ikony bezpośrednio na Canvasie (widoki 3D). */
fun DrawScope.drawAppIcon(img: ImageBitmap, center: Offset, sizePx: Float, alpha: Float, shape: IconShape) {
    val s = sizePx.roundToInt().coerceAtLeast(1)
    val tl = IntOffset((center.x - sizePx / 2).roundToInt(), (center.y - sizePx / 2).roundToInt())
    val shapeObj = iconShapeOf(shape)
    if (shapeObj == null) {
        drawImage(
            img, srcSize = IntSize(img.width, img.height), dstOffset = tl, dstSize = IntSize(s, s),
            alpha = alpha.coerceIn(0f, 1f), filterQuality = FilterQuality.Low,
        )
    } else {
        val outline = shapeObj.createOutline(Size(s.toFloat(), s.toFloat()), layoutDirection, this)
        val path = Path().apply {
            addOutline(outline)
            translate(Offset(tl.x.toFloat(), tl.y.toFloat()))
        }
        clipPath(path) {
            drawImage(
                img, srcSize = IntSize(img.width, img.height), dstOffset = tl, dstSize = IntSize(s, s),
                alpha = alpha.coerceIn(0f, 1f), filterQuality = FilterQuality.Low,
            )
        }
    }
}

/** Rozmiar ikony mieszczący się w komórce o podanych wymiarach. */
fun iconSizeFor(cellW: Dp, cellH: Dp, scale: Float, withLabel: Boolean, desktopMode: Boolean = false): Dp {
    // Ikony wypełniają komórkę: na pulpicie do pełnej szerokości/wysokości (bez podpisu),
    // w szufladzie i folderach z niewielkim marginesem
    val labelSpace = if (withLabel) 20.dp else 0.dp
    val base = minOf(cellW, cellH - labelSpace)
    val baseScale = if (desktopMode) 1f else 0.84f
    return (base * baseScale * scale).coerceAtMost(base).coerceIn(24.dp, 160.dp)
}
