package com.opendesktop.launcher.ui.desktop

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.SearchStyle
import com.opendesktop.launcher.ui.components.LabelShadow
import com.opendesktop.launcher.widget.LauncherWidgetHost
import com.opendesktop.launcher.widget.LauncherWidgetHostView
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ClockWidget(modifier: Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val ctx = LocalContext.current
    var battery by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            if (battery < 0 || now.second % 30 == 0) battery = batteryLevel(ctx)
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    val locale = Locale.getDefault()
    val time = remember(now.hour, now.minute) { now.format(DateTimeFormatter.ofPattern("HH:mm", locale)) }
    val date = remember(now.dayOfYear) {
        now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val big = (maxHeight.value * 0.48f).coerceIn(30f, 104f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                time,
                color = Color.White,
                style = TextStyle(fontSize = big.sp, fontWeight = FontWeight.Light, shadow = LabelShadow, letterSpacing = (-1).sp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(date, color = Color.White.copy(alpha = 0.92f), style = TextStyle(fontSize = (big * 0.2f).coerceIn(12f, 20f).sp, shadow = LabelShadow))
                if (battery >= 0) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "· $battery%",
                        color = Color.White.copy(alpha = 0.75f),
                        style = TextStyle(fontSize = (big * 0.2f).coerceIn(12f, 20f).sp, shadow = LabelShadow),
                    )
                }
            }
        }
    }
}

private fun batteryLevel(ctx: Context): Int = runCatching {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    if (level < 0) -1 else level * 100 / scale
}.getOrDefault(-1)

/**
 * Wyszukiwarka na pulpicie. Wygląd z [SearchStyle]: zaokrąglenie, tło, kolor tekstu, obrys
 * i cienie. Ikona i tekst rosną razem z wysokością pola.
 */
@Composable
fun SearchWidget(modifier: Modifier, aiEnabled: Boolean, style: SearchStyle = SearchStyle()) {
    BoxWithConstraints(modifier.padding(horizontal = 6.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        // Pole nie wyższe niż ~64 dp, nawet gdy widżet zajmuje kilka wierszy
        val h = maxHeight.coerceAtMost(64.dp).coerceAtLeast(36.dp)
        val shape = RoundedCornerShape(h * (style.radius.coerceIn(0, 50) / 100f))
        val fg = Color(style.textColor)
        val textShadow = if (style.textShadow) {
            Shadow(if (fg.luminance() > 0.5f) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.6f), Offset(0f, 1.5f), 6f)
        } else null
        val iconSize = (h.value * 0.42f).coerceIn(18f, 28f).dp
        Row(
            Modifier
                .fillMaxWidth()
                .height(h)
                .then(
                    if (style.shadow > 0.01f) Modifier.shadow(
                        elevation = (style.shadow * 18f).dp,
                        shape = shape,
                        ambientColor = Color.Black,
                        spotColor = Color.Black,
                    ) else Modifier
                )
                .clip(shape)
                .background(Color(style.background))
                .then(if (style.border) Modifier.border(1.dp, fg.copy(alpha = 0.25f), shape) else Modifier)
                .padding(horizontal = (h.value * 0.36f).coerceIn(12f, 22f).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShadowedIcon(Icons.Rounded.Search, fg, iconSize, style.textShadow)
            Spacer(Modifier.width(10.dp))
            Text(
                style.hint.ifBlank { "Szukaj aplikacji…" },
                color = fg.copy(alpha = 0.88f),
                style = TextStyle(fontSize = (h.value * 0.3f).coerceIn(13f, 18f).sp, shadow = textShadow),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (aiEnabled) ShadowedIcon(Icons.Rounded.AutoAwesome, fg.copy(alpha = 0.88f), iconSize * 0.8f, style.textShadow)
        }
    }
}

/** Ikona z miękkim cieniem pod spodem (ta sama ikona, przesunięta i przyciemniona). */
@Composable
private fun ShadowedIcon(icon: ImageVector, tint: Color, size: Dp, shadow: Boolean) {
    Box {
        if (shadow) Icon(
            icon, null,
            tint = if (tint.luminance() > 0.5f) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(size).offset(y = 1.dp).blur(2.dp),
        )
        Icon(icon, null, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
fun SystemWidget(
    host: LauncherWidgetHost,
    item: DesktopItem,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val info = remember(item.widgetId) { runCatching { AppWidgetManager.getInstance(context).getAppWidgetInfo(item.widgetId) }.getOrNull() }
    if (info == null) {
        Box(
            modifier.padding(4.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Widgets, null, tint = Color.White.copy(alpha = 0.7f))
                Text("Widżet niedostępny", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        return
    }
    BoxWithConstraints(modifier) {
        val wDp = maxWidth.value
        val hDp = maxHeight.value
        AndroidView(
            factory = { ctx ->
                host.createView(ctx, item.widgetId, info).apply {
                    setPadding(0, 0, 0, 0)
                }
            },
            update = { v ->
                (v as? LauncherWidgetHostView)?.let {
                    it.onLongPress = onLongPress
                    it.stretch = item.stretch
                    it.updateSize(wDp, hDp)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
