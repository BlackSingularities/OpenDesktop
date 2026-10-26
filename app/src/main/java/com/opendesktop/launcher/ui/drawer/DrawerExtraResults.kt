package com.opendesktop.launcher.ui.drawer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.data.ContactHit
import com.opendesktop.launcher.data.SettingHit
import com.opendesktop.launcher.data.UniversalSearch
import com.opendesktop.launcher.domain.QuickAnswers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val PanelShape = RoundedCornerShape(18.dp)
private val PanelColor = Color.Black.copy(alpha = 0.35f)

/**
 * Zgoda na kontakty: pytamy dopiero przy pierwszym wyszukiwaniu, a odmowa po prostu
 * ukrywa sekcję kontaktów – wyszukiwanie aplikacji działa dalej.
 */
@Composable
fun rememberContactsAccess(query: String, enabled: Boolean): Boolean {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(UniversalSearch.hasContactsPermission(ctx)) }
    var asked by remember { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(query.isNotBlank(), enabled) {
        if (enabled && query.isNotBlank() && !granted && !asked) {
            asked = true
            runCatching { request.launch(Manifest.permission.READ_CONTACTS) }
        }
    }
    return granted && enabled
}

/** Kontakty z książki adresowej – zapytanie w tle, z krótkim wyhamowaniem przy pisaniu. */
@Composable
fun rememberContactHits(query: String, enabled: Boolean): List<ContactHit> {
    val ctx = LocalContext.current
    return produceState(emptyList<ContactHit>(), query, enabled) {
        if (!enabled || query.isBlank()) {
            value = emptyList()
            return@produceState
        }
        delay(180)
        value = withContext(Dispatchers.IO) { UniversalSearch.contacts(ctx, query) }
    }.value
}

/** Wynik działania albo przeliczenia jednostek, z kopiowaniem do schowka. */
@Composable
fun QuickAnswerCard(query: String, modifier: Modifier = Modifier) {
    val answer = remember(query) { QuickAnswers.answer(query) } ?: return
    val clipboard = LocalClipboardManager.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(PanelColor)
            .clickable { clipboard.setText(AnnotatedString(answer.result)) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Functions, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                answer.result,
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                answer.expression,
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.ContentCopy, "Kopiuj wynik", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
    }
}

/** Kontakty i ekrany ustawień systemowych pod wynikami aplikacji. */
@Composable
fun ExtraResults(query: String, contacts: List<ContactHit>, settings: List<SettingHit>, modifier: Modifier = Modifier) {
    if (query.isBlank() || (contacts.isEmpty() && settings.isEmpty())) return
    val ctx = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(PanelColor)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (contacts.isNotEmpty()) {
            SectionLabel("Kontakty")
            contacts.forEach { c ->
                ResultRow(c.name, null, { UniversalSearch.openContact(ctx, c) }) { Avatar(c.name) }
            }
        }
        if (settings.isNotEmpty()) {
            if (contacts.isNotEmpty()) Spacer(Modifier.height(6.dp))
            SectionLabel("Ustawienia systemu")
            settings.forEach { s ->
                ResultRow(s.label, "Ustawienia Androida", { UniversalSearch.openSetting(ctx, s) }) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), Alignment.Center) {
                        Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResultRow(title: String, subtitle: String?, onClick: () -> Unit, leading: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), Alignment.Center) {
        Text(
            name.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
            color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}
