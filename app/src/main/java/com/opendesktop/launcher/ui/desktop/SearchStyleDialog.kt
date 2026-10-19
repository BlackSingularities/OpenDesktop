package com.opendesktop.launcher.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.SearchStyle
import com.opendesktop.launcher.ui.settings.DarkDialog
import com.opendesktop.launcher.ui.settings.PillButton
import com.opendesktop.launcher.ui.settings.SliderRow
import com.opendesktop.launcher.ui.settings.Tile
import com.opendesktop.launcher.ui.settings.TileGrid
import com.opendesktop.launcher.ui.theme.Palette

/** Gotowe zestawy – czytelne na różnych tapetach. */
private val presets = listOf(
    "Szkło" to SearchStyle(),
    "Jasny" to SearchStyle(background = 0xF2FFFFFF, textColor = 0xFF202124, shadow = 0.5f, border = false),
    "Ciemny" to SearchStyle(background = 0xB3000000, textColor = 0xFFFFFFFF, shadow = 0.4f, border = false),
    "Kontur" to SearchStyle(background = 0x00000000, textColor = 0xFFFFFFFF, border = true, textShadow = true),
    "Kafel" to SearchStyle(radius = 18, background = 0x59000000, textColor = 0xFFFFFFFF, shadow = 0.3f, border = true, textShadow = true),
)

private val textColors = listOf(0xFFFFFFFF, 0xFF202124, 0xFFFFD54F, 0xFF80DEEA, 0xFFA5D6A7, 0xFFF48FB1, 0xFFB39DDB)
private val backgroundColors = listOf(0xFFFFFFFF, 0xFF000000, 0xFF1E88E5, 0xFF43A047, 0xFFE53935, 0xFF8E24AA, 0xFFFFB300)

/** Wygląd widżetu wyszukiwarki: zestawy, zaokrąglenie, kolory, przezroczystość, cienie i tekst. */
@Composable
fun SearchStyleDialog(initial: SearchStyle, aiEnabled: Boolean, onDismiss: () -> Unit, onSave: (SearchStyle) -> Unit) {
    var st by remember { mutableStateOf(initial) }
    val bg = Color(st.background)
    DarkDialog(
        title = "Wygląd wyszukiwarki",
        onDismiss = onDismiss,
        buttons = {
            PillButton("Anuluj", primary = false, onClick = onDismiss)
            PillButton("Zapisz", onClick = { onSave(st) })
        },
    ) {
        // Podgląd na przejściu od jasnej do ciemnej tapety – od razu widać, czy pole jest czytelne
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFF3EFE6), Color(0xFF7A8FA6), Color(0xFF0E1420))))
                .padding(horizontal = 10.dp, vertical = 14.dp),
        ) {
            SearchWidget(Modifier.fillMaxWidth().height(64.dp), aiEnabled, st)
        }

        Label("Zestawy")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { (name, p) ->
                val sel = p.copy(hint = st.hint) == st
                Text(
                    name,
                    color = if (sel) Palette.Ink else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) Palette.Accent else Color.White.copy(alpha = 0.1f))
                        .clickable { st = p.copy(hint = st.hint) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        SliderRow(Icons.Rounded.RoundedCorner, "Zaokrąglenie rogów", st.radius.toFloat(), 0f..50f, 9, { "${(it / 50f * 100).toInt()}%" }) {
            st = st.copy(radius = it.toInt())
        }

        Label("Kolor tekstu i ikon")
        Swatches(textColors, st.textColor) { st = st.copy(textColor = it) }

        Label("Kolor tła")
        Swatches(backgroundColors, st.background or 0xFF000000) { c ->
            // Zmiana koloru zachowuje bieżącą przezroczystość
            val alpha = (st.background ushr 24) and 0xFF
            st = st.copy(background = (alpha shl 24) or (c and 0xFFFFFF))
        }
        SliderRow(Icons.Rounded.Opacity, "Krycie tła", bg.alpha * 100f, 0f..100f, 19, { "${it.toInt()}%" }) { a ->
            val alpha = (a / 100f * 255f).toLong().coerceIn(0, 255)
            st = st.copy(background = (alpha shl 24) or (st.background and 0xFFFFFF))
        }
        SliderRow(Icons.Rounded.BlurOn, "Cień pod polem", st.shadow * 100f, 0f..100f, 9, { "${it.toInt()}%" }) {
            st = st.copy(shadow = it / 100f)
        }

        TileGrid(
            listOf(
                Tile(Icons.Rounded.Check, "Obrys", st.border) { v -> st = st.copy(border = v) },
                Tile(Icons.Rounded.BlurOn, "Cień tekstu", st.textShadow) { v -> st = st.copy(textShadow = v) },
            )
        )

        OutlinedTextField(
            value = st.hint,
            onValueChange = { st = st.copy(hint = it.take(40)) },
            label = { Text("Tekst w polu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Palette.TextDim,
                cursorColor = Color.White,
            ),
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        color = Palette.TextDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}

@Composable
private fun Swatches(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            val color = Color(c)
            val sel = (selected and 0xFFFFFF) == (c and 0xFFFFFF)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (sel) 3.dp else 1.dp, if (sel) Palette.Accent else Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable { onPick(color.toArgb().toLong() and 0xFFFFFFFF) },
                contentAlignment = Alignment.Center,
            ) {
                if (sel) Icon(Icons.Rounded.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
