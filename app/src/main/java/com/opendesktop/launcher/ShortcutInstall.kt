package com.opendesktop.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.ItemType
import com.opendesktop.launcher.data.Shortcuts
import com.opendesktop.launcher.domain.GridOps

/**
 * Dokłada przypięty skrót na pulpit i mówi, co się stało. Skrót trafia na ekran domowy,
 * a gdy nie ma na nim miejsca – na pierwszy kolejny z wolną komórką.
 */
private fun installShortcut(context: Context, item: DesktopItem?) {
    val app = context.applicationContext as? LauncherApplication
    if (app == null || item == null) {
        Toast.makeText(context, "Nie udało się dodać skrótu", Toast.LENGTH_SHORT).show()
        return
    }
    val cfg = app.store.config.value
    val existing = app.store.workspace.value.pages.any { page ->
        page.items.any {
            it.type == ItemType.Shortcut &&
                ((item.shortcutId != null && it.shortcutId == item.shortcutId && it.shortcutPkg == item.shortcutPkg) ||
                    (item.shortcutUri != null && it.shortcutUri == item.shortcutUri))
        }
    }
    if (existing) {
        Toast.makeText(context, "Skrót „${item.label}” już jest na pulpicie", Toast.LENGTH_SHORT).show()
        return
    }
    app.store.updateWorkspace { ws ->
        ws.copy(pages = GridOps.addItem(ws.pages, cfg.defaultPage, item, cfg.columns, cfg.rows))
    }
    app.store.saveNow()
    Toast.makeText(context, "Dodano skrót „${item.label}” na pulpit", Toast.LENGTH_SHORT).show()
}

/**
 * Potwierdzenie przypięcia skrótu (Android 8+): aplikacja – na przykład Firefox przy „Dodaj do
 * ekranu głównego” – prosi system, a system otwiera to okno w launcherze. Nie pytamy drugi raz,
 * tylko dokładamy skrót i zamykamy się bez widocznego ekranu.
 */
class PinShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installShortcut(this, Shortcuts.fromPinRequest(this, intent))
        finish()
    }
}

/** Dawne rozgłoszenie `com.android.launcher.action.INSTALL_SHORTCUT` – starsze aplikacje. */
class InstallShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        installShortcut(context, Shortcuts.fromLegacyBroadcast(context, intent))
    }
}
