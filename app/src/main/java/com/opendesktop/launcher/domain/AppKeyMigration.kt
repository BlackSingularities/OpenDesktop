package com.opendesktop.launcher.domain

import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.Workspace

/**
 * Aplikacje takie jak Duolingo zmieniają ikonę, przełączając `activity-alias`: system włącza
 * nowy komponent i wyłącza stary. Klucz aplikacji to nazwa komponentu, więc bez tej migracji
 * ikona znikałaby z pulpitu, doku i folderów. Brakujący klucz przepinamy na komponent
 * startowy tego samego pakietu (i profilu), który właśnie się pojawił.
 */
object AppKeyMigration {

    private fun packageOf(key: String) = key.substringBefore('#').substringBefore('/')
    private fun profileOf(key: String) = key.substringAfter('#', "")

    fun migrate(ws: Workspace, apps: List<AppInfo>): Workspace {
        if (apps.isEmpty()) return ws
        val present = apps.mapTo(HashSet()) { it.key }
        val referenced = referencedKeys(ws)
        val stale = referenced.filter { it !in present }
        if (stale.isEmpty()) return ws

        val byOwner = apps.groupBy { packageOf(it.key) to profileOf(it.key) }
        val taken = referenced.filterTo(HashSet()) { it in present }
        val remap = HashMap<String, String>()
        for (old in stale) {
            // Pakiet odinstalowany lub chwilowo bez aktywności – zostawiamy wpis bez zmian
            val candidates = byOwner[packageOf(old) to profileOf(old)] ?: continue
            // Najpierw komponent, którego jeszcze nie ma w układzie – unikamy duplikatów.
            // Zajęty komponent bierzemy tylko, gdy pakiet ma jedną aktywność startową:
            // przy kilku moglibyśmy np. ukryć albo przemianować inną, niezwiązaną aktywność.
            val target = candidates.firstOrNull { it.key !in taken && it.key !in remap.values }
                ?: candidates.singleOrNull()
                ?: continue
            remap[old] = target.key
        }
        if (remap.isEmpty()) return ws
        return apply(ws, remap)
    }

    private fun referencedKeys(ws: Workspace): Set<String> {
        val out = LinkedHashSet<String>()
        for (p in ws.pages) for (it in p.items) {
            it.app?.let(out::add)
            it.primaryApp?.let(out::add)
            out += it.apps
        }
        out += ws.dock
        out += ws.hidden
        out += ws.renamed.keys
        out += ws.assignments.keys
        out += ws.usage.keys
        return out
    }

    private fun apply(ws: Workspace, remap: Map<String, String>): Workspace {
        fun k(key: String) = remap[key] ?: key
        fun item(i: DesktopItem) = i.copy(
            app = i.app?.let(::k),
            primaryApp = i.primaryApp?.let(::k),
            apps = i.apps.map(::k).distinct(),
        )
        /** Wpis docelowej aplikacji, jeśli już istnieje, ma pierwszeństwo przed przeniesionym. */
        fun <V> remapMap(m: Map<String, V>, merge: (existing: V, moved: V) -> V): Map<String, V> {
            val out = LinkedHashMap<String, V>()
            for ((key, v) in m) if (key !in remap) out[key] = v
            for ((key, v) in m) if (key in remap) {
                val nk = k(key)
                out[nk] = out[nk]?.let { merge(it, v) } ?: v
            }
            return out
        }
        return ws.copy(
            pages = ws.pages.map { p -> p.copy(items = p.items.map(::item)) },
            dock = ws.dock.map(::k).distinct(),
            hidden = ws.hidden.mapTo(LinkedHashSet(), ::k),
            renamed = remapMap(ws.renamed) { existing, _ -> existing },
            assignments = remapMap(ws.assignments) { a, b -> (a + b).distinct() },
            usage = remapMap(ws.usage) { a, b -> a + b },
        )
    }
}
