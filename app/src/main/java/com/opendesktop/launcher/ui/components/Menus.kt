package com.opendesktop.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import com.opendesktop.launcher.ui.theme.Palette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.IconShape
import com.opendesktop.launcher.data.SystemAction
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

data class MenuAction(
    val label: String,
    val icon: ImageVector,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

data class MenuShortcut(val label: String, val icon: ImageBitmap?, val onClick: () -> Unit)

/**
 * Wygląd paneli w stylu okienek folderów: kolor z kryciem z ustawień, małe zaokrąglenie,
 * jasny obrys. Tekst i ikony ciemne na jasnym, kryjącym tle – w pozostałych przypadkach białe.
 */
@Immutable
data class PanelStyle(val background: Color) {
    val content: Color get() = if (background.alpha >= 0.35f && background.copy(alpha = 1f).luminance() > 0.5f) PanelInk else Color.White
    val light: Boolean get() = content == PanelInk
    val border: Color get() = Color.White.copy(alpha = if (light) 0.6f else 0.18f)
    val shape: Shape get() = RoundedCornerShape(6.dp)

    companion object {
        fun of(color: Long, opacity: Float) = PanelStyle(Color(color or 0xFF000000).copy(alpha = opacity.coerceIn(0f, 1f)))
    }
}

val PanelInk = Color(0xFF202124)

private val CompactItem = Modifier.height(36.dp)
private val CompactPadding = PaddingValues(horizontal = 10.dp)

/** Styl menu kontekstowych – dostarczany przez motyw z ustawień. */
val LocalMenuPanel = staticCompositionLocalOf { PanelStyle(Color.White.copy(alpha = 0.74f)) }

/**
 * Styl nagłówków w całej aplikacji (menu, grupy ustawień, okna): półgruby, zwykła wielkość liter,
 * bez rozstrzelenia – spokojniejszy niż dawne wersaliki.
 */
val HeaderTextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp)

/**
 * Panel menu w stylu okienek folderów (kolor i krycie z ustawień). Wspólny dla wszystkich menu
 * kontekstowych i rozwijanych: zwarte, z maksymalną wysokością i przewijaniem.
 */
@Composable
fun PanelMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val panel = LocalMenuPanel.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = panel.shape,
        containerColor = panel.background,
        tonalElevation = 0.dp,
        // Cień prześwituje przez półprzezroczyste tło – tylko przy prawie kryjącym
        shadowElevation = if (panel.background.alpha > 0.95f) 8.dp else 0.dp,
        border = BorderStroke(1.dp, panel.border),
        modifier = Modifier.heightIn(max = 340.dp).widthIn(min = 180.dp, max = 260.dp),
    ) {
        if (title != null) PanelMenuHeader(title)
        content()
    }
}

/** Wyśrodkowany nagłówek menu (tytuł albo nazwa sekcji). */
@Composable
fun PanelMenuHeader(text: String) {
    Text(
        text,
        color = LocalMenuPanel.current.content.copy(alpha = 0.6f),
        style = HeaderTextStyle,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
    )
}

@Composable
fun PanelMenuDivider() {
    HorizontalDivider(Modifier.padding(vertical = 2.dp, horizontal = 10.dp), color = LocalMenuPanel.current.content.copy(alpha = 0.12f))
}

/**
 * Wiersz menu: ikona w kółku (albo znacznik wyboru), etykieta i opcjonalny opis.
 * [checked] = true/false pokazuje znacznik zamiast ikony (listy wyboru), null – zwykłą ikonę.
 */
@Composable
fun PanelMenuItem(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    danger: Boolean = false,
    checked: Boolean? = null,
    subtitle: String? = null,
) {
    val panel = LocalMenuPanel.current
    val color = if (danger) (if (panel.light) Color(0xFFD32F2F) else Palette.Danger) else panel.content
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    label, color = color, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (checked == true) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (subtitle != null) Text(subtitle, color = color.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2)
            }
        },
        leadingIcon = {
            val ic = if (checked != null) (if (checked) Icons.Rounded.Check else null) else icon
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(if (ic != null) color.copy(alpha = 0.14f) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) { if (ic != null) Icon(ic, null, tint = color, modifier = Modifier.size(14.dp)) }
        },
        onClick = onClick,
        modifier = if (subtitle != null) Modifier.heightIn(min = 44.dp) else CompactItem,
        contentPadding = CompactPadding,
    )
}

/** Menu kontekstowe kotwiczone w rodzicu (ikona, widżet, folder). */
@Composable
fun ActionMenu(
    title: String?,
    onDismiss: () -> Unit,
    actions: List<MenuAction>,
    shortcuts: List<MenuShortcut> = emptyList(),
) {
    val fg = LocalMenuPanel.current.content
    PanelMenu(expanded = true, onDismiss = onDismiss, title = title) {
        if (shortcuts.isNotEmpty()) {
            shortcuts.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.label, color = fg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        if (s.icon != null) Image(s.icon, null, Modifier.size(22.dp).clip(CircleShape))
                        else Box(Modifier.size(22.dp))
                    },
                    onClick = { onDismiss(); s.onClick() },
                    modifier = CompactItem,
                    contentPadding = CompactPadding,
                )
            }
            PanelMenuDivider()
        }
        actions.forEach { a ->
            PanelMenuItem(a.label, a.icon, onClick = { onDismiss(); a.onClick() }, danger = a.danger)
        }
    }
}

@Composable
private fun PickerHeader(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        style = HeaderTextStyle,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    label: String = "Nazwa",
    confirm: String = "Zapisz",
    secondField: Pair<String, String>? = null,
    extra: (@Composable (setText: (String) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    var second by remember { mutableStateOf(secondField?.second.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (secondField != null) {
                    OutlinedTextField(
                        second, { second = it.take(4) }, label = { Text(secondField.first) },
                        singleLine = true, modifier = Modifier.width(120.dp),
                    )
                }
                OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                extra?.invoke { text = it }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text, second) }, enabled = text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    descriptions: List<String>? = null,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(options.size) { i ->
                    val sel = i == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { onSelect(i); onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(options[i], fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                            descriptions?.getOrNull(i)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (sel) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}

/** Wybór wielu aplikacji z wyszukiwarką (foldery, dok, kategorie). */
@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppInfo>,
    labelOf: (AppInfo) -> String,
    initial: Set<String>,
    shape: IconShape,
    single: Boolean = false,
    /** Skróty akcji do wyboru obok aplikacji (np. w doku); wybrane wracają jako [SystemAction.dockKey]. */
    actions: List<SystemAction> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(initial) }
    val filtered = remember(query, apps) {
        apps.filter { query.isBlank() || labelOf(it).contains(query, true) || it.packageName.contains(query, true) }
            .sortedBy { labelOf(it).lowercase() }
    }
    val filteredActions = remember(query, actions) { actions.filter { query.isBlank() || it.label.contains(query, true) } }

    @Composable
    fun Pickable(key: String, content: @Composable () -> Unit) {
        val sel = key in chosen
        Box(
            Modifier
                .padding(3.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                .then(if (sel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
                .clickable { if (single) onConfirm(setOf(key)) else chosen = if (sel) chosen - key else chosen + key }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    query, { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    placeholder = { Text("Szukaj") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                LazyVerticalGrid(
                    GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    if (filteredActions.isNotEmpty()) {
                        item(key = "h_actions", span = { GridItemSpan(maxLineSpan) }) { PickerHeader("Akcje") }
                        items(filteredActions, key = { it.dockKey }) { a ->
                            Pickable(a.dockKey) {
                                ActionIcon(a, a.label, 40.dp, showLabel = true, labelSize = 11f, pressed = false)
                            }
                        }
                        item(key = "h_apps", span = { GridItemSpan(maxLineSpan) }) { PickerHeader("Aplikacje") }
                    }
                    items(filtered, key = { it.key }) { app ->
                        Pickable(app.key) {
                            AppIcon(app.icon, labelOf(app), 40.dp, shape, true, labelColor = MaterialTheme.colorScheme.onSurface, labelSize = 11f, shadow = false)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!single) TextButton(onClick = { onConfirm(chosen) }) { Text("Zapisz (${chosen.size})") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
