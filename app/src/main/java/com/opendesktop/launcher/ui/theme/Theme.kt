package com.opendesktop.launcher.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.opendesktop.launcher.data.Config

@Composable
fun OpenDesktopTheme(cfg: Config, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val accent = Color(cfg.accent)
    val scheme = if (cfg.dynamicColor && Build.VERSION.SDK_INT >= 31) {
        dynamicDarkColorScheme(ctx)
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF0B1020),
            primaryContainer = accent.copy(alpha = 0.28f),
            onPrimaryContainer = Color.White,
            secondary = accent,
            secondaryContainer = Color(0xFF2A3245),
            onSecondaryContainer = Color.White,
            surface = Color(0xFF121520),
            surfaceContainer = Color(0xFF1A1E2B),
            surfaceContainerHigh = Color(0xFF222737),
            surfaceContainerHighest = Color(0xFF2A3042),
            background = Color(0xFF0D1018),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
