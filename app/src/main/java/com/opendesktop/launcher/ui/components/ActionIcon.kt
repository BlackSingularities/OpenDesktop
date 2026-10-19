package com.opendesktop.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.SystemAction

val SystemAction.icon: ImageVector
    get() = when (this) {
        SystemAction.OpenDrawer -> Icons.Rounded.Apps
        SystemAction.OpenSearch -> Icons.Rounded.Search
        SystemAction.OpenNotifications -> Icons.Rounded.Notifications
        SystemAction.OpenQuickSettings -> Icons.Rounded.Tune
        SystemAction.OpenSettings -> Icons.Rounded.Settings
    }

/** Ikona skrótu akcji systemowej (pulpit i dok). */
@Composable
fun ActionIcon(
    action: SystemAction?,
    label: String,
    iconSize: Dp,
    showLabel: Boolean,
    labelSize: Float,
    pressed: Boolean,
    modifier: Modifier = Modifier,
) {
    val a = action ?: SystemAction.OpenSettings
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        if (a == SystemAction.OpenDrawer) {
            // Klasyczny przycisk szuflady: biała kula z sześcioma kropkami (2 × 3)
            Canvas(Modifier.size(iconSize)) {
                // 0.84 – ikony aplikacji mają wewnętrzny margines, pełne koło wyglądałoby na większe
                val s = if (pressed) 0.76f else 0.84f
                val r = size.minDimension / 2 * s
                drawCircle(Color.White, r)
                val dot = r * 0.11f
                val gap = r * 0.34f
                for (row in 0..1) for (col in -1..1) {
                    drawCircle(Color(0xFF202124), dot, center + Offset(col * gap, (row - 0.5f) * gap))
                }
            }
        } else {
            Box(
                Modifier.size(iconSize).padding(iconSize * 0.08f).clip(CircleShape).background(Color.White.copy(alpha = if (pressed) 0.32f else 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(a.icon, contentDescription = label.ifBlank { a.label }, tint = Color.White, modifier = Modifier.size(iconSize * 0.5f))
            }
        }
        if (showLabel) {
            Text(
                label.ifBlank { a.label },
                color = Color.White,
                fontSize = labelSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}
