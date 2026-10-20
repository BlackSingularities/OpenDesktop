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
import com.opendesktop.launcher.data.aiModelLabel
import com.opendesktop.launcher.ui.settings.DarkDialog
import com.opendesktop.launcher.ui.settings.DialogChip
import com.opendesktop.launcher.ui.settings.DialogLoading
import com.opendesktop.launcher.ui.settings.DialogNotice
import com.opendesktop.launcher.ui.settings.DialogTextField
import com.opendesktop.launcher.ui.settings.PillButton
import com.opendesktop.launcher.ui.theme.Palette
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults

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
                            Text("AI wymaga dostawcy, klucza i wybranego modelu (Ustawienia → Sztuczna inteligencja).", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                if (ws.categories.isEmpty()) {
                    item { Text("Brak kategorii – utwórz pierwszą przyciskiem +", color = Color.White.copy(alpha = 0.6f)) }
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
        DarkDialog(
            title = "Automatyczne kategorie",
            icon = Icons.Rounded.AutoFixHigh,
            onDismiss = { confirmAuto = false },
            buttons = {
                PillButton("Anuluj", primary = false) { confirmAuto = false }
                PillButton("Dołącz", primary = false) { vm.autoCategorize(replace = false); confirmAuto = false }
                PillButton("Zastąp") { vm.autoCategorize(replace = true); confirmAuto = false }
            },
        ) {
            Text(
                "Kategorie zostaną utworzone na podstawie typu aplikacji i jej nazwy. Zastąpić obecne czy dołączyć do nich?",
                color = Palette.TextDim, fontSize = 14.sp, lineHeight = 19.sp,
            )
        }
    }

    if (aiDialog || ai !is AiJob.Idle) {
        AiCategorizeDialog(vm, ai, apps, onDismiss = { aiDialog = false; vm.dismissAi() })
    }
}

@Composable
private fun AiCategorizeDialog(vm: LauncherViewModel, job: AiJob, apps: List<AppInfo>, onDismiss: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    var instructions by remember { mutableStateOf("") }
    var maxCats by remember { mutableFloatStateOf(10f) }
    var multi by remember { mutableStateOf(true) }
    var replace by remember { mutableStateOf(true) }
    val presets = listOf(
        "Według pory dnia (rano, praca, wieczór)",
        "Praca / dom / rozrywka",
        "Jak w sklepie Play, ale bardziej szczegółowo",
        "Według kolorów ikon",
        "Minimalistycznie – najwyżej 5 grup",
    )
    DarkDialog(
        title = "Kategorie z AI",
        subtitle = if (vm.aiReady) "${cfg.aiProvider.label} · ${cfg.aiModelLabel()}" else null,
        icon = Icons.Rounded.AutoAwesome,
        onDismiss = onDismiss,
        // W trakcie generowania okno nie znika przypadkiem – wynik i tak by przepadł
        dismissible = job !is AiJob.Running,
        scrollable = job !is AiJob.Result,
        buttons = {
            when (job) {
                is AiJob.Running -> PillButton("Anuluj", primary = false, onClick = onDismiss)
                is AiJob.Result -> {
                    PillButton("Odrzuć", primary = false, onClick = onDismiss)
                    PillButton("Zastosuj") { vm.applyAiResult(job.categories, replace); onDismiss() }
                }
                is AiJob.Error -> {
                    PillButton("Zamknij", primary = false, onClick = onDismiss)
                    PillButton("Spróbuj ponownie") { vm.dismissAi() }
                }
                AiJob.Idle -> {
                    PillButton("Zamknij", primary = false, onClick = onDismiss)
                    PillButton("Generuj", enabled = vm.aiReady) { vm.runAiCategorize(instructions, maxCats.toInt(), multi) }
                }
            }
        },
    ) {
        when (job) {
            is AiJob.Running -> DialogLoading(job.message)
            is AiJob.Error -> DialogNotice(job.message, error = true)
            is AiJob.Result -> {
                Text("Propozycja: ${job.categories.size} kategorii", color = Palette.TextDim, fontSize = 13.sp)
                AiResultPreview(job.categories, apps)
                SwitchLine("Zastąp obecne kategorie", replace) { replace = it }
            }
            AiJob.Idle -> {
                if (!vm.aiReady) DialogNotice("Wybierz dostawcę, wpisz klucz i wybierz model w Ustawienia → Sztuczna inteligencja.", error = true)
                Text("AI otrzyma listę ${apps.size} aplikacji (nazwy i pakiety) i zaproponuje kategorie.", color = Palette.TextDim, fontSize = 13.sp)
                DialogTextField(
                    instructions, { instructions = it },
                    label = "Twoje wskazówki (opcjonalnie)",
                    placeholder = "np. osobno aplikacje do latania i do pracy",
                    singleLine = false, minLines = 2,
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { p -> DialogChip(p, instructions == p) { instructions = p } }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Palette.Group).padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Maksymalnie kategorii", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${maxCats.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Slider(
                        maxCats, { maxCats = it }, valueRange = 3f..20f, steps = 16,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.15f), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
                    )
                    SwitchLine("Aplikacja w wielu kategoriach", multi) { multi = it }
                    SwitchLine("Zastąp obecne kategorie", replace) { replace = it }
                }
            }
        }
    }
}

@Composable
private fun SwitchLine(text: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            value, onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Palette.Ink, checkedTrackColor = Color.White, uncheckedThumbColor = Palette.TextDim, uncheckedTrackColor = Color.White.copy(alpha = 0.1f), uncheckedBorderColor = Color.White.copy(alpha = 0.2f)),
        )
    }
}

@Composable
private fun AiResultPreview(cats: List<AiCategory>, apps: List<AppInfo>) {
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(cats) { c ->
            val list = c.apps.mapNotNull { byPkg[it] }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Group).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${c.emoji} ${c.name}".trim(), color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${list.size}", color = Palette.TextDim, fontSize = 12.sp)
                }
                Text(
                    list.joinToString { it.label }.ifBlank { "–" },
                    fontSize = 12.sp, color = Palette.TextDim, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp,
                )
            }
        }
    }
}

/** Przypisanie jednej aplikacji do dowolnej liczby kategorii. */
@Composable
fun AssignCategoriesDialog(vm: LauncherViewModel, app: AppInfo, ws: Workspace, onDismiss: () -> Unit) {
    var chosen by remember(app.key) { mutableStateOf(ws.assignments[app.key].orEmpty().toSet()) }
    var newName by remember { mutableStateOf("") }
    DarkDialog(
        title = ws.renamed[app.key] ?: app.label,
        subtitle = "Kategorie aplikacji",
        onDismiss = onDismiss,
        scrollable = false,
        buttons = {
            PillButton("Anuluj", primary = false, onClick = onDismiss)
            PillButton("Zapisz") { vm.setAppCategories(app.key, chosen.toList()); onDismiss() }
        },
    ) {
        if (ws.categories.isEmpty()) DialogNotice("Brak kategorii – dodaj pierwszą poniżej.")
        LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(ws.categories, key = { it.id }) { c ->
                val on = c.id in chosen
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (on) Color.White.copy(alpha = 0.14f) else Palette.Group)
                        .clickable { chosen = if (on) chosen - c.id else chosen + c.id }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        on, { chosen = if (it) chosen + c.id else chosen - c.id },
                        colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Palette.Ink, uncheckedColor = Palette.TextDim),
                    )
                    Text((c.emoji + "  " + c.name).trim(), color = Color.White, fontSize = 14.sp)
                }
            }
        }
        DialogTextField(
            newName, { newName = it },
            placeholder = "Nowa kategoria",
            trailing = {
                IconButton(onClick = {
                    if (newName.isNotBlank()) {
                        val id = vm.addCategory(newName, "")
                        chosen = chosen + id
                        newName = ""
                    }
                }) { Icon(Icons.Rounded.Add, "Dodaj", tint = Color.White) }
            },
        )
    }
}
