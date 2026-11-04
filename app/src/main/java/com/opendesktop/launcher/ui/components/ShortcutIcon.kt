package com.opendesktop.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.opendesktop.launcher.data.ShortcutIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ikona skrótu z pliku PNG. Raz wczytana zostaje w pamięci, więc przy przewijaniu ekranów
 * i przeciąganiu nic nie czyta z dysku.
 */
@Composable
fun rememberShortcutIcon(file: String?): ImageBitmap? {
    val ctx = LocalContext.current
    return produceState(ShortcutIcons.cached(file), file) {
        if (value == null && file != null) {
            value = withContext(Dispatchers.IO) { ShortcutIcons.load(ctx, file) }
        }
    }.value
}
