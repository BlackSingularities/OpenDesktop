package com.opendesktop.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opendesktop.launcher.data.AiModel
import com.opendesktop.launcher.data.Config
import com.opendesktop.launcher.data.SystemActions
import com.opendesktop.launcher.data.Updater
import com.opendesktop.launcher.ui.LauncherViewModel
import com.opendesktop.launcher.ui.UpdateState

private enum class ModelSort(val label: String) { Name("Nazwa"), Price("Cena"), Context("Kontekst") }

private fun price(v: Double?): String = when {
    v == null -> "?"
    v == 0.0 -> "0"
    v < 0.01 -> "<0,01"
    else -> "%.2f".format(v)
}

/** Lista wszystkich modeli z OpenRouter z wyszukiwaniem, filtrem darmowych i sortowaniem. */
@Composable
fun ModelPickerDialog(vm: LauncherViewModel, onDismiss: () -> Unit) {
    val models by vm.models.collectAsStateWithLifecycle()
    val loading by vm.modelsLoading.collectAsStateWithLifecycle()
    val error by vm.modelsError.collectAsStateWithLifecycle()
    val cfg by vm.config.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var freeOnly by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(ModelSort.Name) }
    LaunchedEffect(Unit) { vm.loadModels() }
    val filtered = remember(models, query, freeOnly, sort) {
        val q = query.trim()
        models.filter { m -> (!freeOnly || m.isFree) && (q.isEmpty() || m.id.contains(q, true) || m.name.contains(q, true)) }
            .let { list ->
                when (sort) {
                    ModelSort.Name -> list
                    ModelSort.Price -> list.sortedBy { (it.promptPerMillion ?: 1e9) + (it.completionPerMillion ?: 1e9) }
                    ModelSort.Context -> list.sortedByDescending { it.contextLength ?: 0 }
                }
            }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Model AI", Modifier.weight(1f))
                IconButton(onClick = { vm.loadModels(force = true) }) { Icon(Icons.Rounded.Refresh, "Odśwież") }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    placeholder = { Text("Szukaj (np. claude, gpt, llama, free)") },
                )
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(freeOnly, { freeOnly = !freeOnly }, label = { Text("Darmowe") })
                    ModelSort.entries.forEach { s -> FilterChip(sort == s, { sort = s }, label = { Text(s.label) }) }
                }
                Text("${filtered.size} z ${models.size} modeli · ceny w USD za 1 mln tokenów (wejście/wyjście)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(6.dp))
                when {
                    loading && models.isEmpty() -> Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text("Pobieram listę modeli…")
                    }
                    error != null && models.isEmpty() -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    else -> LazyColumn(Modifier.heightIn(max = 440.dp)) {
                        items(filtered, key = { it.id }) { m -> ModelRow(m, m.id == cfg.aiModel) {
                            vm.updateConfig { it.copy(aiModel = m.id, aiModelName = m.name.ifBlank { m.id }) }
                            onDismiss()
                        } }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}

@Composable
private fun ModelRow(m: AiModel, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(m.name.ifBlank { m.id }, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(m.id, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    if (m.isFree) append("darmowy") else append("$${price(m.promptPerMillion)} / $${price(m.completionPerMillion)}")
                    m.contextLength?.let { append(" · ${it / 1000}k kontekstu") }
                },
                fontSize = 11.sp,
                color = if (m.isFree) Color(0xFF7EE0C3) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Wyszukiwanie repozytorium z wydaniami na GitHubie – źródło aktualizacji. */
@Composable
fun RepoSearchDialog(vm: LauncherViewModel, onDismiss: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val hits by vm.repoSearch.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("OpenDesktop launcher") }
    var manual by remember { mutableStateOf(cfg.updateRepo) }
    LaunchedEffect(Unit) { vm.searchRepos(query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Źródło aktualizacji") },
        text = {
            Column {
                OutlinedTextField(
                    manual, { manual = it.trim() }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("właściciel/repozytorium") },
                )
                TextButton(onClick = { manual = Config.DEFAULT_REPO }) { Text("Domyślne: ${Config.DEFAULT_REPO}") }
                OutlinedTextField(
                    query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Szukaj na GitHubie") },
                    trailingIcon = { IconButton(onClick = { vm.searchRepos(query) }) { Icon(Icons.Rounded.Search, "Szukaj") } },
                )
                LazyColumn(Modifier.heightIn(max = 300.dp).padding(top = 6.dp)) {
                    items(hits, key = { it.fullName }) { h ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (h.fullName == manual) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                                .clickable { manual = h.fullName }
                                .padding(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(h.fullName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Rounded.Star, null, Modifier.size(14.dp))
                                Text(" ${h.stars}", fontSize = 12.sp)
                            }
                            if (h.description.isNotBlank()) Text(h.description, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (manual.contains('/')) vm.updateConfig { it.copy(updateRepo = manual) }
                onDismiss()
                vm.checkUpdates()
            }) { Text("Zapisz i sprawdź") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

/**
 * Dialog aktualizacji. Przy `onlyAuto` pokazuje się sam, gdy automatyczne sprawdzenie znajdzie
 * nowe wydanie; w ustawieniach pokazuje też stan „aktualna wersja” i błędy.
 */
@Composable
fun UpdateDialog(vm: LauncherViewModel, state: UpdateState, onlyAuto: Boolean, onClosed: () -> Unit = {}) {
    val ctx = LocalContext.current
    val close = { vm.dismissUpdate(); onClosed() }
    when (state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = close,
            title = { Text("Nowa wersja ${state.info.versionName}") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (state.info.sizeBytes > 0) Text("Rozmiar: ${"%.1f".format(state.info.sizeBytes / 1_048_576.0)} MB", fontSize = 13.sp)
                    Spacer(Modifier.size(6.dp))
                    Text(state.info.notes.ifBlank { "Bez opisu zmian." }, fontSize = 13.sp)
                }
            },
            confirmButton = { TextButton(onClick = { vm.downloadUpdate(state.info) }) { Text("Pobierz i zainstaluj") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.skipUpdate(state.info); onClosed() }) { Text("Pomiń tę wersję") }
                    TextButton(onClick = close) { Text("Później") }
                }
            },
        )
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Pobieranie ${state.info.versionName}") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(state.progress * 100).toInt()}%", modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {},
        )
        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = close,
            title = { Text("Gotowe do instalacji") },
            text = {
                Text(
                    if (Updater.canInstall(ctx)) "Instalator systemowy zaktualizuje OpenDesktop. Układ pulpitu zostanie zachowany."
                    else "Zezwól OpenDesktop na instalowanie aplikacji, a następnie wróć i dotknij „Instaluj”.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (Updater.canInstall(ctx)) {
                        vm.store.saveNow()
                        Updater.install(ctx, state.file)
                    } else Updater.openInstallPermission(ctx)
                }) { Text(if (Updater.canInstall(ctx)) "Instaluj" else "Zezwól") }
            },
            dismissButton = { TextButton(onClick = close) { Text("Później") } },
        )
        is UpdateState.UpToDate -> if (!onlyAuto) AlertDialog(
            onDismissRequest = close,
            title = { Text("Brak aktualizacji") },
            text = { Text("Masz najnowszą wersję OpenDesktop.") },
            confirmButton = { TextButton(onClick = close) { Text("OK") } },
        )
        is UpdateState.Error -> if (!onlyAuto) AlertDialog(
            onDismissRequest = close,
            title = { Text("Nie udało się sprawdzić") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = close) { Text("OK") } },
            dismissButton = { TextButton(onClick = { SystemActions.openUrl(ctx, "https://github.com/${vm.config.value.updateRepo}/releases") }) { Text("Otwórz GitHub") } },
        )
        else -> Unit
    }
}
