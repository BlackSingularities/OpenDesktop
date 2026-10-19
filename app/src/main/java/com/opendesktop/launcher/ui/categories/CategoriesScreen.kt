package com.opendesktop.launcher.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opendesktop.launcher.data.AiCategory
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.Category
import com.opendesktop.launcher.data.Workspace
import com.opendesktop.launcher.ui.AiJob
import com.opendesktop.launcher.ui.LauncherViewModel
import com.opendesktop.launcher.ui.components.AppIcon
import com.opendesktop.launcher.ui.components.AppPickerDialog
import com.opendesktop.launcher.ui.components.TextInputDialog

@Composable
fun CategoriesScreen(vm: LauncherViewModel, onClose: () -> Unit) {
    val ws by vm.workspace.collectAsStateWithLifecycle()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val ai by vm.aiJob.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pickFor by remember { mutableStateOf<Category?>(null) }
    var confirmAuto by remember { mutableStateOf(false) }
    var aiDialog by remember { mutableStateOf(false) }
    val labelOf: (AppInfo) -> String = { ws.renamed[it.key] ?: it.label }
    val counts = remember(ws.assignments) {
        val m = HashMap<String, Int>()
        ws.assignments.values.forEach { ids -> ids.forEach { m[it] = (m[it] ?: 0) + 1 } }
        m
    }
    val uncategorized = apps.count { ws.assignments[it.key].isNullOrEmpty() }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", tint = Color.White) }
                Text("Kategorie aplikacji", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, "Nowa kategoria", tint = Color.White) }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)).padding(16.dp),
                    ) {
                        Text("Zbuduj kategorie automatycznie", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Aplikacja może należeć do wielu kategorii. Kategorie filtrują szufladę i tworzą sekcje przy sortowaniu „Kategoria”.",
                            color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { confirmAuto = true }) {
                                Icon(Icons.Rounded.AutoFixHigh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Bez AI")
                            }
                            Button(onClick = { aiDialog = true }) {
                                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Z AI")
                            }
                        }
                        if (!vm.aiReady) {
                            Text("AI wymaga klucza OpenRouter i wybranego modelu (Ustawienia → AI).", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                if (ws.categories.isEmpty()) {
                    item { Text("Brak kategorii — utwórz pierwszą przyciskiem +", color = Color.White.copy(alpha = 0.6f)) }
                }
                items(ws.categories, key = { it.id }) { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable { pickFor = c }
                            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(c.color).copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Text(c.emoji.ifBlank { c.name.take(1) }, fontSize = 18.sp, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${counts[c.id] ?: 0} aplikacji" + when (c.source) { "ai" -> " · AI"; "auto" -> " · automatyczna"; else -> "" },
                                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                            )
                        }
                        IconButton(onClick = { vm.moveCategory(c.id, -1) }) { Icon(Icons.Rounded.KeyboardArrowUp, "W górę", tint = Color.White) }
                        IconButton(onClick = { vm.moveCategory(c.id, 1) }) { Icon(Icons.Rounded.KeyboardArrowDown, "W dół", tint = Color.White) }
                        IconButton(onClick = { editing = c }) { Icon(Icons.Rounded.Edit, "Edytuj", tint = Color.White) }
                        IconButton(onClick = { vm.deleteCategory(c.id) }) { Icon(Icons.Rounded.Delete, "Usuń", tint = Color.White.copy(alpha = 0.8f)) }
                    }
                }
                item {
                    Text("Bez kategorii: $uncategorized aplikacji", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    if (creating || editing != null) {
        val c = editing
        TextInputDialog(
            title = if (c == null) "Nowa kategoria" else "Edytuj kategorię",
            initial = c?.name.orEmpty(),
            secondField = "Emoji" to (c?.emoji ?: "📁"),
            onDismiss = { creating = false; editing = null },
            onConfirm = { name, emoji ->
                if (c == null) vm.addCategory(name, emoji) else vm.updateCategory(c.id, name, emoji)
                creating = false; editing = null
            },
        )
    }

    pickFor?.let { c ->
        AppPickerDialog(
            title = (c.emoji + " " + c.name).trim(),
            apps = apps,
            labelOf = labelOf,
            initial = ws.assignments.filterValues { c.id in it }.keys,
            shape = cfg.iconShape,
            onDismiss = { pickFor = null },
            onConfirm = { keys -> vm.setCategoryApps(c.id, keys); pickFor = null },
        )
    }

    if (confirmAuto) {
        AlertDialog(
            onDismissRequest = { confirmAuto = false },
            title = { Text("Automatyczne kategorie") },
            text = { Text("Kategorie zostaną utworzone na podstawie typu aplikacji i jej nazwy. Zastąpić obecne czy dołączyć do nich?") },
            confirmButton = { TextButton(onClick = { vm.autoCategorize(replace = true); confirmAuto = false }) { Text("Zastąp") } },
            dismissButton = { TextButton(onClick = { vm.autoCategorize(replace = false); confirmAuto = false }) { Text("Dołącz") } },
        )
    }

    if (aiDialog || ai !is AiJob.Idle) {
        AiCategorizeDialog(vm, ai, apps, onDismiss = { aiDialog = false; vm.dismissAi() })
    }
}

@Composable
private fun AiCategorizeDialog(vm: LauncherViewModel, job: AiJob, apps: List<AppInfo>, onDismiss: () -> Unit) {
    var instructions by remember { mutableStateOf("") }
    var maxCats by remember { mutableFloatStateOf(10f) }
    var multi by remember { mutableStateOf(true) }
    var replace by remember { mutableStateOf(true) }
    val presets = listOf(
        "Według pory dnia (rano, praca, wieczór)",
        "Praca / dom / rozrywka",
        "Jak w sklepie Play, ale bardziej szczegółowo",
        "Według kolorów ikon",
        "Minimalistycznie — najwyżej 5 grup",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategorie z AI") },
        text = {
            when (job) {
                is AiJob.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(job.message)
                }
                is AiJob.Error -> Text(job.message, color = MaterialTheme.colorScheme.error)
                is AiJob.Result -> AiResultPreview(job.categories, apps)
                AiJob.Idle -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI otrzyma listę ${apps.size} aplikacji (nazwy i pakiety) i zaproponuje kategorie.", fontSize = 13.sp)
                    OutlinedTextField(
                        instructions, { instructions = it },
                        label = { Text("Twoje wskazówki (opcjonalnie)") },
                        placeholder = { Text("np. osobno aplikacje do latania i do pracy") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                    )
                    LazyColumn(Modifier.heightIn(max = 120.dp)) {
                        items(presets) { p ->
                            Text(
                                "• $p", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().clickable { instructions = p }.padding(vertical = 4.dp),
                            )
                        }
                    }
                    Text("Maksymalnie kategorii: ${maxCats.toInt()}", fontSize = 13.sp)
                    Slider(maxCats, { maxCats = it }, valueRange = 3f..20f, steps = 16)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aplikacja w wielu kategoriach", Modifier.weight(1f), fontSize = 13.sp)
                        Switch(multi, { multi = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zastąp obecne kategorie", Modifier.weight(1f), fontSize = 13.sp)
                        Switch(replace, { replace = it })
                    }
                }
            }
        },
        confirmButton = {
            when (job) {
                AiJob.Idle -> TextButton(onClick = { vm.runAiCategorize(instructions, maxCats.toInt(), multi) }, enabled = vm.aiReady) { Text("Generuj") }
                is AiJob.Result -> TextButton(onClick = { vm.applyAiResult(job.categories, replace); onDismiss() }) { Text("Zastosuj") }
                is AiJob.Error -> TextButton(onClick = { vm.dismissAi() }) { Text("Spróbuj ponownie") }
                is AiJob.Running -> Unit
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (job is AiJob.Result) "Odrzuć" else "Zamknij") } },
    )
}

@Composable
private fun AiResultPreview(cats: List<AiCategory>, apps: List<AppInfo>) {
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(cats) { c ->
            Column {
                Text("${c.emoji} ${c.name}".trim(), fontWeight = FontWeight.SemiBold)
                val list = c.apps.mapNotNull { byPkg[it] }
                Text(
                    list.joinToString { it.label }.ifBlank { "—" },
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Przypisanie jednej aplikacji do dowolnej liczby kategorii. */
@Composable
fun AssignCategoriesDialog(vm: LauncherViewModel, app: AppInfo, ws: Workspace, onDismiss: () -> Unit) {
    var chosen by remember(app.key) { mutableStateOf(ws.assignments[app.key].orEmpty().toSet()) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.icon, "", 36.dp, com.opendesktop.launcher.data.IconShape.System, false)
                Spacer(Modifier.width(10.dp))
                Text(ws.renamed[app.key] ?: app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(ws.categories, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { chosen = if (c.id in chosen) chosen - c.id else chosen + c.id }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(c.id in chosen, { chosen = if (it) chosen + c.id else chosen - c.id })
                            Text((c.emoji + "  " + c.name).trim())
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newName, { newName = it }, singleLine = true, placeholder = { Text("Nowa kategoria") }, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (newName.isNotBlank()) {
                            val id = vm.addCategory(newName, "")
                            chosen = chosen + id
                            newName = ""
                        }
                    }) { Icon(Icons.Rounded.Add, "Dodaj") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.setAppCategories(app.key, chosen.toList()); onDismiss() }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
