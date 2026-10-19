package com.opendesktop.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.opendesktop.launcher.ui.theme.Palette

// Kontrolki ustawień przeniesione ze stylu OpenPDF: czarne tło, grupy na bieli 6%, biały akcent.

/** Pełnoekranowa strona ustawień: przycisk wstecz, tytuł, przewijana treść. */
@Composable
fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Palette.Panel).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Palette.PanelHigh).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Nagłówek sekcji: ikona, nazwa i cienka linia do krawędzi. */
@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
    }
}

/** Grupa wierszy na wspólnym zaokrąglonym tle; opcjonalny tytuł wersalikami. */
@Composable
fun Group(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        if (title != null) {
            Text(
                title.uppercase(),
                color = Palette.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 3.dp, top = 4.dp),
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Group).padding(vertical = 2.dp),
            content = content,
        )
    }
}

@Composable
fun RowIcon(icon: ImageVector, tint: Color = Palette.Accent) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SwitchRow(icon: ImageVector, title: String, subtitle: String? = null, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!value) }.padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontSize = 14.sp, color = if (enabled) Color.White else Palette.TextFaint)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Palette.TextDim, lineHeight = 14.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.scale(0.82f),
            colors = SwitchDefaults.colors(
                checkedTrackColor = Palette.Accent,
                checkedThumbColor = Palette.Ink,
                uncheckedTrackColor = Palette.PanelHigh,
                uncheckedThumbColor = Palette.TextDim,
                uncheckedBorderColor = Color.White.copy(alpha = 0.2f),
            ),
        )
    }
}

/** Wiersz-akcja: ikona, tytuł, opis; dotknięcie wykonuje akcję. [chevron] dla przejścia do podstrony. */
@Composable
fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    chevron: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, if (danger) Palette.Danger else Palette.Accent)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                title,
                fontSize = 14.sp,
                color = when {
                    !enabled -> Palette.TextFaint
                    danger -> Palette.Danger
                    else -> Color.White
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Palette.TextDim, lineHeight = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.TextFaint)
    }
}

/** Suwak z nazwą i wartością; zapisuje dopiero po puszczeniu. */
@Composable
fun SliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String = { it.toInt().toString() },
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Text(
                format(local),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.Ink,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Palette.Accent).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
            steps = steps,
            modifier = Modifier.padding(start = 40.dp),
            colors = SliderDefaults.colors(
                thumbColor = Palette.Accent,
                activeTrackColor = Palette.Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                activeTickColor = Palette.Ink.copy(alpha = 0.4f),
                inactiveTickColor = Color.White.copy(alpha = 0.3f),
            ),
        )
    }
}

/** Kafelek-przełącznik: biały, gdy włączony. */
class Tile(val icon: ImageVector, val label: String, val value: Boolean, val onChange: (Boolean) -> Unit)

@Composable
fun TileGrid(tiles: List<Tile>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (t.value) Palette.Accent else Palette.Group)
                            .clickable { t.onChange(!t.value) }
                            .padding(vertical = 10.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(t.icon, null, tint = if (t.value) Palette.Ink else Palette.TextDim, modifier = Modifier.size(20.dp))
                        Text(
                            t.label,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            color = if (t.value) Palette.Ink else Palette.TextDim,
                            fontWeight = if (t.value) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Przycisk-pigułka: biały (główny), półprzezroczysty albo czerwony. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, primary: Boolean = true, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = when {
        !enabled -> Color.White.copy(alpha = 0.08f)
        danger -> Palette.Danger
        primary -> Palette.Accent
        else -> Color.White.copy(alpha = 0.1f)
    }
    val fg = when {
        !enabled -> Palette.TextFaint
        danger -> Color.White
        primary -> Palette.Ink
        else -> Color.White
    }
    Box(
        modifier.clip(RoundedCornerShape(50)).background(bg).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1) }
}

/** Ciemne okno dialogowe w stylu OpenPDF. */
@Composable
fun DarkDialog(title: String, onDismiss: () -> Unit, buttons: @Composable RowScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(20.dp).widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.PanelHigh).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically, content = buttons)
        }
    }
}
