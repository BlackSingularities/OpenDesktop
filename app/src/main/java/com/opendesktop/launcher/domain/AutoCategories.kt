package com.opendesktop.launcher.domain

import android.content.pm.ApplicationInfo
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.Category

/** Kategorie bez AI: systemowa kategoria z manifestu + słowa kluczowe w nazwie i pakiecie. */
object AutoCategories {
    private class Rule(
        val name: String,
        val emoji: String,
        val color: Long,
        val sys: Set<Int>,
        val keywords: List<String>,
    )

    private val rules = listOf(
        Rule("Komunikacja", "💬", 0xFF64B5F6, emptySet(), listOf(
            "whatsapp", "messenger", "telegram", "signal", "thoughtcrime", "messaging", ".mms", "sms", "dialer",
            "contacts", "discord", "viber", "skype", "teams", "slack", "zoom", "meet", "wire", "element", "threema", "phone",
        )),
        Rule("Społeczności", "👥", 0xFFBA68C8, setOf(ApplicationInfo.CATEGORY_SOCIAL), listOf(
            "facebook", "instagram", "twitter", "tiktok", "musically", "reddit", "snapchat", "linkedin",
            "pinterest", "threads", "mastodon", "bluesky", "tumblr", "wykop",
        )),
        Rule("Internet i poczta", "🌐", 0xFF4DD0E1, emptySet(), listOf(
            "browser", "chrome", "firefox", "opera", "brave", "edge", "duckduckgo", "vivaldi", "samsung.android.app.sbrowser",
            "mail", "gmail", "outlook", "thunderbird", "protonmail", "k9",
        )),
        Rule("Muzyka i audio", "🎵", 0xFF81C784, setOf(ApplicationInfo.CATEGORY_AUDIO), listOf(
            "spotify", "music", "deezer", "tidal", "soundcloud", "podcast", "radio", "audible", "shazam", "audiobook",
        )),
        Rule("Wideo", "🎬", 0xFFE57373, setOf(ApplicationInfo.CATEGORY_VIDEO), listOf(
            "youtube", "netflix", "video", "vlc", "twitch", "hbo", "disney", "primevideo", "player", "max.", "skyshowtime", "canal",
        )),
        Rule("Zdjęcia", "📷", 0xFFFFB74D, setOf(ApplicationInfo.CATEGORY_IMAGE), listOf(
            "camera", "gallery", "photos", "photo", "snapseed", "lightroom", "picsart", "canva", "scanner",
        )),
        Rule("Mapy i podróże", "🗺️", 0xFF4DB6AC, setOf(ApplicationInfo.CATEGORY_MAPS), listOf(
            "maps", "navigation", "waze", "uber", "bolt", "booking", "airbnb", "transit", "jakdojade", "pkp",
            "flight", "ryanair", "wizz", "trip", "osmand", "yanosik", "freenow",
        )),
        Rule("Wiadomości", "📰", 0xFF90A4AE, setOf(ApplicationInfo.CATEGORY_NEWS), listOf(
            "news", "onet", "wp.pl", "interia", "gazeta", "rss", "feedly", "tvn24", "weather", "pogoda",
        )),
        Rule("Produktywność", "📋", 0xFF7986CB, setOf(ApplicationInfo.CATEGORY_PRODUCTIVITY), listOf(
            "docs", "sheets", "slides", "office", "word", "excel", "powerpoint", "notion", "todo", "tasks", "calendar",
            "keep", "notes", "drive", "dropbox", "onedrive", "pdf", "evernote", "trello", "obsidian",
        )),
        Rule("Finanse", "💳", 0xFFAED581, emptySet(), listOf(
            "bank", "wallet", "revolut", "mbank", "pko", "ing", "santander", "millennium", "pekao", "blik",
            "finance", "crypto", "binance", "paypal", "zen.com", "aliorbank", "creditagricole", "nest",
        )),
        Rule("Zakupy", "🛍️", 0xFFF06292, emptySet(), listOf(
            "allegro", "amazon", "shop", "olx", "aliexpress", "temu", "vinted", "zalando", "lidl", "biedronka",
            "ebay", "shein", "inpost", "glovo", "pyszne", "wolt", "ikea", "rossmann",
        )),
        Rule("Gry", "🎮", 0xFFFF8A65, setOf(ApplicationInfo.CATEGORY_GAME), listOf("game", "games", "supercell", "king.", "rovio")),
        Rule("Zdrowie i sport", "💪", 0xFFFFD54F, emptySet(), listOf(
            "fit", "health", "strava", "sport", "workout", "sleep", "garmin", "zepp", "mifit", "samsung.android.app.shealth", "ikp", "medicover", "luxmed",
        )),
        Rule("Edukacja", "🎓", 0xFF9575CD, emptySet(), listOf(
            "duolingo", "edu", "learn", "school", "librus", "vulcan", "quizlet", "dictionary", "translate", "deepl", "khan",
        )),
        Rule("Narzędzia", "🛠️", 0xFFA1887F, setOf(ApplicationInfo.CATEGORY_ACCESSIBILITY), listOf(
            "settings", "calculator", "clock", "deskclock", "files", "filemanager", "documentsui", "flashlight", "torch",
            "compass", "recorder", "vpn", "keyboard", "launcher", "authenticator", "backup", "cleaner", "tools", "store", "vending",
        )),
    )

    fun generate(apps: List<AppInfo>): Pair<List<Category>, Map<String, List<String>>> {
        val cats = rules.map { Category(name = it.name, emoji = it.emoji, color = it.color, source = "auto") }
        val assign = HashMap<String, MutableList<String>>()
        val other = Category(name = "Inne", emoji = "📦", color = 0xFF78909C, source = "auto")
        for (app in apps) {
            val hay = (app.packageName + " " + app.label).lowercase()
            val hits = ArrayList<String>()
            rules.forEachIndexed { i, r ->
                if (app.systemCategory in r.sys || r.keywords.any { hay.contains(it) }) hits += cats[i].id
            }
            assign[app.key] = (if (hits.isEmpty()) listOf(other.id) else hits.take(2)).toMutableList()
        }
        val used = assign.values.flatten().toSet()
        val all = (cats + other).filter { it.id in used }
        return all to assign
    }
}
