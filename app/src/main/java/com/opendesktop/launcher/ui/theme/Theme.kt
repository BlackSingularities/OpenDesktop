package com.opendesktop.launcher.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.opendesktop.launcher.data.Config

/** Czarno-biała paleta jak w OpenPDF: akcent = biel, czerwień dla usuwania. */
object Palette {
    val Accent = Color(0xFFFFFFFF)
    val Danger = Color(0xFFFF3B47)
    val Ink = Color(0xFF000000)
    val Panel = Color(0xFF000000)
    val PanelHigh = Color(0xFF1C1C1C)
    val Group = Color(0x0FFFFFFF)
    val TextDim = Color(0xB3FFFFFF)
    val TextFaint = Color(0x66FFFFFF)
}

@Composable
fun OpenDesktopTheme(cfg: Config, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = if (cfg.dynamicColor && Build.VERSION.SDK_INT >= 31) {
        dynamicDarkColorScheme(ctx)
    } else {
        val accent = Color(cfg.accent)
        darkColorScheme(
            primary = accent,
            onPrimary = Palette.Ink,
            primaryContainer = Color.White.copy(alpha = 0.14f),
            onPrimaryContainer = Color.White,
            secondary = accent,
            onSecondary = Palette.Ink,
            secondaryContainer = Palette.PanelHigh,
            onSecondaryContainer = Color.White,
            background = Palette.Ink,
            onBackground = Color.White,
            surface = Palette.Panel,
            onSurface = Color.White,
            surfaceVariant = Palette.PanelHigh,
            onSurfaceVariant = Palette.TextDim,
            surfaceContainerLowest = Palette.Panel,
            surfaceContainerLow = Palette.Panel,
            surfaceContainer = Palette.PanelHigh,
            surfaceContainerHigh = Palette.PanelHigh,
            surfaceContainerHighest = Palette.PanelHigh,
            outline = Color(0x33FFFFFF),
            outlineVariant = Color(0x1FFFFFFF),
            error = Palette.Danger,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
