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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.opendesktop.launcher.data.DesktopItem
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

@Composable
fun SearchWidget(modifier: Modifier, aiEnabled: Boolean) {
    Box(modifier.padding(horizontal = 6.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.2f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text("Szukaj aplikacji…", color = Color.White.copy(alpha = 0.85f), modifier = Modifier.weight(1f), maxLines = 1)
            if (aiEnabled) Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
        }
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
