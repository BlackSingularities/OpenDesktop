package com.opendesktop.launcher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Kontakt znaleziony w wyszukiwarce. */
data class ContactHit(val id: Long, val name: String, val photo: Uri?, val lookupKey: String) {
    val uri: Uri get() = ContactsContract.Contacts.getLookupUri(id, lookupKey)
}

/** Ekran ustawień systemowych do otwarcia z wyszukiwarki. */
data class SettingHit(val label: String, val action: String, val keywords: List<String>)

/** Źródła wyników poza aplikacjami: kontakty i ustawienia systemowe. */
object UniversalSearch {

    fun hasContactsPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Wywoływać poza wątkiem głównym. */
    fun contacts(ctx: Context, query: String, limit: Int = 5): List<ContactHit> {
        if (query.isBlank() || !hasContactsPermission(ctx)) return emptyList()
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(query.trim()))
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.LOOKUP_KEY,
        )
        return runCatching {
            ctx.contentResolver.query(uri, projection, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY)?.use { c ->
                buildList {
                    while (c.moveToNext() && size < limit) {
                        val name = c.getString(1) ?: continue
                        add(ContactHit(c.getLong(0), name, c.getString(2)?.let(Uri::parse), c.getString(3).orEmpty()))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    fun openContact(ctx: Context, hit: ContactHit) {
        val uri = if (hit.lookupKey.isNotEmpty()) hit.uri else ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, hit.id)
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private val settings = listOf(
        SettingHit("Wi‑Fi", Settings.ACTION_WIFI_SETTINGS, listOf("wifi", "wi-fi", "sieć", "internet")),
        SettingHit("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, listOf("bluetooth", "bt", "słuchawki", "parowanie")),
        SettingHit("Dane komórkowe", Settings.ACTION_DATA_ROAMING_SETTINGS, listOf("dane", "komórkowe", "roaming", "lte", "5g")),
        SettingHit("Tryb samolotowy", Settings.ACTION_AIRPLANE_MODE_SETTINGS, listOf("samolot", "samolotowy")),
        SettingHit("Ekran", Settings.ACTION_DISPLAY_SETTINGS, listOf("ekran", "jasność", "wyświetlacz", "czcionka", "ciemny")),
        SettingHit("Dźwięk", Settings.ACTION_SOUND_SETTINGS, listOf("dźwięk", "głośność", "dzwonek", "wibracje", "cisza")),
        SettingHit("Bateria", Intent.ACTION_POWER_USAGE_SUMMARY, listOf("bateria", "akumulator", "oszczędzanie", "zużycie")),
        SettingHit("Pamięć", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, listOf("pamięć", "miejsce", "dysk")),
        SettingHit("Aplikacje", Settings.ACTION_APPLICATION_SETTINGS, listOf("aplikacje", "domyślne")),
        SettingHit("Powiadomienia", "android.settings.NOTIFICATION_SETTINGS", listOf("powiadomienia", "nie przeszkadzać")),
        SettingHit("Lokalizacja", Settings.ACTION_LOCATION_SOURCE_SETTINGS, listOf("lokalizacja", "gps", "położenie")),
        SettingHit("Bezpieczeństwo", Settings.ACTION_SECURITY_SETTINGS, listOf("bezpieczeństwo", "blokada", "odcisk", "pin", "hasło")),
        SettingHit("Prywatność", Settings.ACTION_PRIVACY_SETTINGS, listOf("prywatność", "uprawnienia")),
        SettingHit("Konta", Settings.ACTION_SYNC_SETTINGS, listOf("konta", "konto", "synchronizacja", "google")),
        SettingHit("Data i godzina", Settings.ACTION_DATE_SETTINGS, listOf("data", "godzina", "czas", "strefa")),
        SettingHit("Język i klawiatura", Settings.ACTION_LOCALE_SETTINGS, listOf("język", "klawiatura")),
        SettingHit("Ułatwienia dostępu", Settings.ACTION_ACCESSIBILITY_SETTINGS, listOf("ułatwienia", "dostępność", "talkback")),
        SettingHit("NFC", Settings.ACTION_NFC_SETTINGS, listOf("nfc", "płatności zbliżeniowe")),
        SettingHit("Hotspot", "android.settings.TETHER_SETTINGS", listOf("hotspot", "tethering", "udostępnianie internetu")),
        SettingHit("Tapeta", Intent.ACTION_SET_WALLPAPER, listOf("tapeta")),
        SettingHit("Ekran główny", Settings.ACTION_HOME_SETTINGS, listOf("launcher", "ekran główny")),
        SettingHit("Opcje programisty", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, listOf("programista", "deweloper", "debugowanie")),
        SettingHit("Informacje o telefonie", Settings.ACTION_DEVICE_INFO_SETTINGS, listOf("telefon", "wersja", "android", "model")),
        SettingHit("Ustawienia", Settings.ACTION_SETTINGS, listOf("ustawienia")),
    )

    fun settings(query: String, limit: Int = 3): List<SettingHit> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return settings.filter { s -> s.label.lowercase().contains(q) || s.keywords.any { it.startsWith(q) || (q.length >= 4 && q in it) } }
            .take(limit)
    }

    fun openSetting(ctx: Context, hit: SettingHit) {
        val ok = runCatching { ctx.startActivity(Intent(hit.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!ok) runCatching { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
