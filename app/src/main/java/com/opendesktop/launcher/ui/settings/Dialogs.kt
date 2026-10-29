package com.opendesktop.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import com.opendesktop.launcher.data.AiProvider
import com.opendesktop.launcher.data.Config
import com.opendesktop.launcher.data.SystemActions
import com.opendesktop.launcher.data.Updater
import com.opendesktop.launcher.data.aiModelId
import com.opendesktop.launcher.data.withAiModel
import com.opendesktop.launcher.ui.LauncherViewModel
import com.opendesktop.launcher.ui.UpdateState
import com.opendesktop.launcher.ui.theme.Palette

private enum class ModelSort(val label: String) { Name("Nazwa"), Price("Cena"), Context("Kontekst") }

private fun price(v: Double?): String = when {
    v == null -> "?"
    v == 0.0 -> "0"
    v < 0.01 -> "<0,01"
    else -> "%.2f".format(v)
}

/**
 * Modele wybranego dostawcy z wyszukiwaniem. Ceny, kontekst i filtr darmowych pokazujemy tylko tam,
 * gdzie API je podaje (OpenRouter); u pozostałych lista zawiera same identyfikatory.
 */
@Composable
fun ModelPickerDialog(vm: LauncherViewModel, onDismiss: () -> Unit) {
    val models by vm.models.collectAsStateWithLifecycle()
    val loading by vm.modelsLoading.collectAsStateWithLifecycle()
    val error by vm.modelsError.collectAsStateWithLifecycle()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val provider = cfg.aiProvider
    var query by remember { mutableStateOf("") }
    var freeOnly by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(ModelSort.Name) }
    LaunchedEffect(Unit) { vm.loadModels() }
    val priced = remember(models) { models.any { it.hasPricing } }
    val withContext = remember(models) { models.any { it.contextLength != null } }
    val filtered = remember(models, query, freeOnly, sort, priced) {
        val q = query.trim()
        models.filter { m -> (!freeOnly || !priced || m.isFree) && (q.isEmpty() || m.id.contains(q, true) || m.title.contains(q, true)) }
            .let { list ->
                when (sort) {
                    ModelSort.Name -> list
                    ModelSort.Price -> list.sortedBy { (it.promptPerMillion ?: 1e9) + (it.completionPerMillion ?: 1e9) }
                    ModelSort.Context -> list.sortedByDescending { it.contextLength ?: 0 }
                }
            }
    }
    val current = cfg.aiModelId()
    DarkDialog(
        title = "Model AI",
        subtitle = provider.label,
        icon = Icons.Rounded.Memory,
        onDismiss = onDismiss,
        scrollable = false,
        titleAction = { DialogIconButton(Icons.Rounded.Refresh, "Odśwież", enabled = !loading) { vm.loadModels(force = true) } },
        buttons = { PillButton("Zamknij", primary = false, onClick = onDismiss) },
    ) {
        DialogTextField(
            query, { query = it },
            placeholder = when (provider) {
                AiProvider.OpenRouter -> "Szukaj (claude, gpt, llama, free…)"
                else -> "Szukaj modelu"
            },
            leadingIcon = Icons.Rounded.Search,
        )
        if (priced || withContext) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (priced) DialogChip("Darmowe", freeOnly) { freeOnly = !freeOnly }
                ModelSort.entries
                    .filter { (it != ModelSort.Price || priced) && (it != ModelSort.Context || withContext) }
                    .forEach { s -> DialogChip(s.label, sort == s) { sort = s } }
            }
        }
        if (models.isNotEmpty()) {
            Text(
                "${filtered.size} z ${models.size} modeli" + if (priced) " · USD za 1 mln tokenów (wejście / wyjście)" else "",
                fontSize = 11.sp, color = Palette.TextFaint,
            )
        }
        when {
            loading && models.isEmpty() -> DialogLoading("Pobieram listę modeli z ${provider.label}…")
            error != null && models.isEmpty() -> {
                DialogNotice(error ?: "", error = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    PillButton("Spróbuj ponownie", primary = false) { vm.loadModels(force = true) }
                }
            }
            models.isEmpty() -> DialogNotice("Dostawca nie zwrócił żadnych modeli.")
            filtered.isEmpty() -> DialogNotice("Brak modeli pasujących do „${query.trim()}”.")
            else -> LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.id }) { m ->
                    ModelRow(m, m.id == current, priced) {
                        vm.updateConfig { it.withAiModel(m.id, m.title) }
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(m: AiModel, selected: Boolean, priced: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
            .then(if (selected) Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(m.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (m.title != m.id) Text(m.id, fontSize = 11.sp, color = Palette.TextFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = buildString {
                if (priced) {
                    if (m.isFree) append("darmowy") else append("$${price(m.promptPerMillion)} / $${price(m.completionPerMillion)}")
                }
                m.contextLength?.let { if (isNotEmpty()) append(" · "); append("${it / 1000}k kontekstu") }
                if (isEmpty() && m.ownedBy.isNotBlank()) append(m.ownedBy)
            }
            if (meta.isNotEmpty()) Text(meta, fontSize = 11.sp, color = if (m.isFree) Color(0xFF7EE0C3) else Palette.TextDim)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(24.dp).clip(CircleShape).background(Palette.Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Palette.Ink, modifier = Modifier.size(16.dp))
            }
        }
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
    DarkDialog(
        title = "Źródło aktualizacji",
        icon = Icons.Rounded.Code,
        onDismiss = onDismiss,
        scrollable = false,
        buttons = {
            PillButton("Anuluj", primary = false, onClick = onDismiss)
            PillButton("Zapisz i sprawdź") {
                if (manual.contains('/')) vm.updateConfig { it.copy(updateRepo = manual) }
                onDismiss()
                vm.checkUpdates()
            }
        },
    ) {
        DialogTextField(manual, { manual = it.trim() }, label = "właściciel/repozytorium")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            DialogChip("Domyślne: ${Config.DEFAULT_REPO}", manual == Config.DEFAULT_REPO) { manual = Config.DEFAULT_REPO }
        }
        DialogTextField(
            query, { query = it },
            label = "Szukaj na GitHubie",
            trailing = { DialogIconButton(Icons.Rounded.Search, "Szukaj") { vm.searchRepos(query) } },
        )
        LazyColumn(Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(hits, key = { it.fullName }) { h ->
                val sel = h.fullName == manual
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (sel) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                        .clickable { manual = h.fullName }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.fullName, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = Palette.TextDim)
                        Text(" ${h.stars}", fontSize = 12.sp, color = Palette.TextDim)
                    }
                    if (h.description.isNotBlank()) Text(h.description, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Palette.TextDim)
                }
            }
        }
    }
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
        is UpdateState.Available -> DarkDialog(
            title = "Nowa wersja ${state.info.versionName}",
            subtitle = if (state.info.sizeBytes > 0) "Rozmiar: ${"%.1f".format(state.info.sizeBytes / 1_048_576.0)} MB" else null,
            icon = Icons.Rounded.SystemUpdate,
            onDismiss = close,
            buttons = {
                PillButton("Pomiń", primary = false) { vm.skipUpdate(state.info); onClosed() }
                PillButton("Później", primary = false, onClick = close)
                PillButton("Zainstaluj") { vm.downloadUpdate(state.info) }
            },
        ) {
            Text(state.info.notes.ifBlank { "Bez opisu zmian." }, fontSize = 13.sp, color = Palette.TextDim, lineHeight = 18.sp)
        }
        is UpdateState.Downloading -> DarkDialog(
            title = "Pobieranie ${state.info.versionName}",
            icon = Icons.Rounded.SystemUpdate,
            onDismiss = {},
            dismissible = false,
            buttons = {},
        ) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.12f),
            )
            Text("${(state.progress * 100).toInt()}%", color = Palette.TextDim, fontSize = 13.sp)
        }
        is UpdateState.Ready -> DarkDialog(
            title = "Gotowe do instalacji",
            icon = Icons.Rounded.SystemUpdate,
            onDismiss = close,
            buttons = {
                PillButton("Później", primary = false, onClick = close)
                PillButton(if (Updater.canInstall(ctx)) "Instaluj" else "Zezwól") {
                    if (Updater.canInstall(ctx)) {
                        vm.store.saveNow()
                        Updater.install(ctx, state.file)
                    } else Updater.openInstallPermission(ctx)
                }
            },
        ) {
            Text(
                if (Updater.canInstall(ctx)) "Instalator systemowy zaktualizuje OpenDesktop. Układ pulpitu zostanie zachowany."
                else "Zezwól OpenDesktop na instalowanie aplikacji, a następnie wróć i dotknij „Instaluj”.",
                color = Palette.TextDim, fontSize = 14.sp, lineHeight = 19.sp,
            )
        }
        is UpdateState.UpToDate -> if (!onlyAuto) DarkDialog(
            title = "Brak aktualizacji",
            icon = Icons.Rounded.Check,
            onDismiss = close,
            buttons = { PillButton("OK", onClick = close) },
        ) {
            Text("Masz najnowszą wersję OpenDesktop.", color = Palette.TextDim, fontSize = 14.sp)
        }
        is UpdateState.Error -> if (!onlyAuto) DarkDialog(
            title = "Nie udało się sprawdzić",
            icon = Icons.Rounded.SystemUpdate,
            onDismiss = close,
            buttons = {
                PillButton("Historia wydań", primary = false) { SystemActions.openUrl(ctx, if (vm.config.value.updateRepo.equals(Config.DEFAULT_REPO, ignoreCase = true)) "https://redzikowski.com/portfolio/opendesktop#zmiany" else "https://github.com/${vm.config.value.updateRepo}/releases") }
                PillButton("OK", onClick = close)
            },
        ) {
            DialogNotice(state.message, error = true)
        }
        else -> Unit
    }
}
