package com.opendesktop.launcher.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.IconShape

data class MenuAction(
    val label: String,
    val icon: ImageVector,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

data class MenuShortcut(val label: String, val icon: ImageBitmap?, val onClick: () -> Unit)

/** Menu kontekstowe kotwiczone w rodzicu (ikona, widżet, folder). */
@Composable
fun ActionMenu(
    title: String?,
    onDismiss: () -> Unit,
    actions: List<MenuAction>,
    shortcuts: List<MenuShortcut> = emptyList(),
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).width(220.dp),
            )
        }
        if (shortcuts.isNotEmpty()) {
            shortcuts.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        if (s.icon != null) Image(s.icon, null, Modifier.size(24.dp).clip(CircleShape))
                        else Box(Modifier.size(24.dp))
                    },
                    onClick = { onDismiss(); s.onClick() },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
        }
        actions.forEach { a ->
            val color = if (a.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            DropdownMenuItem(
                text = { Text(a.label, color = color) },
                leadingIcon = { Icon(a.icon, null, tint = color) },
                onClick = { onDismiss(); a.onClick() },
            )
        }
    }
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
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(initial) }
    val filtered = remember(query, apps) {
        apps.filter { query.isBlank() || labelOf(it).contains(query, true) || it.packageName.contains(query, true) }
            .sortedBy { labelOf(it).lowercase() }
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
                    items(filtered, key = { it.key }) { app ->
                        val sel = app.key in chosen
                        Box(
                            Modifier
                                .padding(3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                                .then(
                                    if (sel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable {
                                    if (single) {
                                        onConfirm(setOf(app.key))
                                    } else {
                                        chosen = if (sel) chosen - app.key else chosen + app.key
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
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
