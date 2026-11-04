package com.opendesktop.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Skróty przypinane na pulpit przez inne aplikacje – adres z przeglądarki (Firefox, Chrome),
 * rozmowa z komunikatora, strona w aplikacji.
 *
 * Android ma na to dwie drogi i obie tu obsługujemy:
 * - nowszą ([LauncherApps.PinItemRequest], od Androida 8) – skrót przypinamy przez system
 *   i uruchamiamy po parze pakiet + identyfikator;
 * - dawne rozgłoszenie `com.android.launcher.action.INSTALL_SHORTCUT` – zostaje nam sama
 *   intencja, więc zapisujemy ją jako URI.
 *
 * Ikony nie wchodzą do workspace.json (to bitmapy) – lądują w plikach PNG w katalogu
 * `shortcuts`, a element pulpitu trzyma tylko nazwę pliku.
 */
object ShortcutIcons {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    private fun dir(context: Context): File = File(context.filesDir, "shortcuts").apply { mkdirs() }

    /** Zapisuje ikonę skrótu i zwraca nazwę pliku; null, gdy nie udało się nic zapisać. */
    fun save(context: Context, bitmap: Bitmap?): String? {
        val bmp = bitmap ?: return null
        val name = newId() + ".png"
        return runCatching {
            File(dir(context), name).outputStream().use { out ->
                val size = 192
                val scaled = if (bmp.width > size || bmp.height > size) Bitmap.createScaledBitmap(bmp, size, size, true) else bmp
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            name
        }.getOrNull()
    }

    fun save(context: Context, drawable: Drawable?): String? =
        save(context, runCatching { drawable?.toBitmap(192, 192, Bitmap.Config.ARGB_8888) }.getOrNull())

    fun cached(name: String?): ImageBitmap? = name?.let { cache[it] }

    fun load(context: Context, name: String): ImageBitmap? = cache[name] ?: runCatching {
        val f = File(dir(context), name)
        if (!f.exists()) return null
        android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()?.also { cache[name] = it }
    }.getOrNull()
}

object Shortcuts {
    /**
     * Skrót z żądania systemu (aplikacja wywołała `requestPinShortcut`). Przypięcie potwierdzamy
     * od razu – użytkownik dotknął „Dodaj na ekran główny” w tamtej aplikacji, więc drugie pytanie
     * tylko by przeszkadzało. Zwraca element bez położenia (x/y ustawia pulpit).
     */
    fun fromPinRequest(context: Context, intent: Intent): DesktopItem? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val request = runCatching { launcherApps.getPinItemRequest(intent) }.getOrNull() ?: return null
        if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return null
        val info = request.shortcutInfo ?: return null
        if (!runCatching { request.accept() }.getOrDefault(false)) return null
        val density = context.resources.displayMetrics.densityDpi
        val icon = runCatching { launcherApps.getShortcutIconDrawable(info, density) }.getOrNull()
        return DesktopItem(
            type = ItemType.Shortcut,
            x = 0, y = 0,
            label = (info.shortLabel ?: info.longLabel ?: "").toString().trim().ifBlank { info.`package` },
            shortcutPkg = info.`package`,
            shortcutId = info.id,
            iconFile = ShortcutIcons.save(context, icon),
        )
    }

    /** Skrót z dawnego rozgłoszenia `INSTALL_SHORTCUT`. */
    @Suppress("DEPRECATION")
    fun fromLegacyBroadcast(context: Context, intent: Intent): DesktopItem? {
        val target = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT) ?: return null
        // Przyjmujemy tylko skróty uruchamiające aktywność – nic nie rozgłaszamy w imieniu innych aplikacji
        if (target.action == null && target.component == null && target.data == null) return null
        val name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.trim().orEmpty()
        val bitmap = intent.getParcelableExtra<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)
        val iconFile = ShortcutIcons.save(context, bitmap) ?: legacyResourceIcon(context, intent)
        return DesktopItem(
            type = ItemType.Shortcut,
            x = 0, y = 0,
            label = name.ifBlank { target.data?.host ?: "Skrót" },
            shortcutUri = target.toUri(Intent.URI_INTENT_SCHEME),
            iconFile = iconFile,
        )
    }

    @Suppress("DEPRECATION")
    private fun legacyResourceIcon(context: Context, intent: Intent): String? = runCatching {
        val res = intent.getParcelableExtra<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE) ?: return null
        val pkgRes = context.packageManager.getResourcesForApplication(res.packageName)
        val id = pkgRes.getIdentifier(res.resourceName, null, res.packageName)
        if (id == 0) return null
        ShortcutIcons.save(context, pkgRes.getDrawable(id, null))
    }.getOrNull()

    /** Uruchomienie skrótu: przypiętego przez system albo z zapisanej intencji. */
    fun start(context: Context, item: DesktopItem, bounds: android.graphics.Rect?, options: android.os.Bundle?): Boolean {
        val pkg = item.shortcutPkg
        val id = item.shortcutId
        if (pkg != null && id != null) {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            val ok = runCatching {
                launcherApps.startShortcut(pkg, id, bounds, options, Process.myUserHandle())
            }.isSuccess
            if (ok) return true
        }
        val uri = item.shortcutUri ?: return false
        return runCatching {
            val target = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            target.sourceBounds = bounds
            context.startActivity(target, options)
            true
        }.getOrDefault(false)
    }
}
