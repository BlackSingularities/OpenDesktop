package com.opendesktop.launcher.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opendesktop.launcher.data.AppInfo

/** Liczba powiadomień na pakiet, dostępna w całym drzewie UI (pusta = plakietki wyłączone). */
val LocalBadgeCounts = compositionLocalOf { emptyMap<String, Int>() }

/** Kolor plakietki – czytelny na każdej tapecie. */
private val BadgeRed = Color(0xFFE5484D)

@Composable
fun badgeCount(app: AppInfo?): Int = app?.let { LocalBadgeCounts.current[it.packageName] ?: 0 } ?: 0

/** Suma powiadomień aplikacji w folderze. */
@Composable
fun badgeCount(apps: List<AppInfo?>): Int {
    val counts = LocalBadgeCounts.current
    return apps.sumOf { a -> a?.let { counts[it.packageName] } ?: 0 }
}

fun badgeText(count: Int): String = if (count > 99) "99+" else count.toString()

/**
 * Plakietka z liczbą powiadomień w prawym górnym rogu ikony.
 * Rozmiar skaluje się z ikoną, przy kilku cyfrach rozsuwa się w pigułkę.
 */
@Composable
fun NotificationBadge(count: Int, iconSize: Dp, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val text = badgeText(count)
    val d = (iconSize * 0.36f).coerceIn(14.dp, 26.dp)
    val fontSize = with(LocalDensity.current) { (d * 0.56f).toSp() }
    Box(
        modifier
            .offset(x = d * 0.16f, y = -d * 0.16f)
            .defaultMinSize(minWidth = d, minHeight = d)
            .clip(RoundedCornerShape(50))
            .background(BadgeRed)
            .border(d * 0.08f, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = if (text.length > 1) d * 0.22f else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = fontSize,
            lineHeight = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    textAlign = Paint.Align.CENTER
}

/** Plakietka rysowana wprost na Canvasie (układy 3D szuflady). */
fun DrawScope.drawBadge(count: Int, iconCenter: Offset, iconPx: Float, alpha: Float) {
    if (count <= 0 || iconPx <= 0f) return
    val text = badgeText(count)
    val r = iconPx * 0.2f
    val extra = if (text.length > 1) r * (text.length - 1) * 0.5f else 0f
    val cx = iconCenter.x + iconPx / 2 - r * 0.5f
    val cy = iconCenter.y - iconPx / 2 + r * 0.5f
    val a = alpha.coerceIn(0f, 1f)
    drawRoundRect(
        color = BadgeRed.copy(alpha = a),
        topLeft = Offset(cx - r - extra, cy - r),
        size = Size(2 * (r + extra), 2 * r),
        cornerRadius = CornerRadius(r, r),
    )
    badgePaint.textSize = r * 1.25f
    badgePaint.color = Color.White.copy(alpha = a).toArgb()
    drawContext.canvas.nativeCanvas.drawText(text, cx, cy + r * 0.45f, badgePaint)
}
