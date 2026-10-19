package com.opendesktop.launcher.ui

import android.app.ActivityOptions
import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesktop.launcher.LauncherApplication
import com.opendesktop.launcher.data.AiAppRef
import com.opendesktop.launcher.data.AiCategory
import com.opendesktop.launcher.data.AiModel
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.Category
import com.opendesktop.launcher.data.Config
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.ItemType
import com.opendesktop.launcher.data.OpenRouter
import com.opendesktop.launcher.data.Page
import com.opendesktop.launcher.data.RepoHit
import com.opendesktop.launcher.data.SearchStyle
import com.opendesktop.launcher.data.GadgetKind
import com.opendesktop.launcher.data.GadgetStyle
import com.opendesktop.launcher.data.ShortcutEntry
import com.opendesktop.launcher.data.SystemAction
import com.opendesktop.launcher.data.UpdateInfo
import com.opendesktop.launcher.data.Updater
import com.opendesktop.launcher.data.Workspace
import com.opendesktop.launcher.domain.AutoCategories
import com.opendesktop.launcher.domain.GridOps
import com.opendesktop.launcher.ui.desktop.DragPayload
import com.opendesktop.launcher.ui.desktop.DragSource
import com.opendesktop.launcher.ui.desktop.DropTarget
import com.opendesktop.launcher.ui.desktop.DropZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

sealed interface AiJob {
    data object Idle : AiJob
    data class Running(val message: String) : AiJob
    data class Result(val categories: List<AiCategory>) : AiJob
    data class Error(val message: String) : AiJob
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val launcherApp = application as LauncherApplication
    val store = launcherApp.store
    private val repo = launcherApp.apps
    val widgetHost = launcherApp.widgetHost
    val config: StateFlow<Config> = store.config
    val workspace: StateFlow<Workspace> = store.workspace
    val apps: StateFlow<List<AppInfo>> = repo.apps
    val appsLoaded: StateFlow<Boolean> = repo.loaded
    private val awm = AppWidgetManager.getInstance(application)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    /** Rozmiar komórki pulpitu w pikselach (aktualizowany przez siatkę). */
    var cellSizePx: Size = Size(200f, 220f)

    val models = MutableStateFlow<List<AiModel>>(emptyList())
    val modelsLoading = MutableStateFlow(false)
    val modelsError = MutableStateFlow<String?>(null)
    val aiJob = MutableStateFlow<AiJob>(AiJob.Idle)
    val update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val repoSearch = MutableStateFlow<List<RepoHit>>(emptyList())
    val aiSearch = MutableStateFlow<List<String>>(emptyList())
    val aiSearchBusy = MutableStateFlow(false)
    private var aiSearchJob: Job? = null

    private val cols get() = config.value.columns
    private val rows get() = config.value.rows

    init {
        viewModelScope.launch {
            config.map { it.iconShape }.distinctUntilChanged().collect { repo.setShape(it) }
        }
        viewModelScope.launch {
            repo.loaded.first { it }
            if (!workspace.value.initialized) initializeDefaults()
        }
        viewModelScope.launch {
            delay(5000)
            if (config.value.autoUpdate) checkUpdates(silent = true)
        }
    }

    fun toast(msg: String) {
        _messages.tryEmit(msg)
    }

    // ---------------------------------------------------------------- aplikacje

    fun app(key: String?): AppInfo? = key?.let { k -> apps.value.firstOrNull { it.key == k } }

    fun labelOf(app: AppInfo): String = workspace.value.renamed[app.key] ?: app.label

    fun launch(app: AppInfo, view: View?, bounds: Rect?) {
        val rect = bounds?.takeIf { it.width > 0 }?.let {
            android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt())
        }
        val opts = if (view != null && rect != null) {
            ActivityOptions.makeScaleUpAnimation(view, rect.left, rect.top, rect.width(), rect.height()).toBundle()
        } else null
        repo.launch(app, rect, opts)
        store.updateWorkspace { it.copy(usage = it.usage + (app.key to ((it.usage[app.key] ?: 0) + 1))) }
    }

    fun openDetails(app: AppInfo) = repo.openDetails(app)
    fun uninstall(app: AppInfo) = repo.uninstall(app)
    fun shortcuts(app: AppInfo): List<ShortcutEntry> = repo.shortcuts(app)
    fun startShortcut(s: ShortcutEntry) = repo.startShortcut(s.info)

    fun renameApp(key: String, name: String) = store.updateWorkspace {
        val n = name.trim()
        it.copy(renamed = if (n.isEmpty() || n == app(key)?.label) it.renamed - key else it.renamed + (key to n))
    }

    fun setHidden(key: String, hidden: Boolean) = store.updateWorkspace {
        it.copy(hidden = if (hidden) it.hidden + key else it.hidden - key)
    }

    // ---------------------------------------------------------------- konfiguracja

    fun updateConfig(f: (Config) -> Config) = store.updateConfig(f)

    fun setGrid(columns: Int, rows: Int) {
        store.updateConfig { it.copy(columns = columns, rows = rows) }
        store.updateWorkspace { it.copy(pages = GridOps.normalize(it.pages, columns, rows, config.value.allowOverlap)) }
    }

    fun setOverlap(allow: Boolean) {
        store.updateConfig { it.copy(allowOverlap = allow) }
        if (!allow) store.updateWorkspace { it.copy(pages = GridOps.normalize(it.pages, cols, rows, false)) }
    }

    // ---------------------------------------------------------------- pulpit

    private fun Workspace.updatePage(pageId: String, f: (Page) -> Page) =
        copy(pages = pages.map { if (it.id == pageId) f(it) else it })

    private fun Workspace.withoutItem(pageId: String?, itemId: String) =
        if (pageId == null) this else updatePage(pageId) { p -> p.copy(items = p.items.filterNot { it.id == itemId }) }

    /** Wolne miejsce na podanym ekranie albo na kolejnych; tworzy nowy ekran, gdy brak miejsca. */
    private fun Workspace.place(startPage: Int, w: Int, h: Int, fromBottom: Boolean = false): Pair<Workspace, Triple<String, Int, Int>> {
        val order = pages.indices.let { idx -> idx.drop(startPage.coerceIn(0, pages.lastIndex)) + idx.take(startPage.coerceIn(0, pages.lastIndex)) }
        for (i in order) {
            val spot = GridOps.firstFree(pages[i].items, w, h, cols, rows, fromBottom)
            if (spot != null) return this to Triple(pages[i].id, spot.first, spot.second)
        }
        val page = Page()
        return copy(pages = pages + page) to Triple(page.id, 0, 0)
    }

    fun addAppToDesktop(key: String, pageIndex: Int) {
        store.updateWorkspace { ws ->
            val (w2, spot) = ws.place(pageIndex, 1, 1)
            w2.updatePage(spot.first) { p -> p.copy(items = p.items + DesktopItem(type = ItemType.App, x = spot.second, y = spot.third, app = key)) }
        }
        toast("Dodano na pulpit")
    }

    fun addActionToDesktop(action: SystemAction, pageIndex: Int) {
        store.updateWorkspace { ws ->
            val (w2, spot) = ws.place(pageIndex, 1, 1)
            w2.updatePage(spot.first) { p ->
                p.copy(items = p.items + DesktopItem(type = ItemType.Action, x = spot.second, y = spot.third, systemAction = action, label = action.label))
            }
        }
        toast("Dodano na pulpit")
    }

    fun addBuiltinWidget(type: ItemType, pageIndex: Int) {
        val (w, h) = when (type) {
            ItemType.Clock -> cols.coerceAtMost(4).coerceAtLeast(2) to 2
            ItemType.Search -> cols to 1
            else -> 1 to 1
        }
        store.updateWorkspace { ws ->
            val (w2, spot) = ws.place(pageIndex, w, h)
            w2.updatePage(spot.first) { p -> p.copy(items = p.items + DesktopItem(type = type, x = spot.second, y = spot.third, w = w, h = h)) }
        }
    }

    fun addGadget(kind: GadgetKind, pageIndex: Int) {
        val w = kind.w.coerceAtMost(cols)
        val h = kind.h.coerceAtMost(rows)
        store.updateWorkspace { ws ->
            val (w2, spot) = ws.place(pageIndex, w, h)
            w2.updatePage(spot.first) { p ->
                p.copy(items = p.items + DesktopItem(type = ItemType.Gadget, x = spot.second, y = spot.third, w = w, h = h, gadget = kind, gadgetStyle = kind.defaultStyle()))
            }
        }
    }

    fun setGadgetStyle(pageId: String, itemId: String, style: GadgetStyle) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { p -> p.copy(items = p.items.map { if (it.id == itemId) it.copy(gadgetStyle = style) else it }) }
    }

    /** Rozmiar startowy widżetu w komórkach, na podstawie wymagań dostawcy. */
    fun widgetSpan(info: AppWidgetProviderInfo): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 31 && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
            return info.targetCellWidth.coerceIn(1, cols) to info.targetCellHeight.coerceIn(1, rows)
        }
        val w = ceil(info.minWidth / cellSizePx.width.coerceAtLeast(1f)).toInt().coerceIn(1, cols)
        val h = ceil(info.minHeight / cellSizePx.height.coerceAtLeast(1f)).toInt().coerceIn(1, rows)
        return w to h
    }

    /** Rezerwuje miejsce dla widżetu: (pageId, x, y, w, h). */
    fun reserveWidgetSpot(pageIndex: Int, w: Int, h: Int): List<Any> {
        var result: Triple<String, Int, Int>? = null
        store.updateWorkspace { ws ->
            val (w2, spot) = ws.place(pageIndex, w, h)
            result = spot
            w2
        }
        val r = result!!
        return listOf(r.first, r.second, r.third, w, h)
    }

    fun addWidgetItem(pageId: String, x: Int, y: Int, w: Int, h: Int, widgetId: Int, provider: String) {
        store.updateWorkspace { ws ->
            val page = ws.pages.firstOrNull { it.id == pageId }
            val target = if (page == null) ws.pages.first().id else pageId
            ws.updatePage(target) { p ->
                p.copy(items = p.items + DesktopItem(type = ItemType.Widget, x = x, y = y, w = w, h = h, widgetId = widgetId, provider = provider))
            }
        }
    }

    fun providers(): List<AppWidgetProviderInfo> = runCatching {
        val um = getApplication<Application>().getSystemService(android.os.UserManager::class.java)
        um.userProfiles.flatMap { awm.getInstalledProvidersForProfile(it) }
    }.getOrElse { awm.installedProviders }

    fun widgetHasConfiguration(item: DesktopItem): Boolean {
        if (item.type != ItemType.Widget || item.provider == null) return false
        val info = awm.installedProviders.firstOrNull { it.provider.flattenToString() == item.provider }
        return info?.configure != null
    }

    fun configureWidget(activity: android.app.Activity, item: DesktopItem) {
        if (item.type != ItemType.Widget || item.widgetId < 0) return
        val info = awm.installedProviders.firstOrNull { it.provider.flattenToString() == item.provider } ?: return
        if (info.configure == null) return
        runCatching {
            widgetHost.startAppWidgetConfigureActivityForResult(activity, item.widgetId, 0, 5102, null)
        }
    }

    /** Przenosi element na wierzch (koniec listy) albo na spód (początek) – kolejność listy = warstwy. */
    fun moveLayer(pageId: String, itemId: String, toFront: Boolean) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { p ->
            val item = p.items.firstOrNull { it.id == itemId } ?: return@updatePage p
            val rest = p.items - item
            p.copy(items = if (toFront) rest + item else listOf(item) + rest)
        }
    }

    fun setWidgetStretch(pageId: String, itemId: String, stretch: Boolean) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { p -> p.copy(items = p.items.map { if (it.id == itemId) it.copy(stretch = stretch) else it }) }
    }

    fun setSearchStyle(pageId: String, itemId: String, style: SearchStyle) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { p -> p.copy(items = p.items.map { if (it.id == itemId) it.copy(searchStyle = style) else it }) }
    }

    fun removeItem(pageId: String, itemId: String) {
        val item = workspace.value.pages.firstOrNull { it.id == pageId }?.items?.firstOrNull { it.id == itemId }
        if (item?.type == ItemType.Widget && item.widgetId >= 0) runCatching { widgetHost.deleteAppWidgetId(item.widgetId) }
        store.updateWorkspace { it.withoutItem(pageId, itemId) }
    }

    /** Zmiana rozmiaru z dowolnej krawędzi – lewa i górna przesuwają też początek elementu. */
    fun resizeItem(pageId: String, itemId: String, x: Int, y: Int, w: Int, h: Int) {
        val ws = workspace.value
        val page = ws.pages.firstOrNull { it.id == pageId } ?: return
        if (page.items.none { it.id == itemId }) return
        val nx = x.coerceIn(0, cols - 1)
        val ny = y.coerceIn(0, rows - 1)
        val nw = w.coerceIn(1, cols - nx)
        val nh = h.coerceIn(1, rows - ny)
        if (!config.value.allowOverlap && !GridOps.isFree(page.items, nx, ny, nw, nh, cols, rows, itemId)) {
            toast("Brak miejsca na taki rozmiar")
            return
        }
        store.updateWorkspace { it.updatePage(pageId) { p -> p.copy(items = p.items.map { i -> if (i.id == itemId) i.copy(x = nx, y = ny, w = nw, h = nh) else i }) } }
    }

    /** Wyznacza cel upuszczenia na siatce. (x, y) to lewy górny róg, (px, py) – komórka pod palcem. */
    fun cellTarget(page: Page, moving: DesktopItem, x: Int, y: Int, px: Int, py: Int): DropTarget.Cell {
        val w = moving.w.coerceAtMost(cols)
        val h = moving.h.coerceAtMost(rows)
        val cx = x.coerceIn(0, cols - w)
        val cy = y.coerceIn(0, rows - h)
        if (moving.type == ItemType.App) {
            val under = GridOps.itemAt(page.items, px, py, moving.id)
            if (under != null && (under.type == ItemType.App || under.type == ItemType.Folder) && under.app != moving.app) {
                return DropTarget.Cell(page.id, under.x, under.y, under.w, under.h, true, under.id)
            }
        }
        val valid = GridOps.inBounds(cx, cy, w, h, cols, rows) &&
            (config.value.allowOverlap || GridOps.isFree(page.items, cx, cy, w, h, cols, rows, moving.id))
        return DropTarget.Cell(page.id, cx, cy, w, h, valid)
    }

    fun drop(p: DragPayload, t: DropTarget?) {
        val moving = p.item
        when (t) {
            null -> Unit
            is DropTarget.Cell -> {
                if (!t.valid) {
                    toast("Tu nie ma miejsca")
                    return
                }
                store.updateWorkspace { ws ->
                    var w = when (p.source) {
                        DragSource.Desktop -> ws.withoutItem(p.fromPageId, moving.id)
                        DragSource.Dock -> ws.copy(dock = ws.dock - (moving.app ?: ""))
                        DragSource.Drawer -> ws
                    }
                    w = if (t.mergeWith != null) {
                        w.updatePage(t.pageId) { pg -> pg.copy(items = pg.items.map { if (it.id == t.mergeWith) merge(it, moving) else it }) }
                    } else {
                        w.updatePage(t.pageId) { pg -> pg.copy(items = pg.items + moving.copy(x = t.x, y = t.y, w = t.w, h = t.h)) }
                    }
                    w
                }
            }
            is DropTarget.Dock -> {
                val key = when (moving.type) {
                    ItemType.App -> moving.app
                    ItemType.Action -> moving.systemAction?.dockKey
                    else -> null
                } ?: return
                val dock = workspace.value.dock
                if (p.source != DragSource.Dock && dock.size >= config.value.dockCount && key !in dock) {
                    toast("Dok jest pełny")
                    return
                }
                store.updateWorkspace { ws ->
                    val base = when (p.source) {
                        DragSource.Desktop -> ws.withoutItem(p.fromPageId, moving.id)
                        else -> ws
                    }
                    val list = base.dock.filterNot { it == key }.toMutableList()
                    list.add(t.index.coerceIn(0, list.size), key)
                    base.copy(dock = list.take(config.value.dockCount))
                }
            }
            is DropTarget.Zone -> when (t.zone) {
                DropZone.Remove -> when (p.source) {
                    DragSource.Desktop -> p.fromPageId?.let { removeItem(it, moving.id) }
                    DragSource.Dock -> store.updateWorkspace { it.copy(dock = it.dock - (moving.app ?: "")) }
                    DragSource.Drawer -> setHidden(moving.app ?: return, true).also { toast("Ukryto w szufladzie") }
                }
                DropZone.Uninstall -> app(moving.app)?.let(::uninstall)
                DropZone.Info -> app(moving.app)?.let(::openDetails)
            }
        }
    }

    private fun merge(target: DesktopItem, moving: DesktopItem): DesktopItem {
        val key = moving.app ?: return target
        return if (target.type == ItemType.Folder) {
            target.copy(apps = (target.apps + key).distinct())
        } else {
            DesktopItem(
                type = ItemType.Folder, x = target.x, y = target.y, w = target.w, h = target.h,
                apps = listOfNotNull(target.app, key).distinct(), label = "Folder",
            )
        }
    }

    // ---------------------------------------------------------------- foldery

    fun renameItem(pageId: String, itemId: String, name: String) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { p -> p.copy(items = p.items.map { if (it.id == itemId) it.copy(label = name.trim()) else it }) }
    }

    fun addToFolder(pageId: String, folderId: String, keys: List<String>) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { p -> p.copy(items = p.items.map { if (it.id == folderId) it.copy(apps = (it.apps + keys).distinct()) else it }) }
    }

    fun setFolderApps(pageId: String, folderId: String, keys: List<String>) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { p -> p.copy(items = p.items.map { if (it.id == folderId) it.copy(apps = keys.distinct()) else it }) }
    }

    /** Usuwa aplikację z folderu; folder z jedną aplikacją zamienia się z powrotem w ikonę. */
    fun removeFromFolder(pageId: String, folderId: String, key: String, toDesktop: Boolean) = store.updateWorkspace { ws ->
        val page = ws.pages.firstOrNull { it.id == pageId } ?: return@updateWorkspace ws
        val folder = page.items.firstOrNull { it.id == folderId } ?: return@updateWorkspace ws
        val rest = folder.apps - key
        var items = page.items.mapNotNull {
            if (it.id != folderId) it
            else when (rest.size) {
                0 -> null
                // Czysta ikona aplikacji – bez resztek folderu (główna aplikacja, rozmiar)
                1 -> DesktopItem(id = it.id, type = ItemType.App, x = it.x, y = it.y, app = rest.first())
                else -> it.copy(apps = rest, primaryApp = it.primaryApp?.takeIf { p -> p in rest })
            }
        }
        if (toDesktop) {
            val spot = GridOps.findFree(items, 1, 1, cols, rows, folder.x, folder.y)
            if (spot != null) items = items + DesktopItem(type = ItemType.App, x = spot.first, y = spot.second, app = key)
            else {
                val (w2, s) = ws.updatePage(pageId) { it.copy(items = items) }.place(0, 1, 1)
                return@updateWorkspace w2.updatePage(s.first) { p -> p.copy(items = p.items + DesktopItem(type = ItemType.App, x = s.second, y = s.third, app = key)) }
            }
        }
        ws.updatePage(pageId) { it.copy(items = items) }
    }

    /** Rozkłada folder na osobne ikony w jednej zmianie: pierwsza w miejscu folderu, reszta obok. */
    fun ungroup(pageId: String, folderId: String) = store.updateWorkspace { ws ->
        val page = ws.pages.firstOrNull { it.id == pageId } ?: return@updateWorkspace ws
        val folder = page.items.firstOrNull { it.id == folderId && it.type == ItemType.Folder } ?: return@updateWorkspace ws
        val keys = folder.apps.distinct()
        if (keys.isEmpty()) return@updateWorkspace ws.updatePage(pageId) { p -> p.copy(items = p.items - folder) }
        val items = (page.items - folder).toMutableList()
        items += DesktopItem(type = ItemType.App, x = folder.x, y = folder.y, app = keys.first())
        val overflow = mutableListOf<String>()
        for (key in keys.drop(1)) {
            val spot = GridOps.findFree(items, 1, 1, cols, rows, folder.x, folder.y)
            if (spot != null) items += DesktopItem(type = ItemType.App, x = spot.first, y = spot.second, app = key)
            else overflow += key
        }
        var w = ws.updatePage(pageId) { it.copy(items = items) }
        // Nie zmieściło się na tej stronie – na kolejne wolne miejsca
        for (key in overflow) {
            val (w2, s) = w.place(0, 1, 1)
            w = w2.updatePage(s.first) { p -> p.copy(items = p.items + DesktopItem(type = ItemType.App, x = s.second, y = s.third, app = key)) }
        }
        w
    }

    fun setFolderPrimaryApp(pageId: String, folderId: String, appKey: String?) = store.updateWorkspace { ws ->
        ws.updatePage(pageId) { pg ->
            pg.copy(items = pg.items.map { if (it.id == folderId) it.copy(primaryApp = appKey) else it })
        }
    }

    // ---------------------------------------------------------------- dok

    fun addToDock(key: String) {
        val ws = workspace.value
        if (key in ws.dock) return
        if (ws.dock.size >= config.value.dockCount) {
            toast("Dok jest pełny – zwiększ liczbę miejsc w ustawieniach")
            return
        }
        store.updateWorkspace { it.copy(dock = it.dock + key) }
    }

    fun removeFromDock(key: String) = store.updateWorkspace { it.copy(dock = it.dock - key) }

    // ---------------------------------------------------------------- ekrany

    fun addPage(afterIndex: Int): Int {
        var index = 0
        store.updateWorkspace { ws ->
            index = (afterIndex + 1).coerceIn(0, ws.pages.size)
            ws.copy(pages = ws.pages.toMutableList().apply { add(index, Page()) })
        }
        return index
    }

    fun addPageAtEnd(): String {
        val page = Page()
        store.updateWorkspace { it.copy(pages = it.pages + page) }
        return page.id
    }

    fun removePage(index: Int) {
        val ws = workspace.value
        val page = ws.pages.getOrNull(index) ?: return
        page.items.filter { it.type == ItemType.Widget }.forEach { runCatching { widgetHost.deleteAppWidgetId(it.widgetId) } }
        store.updateWorkspace { w ->
            val rest = w.pages.filterNot { it.id == page.id }
            w.copy(pages = rest.ifEmpty { listOf(Page()) })
        }
        val def = config.value.defaultPage
        if (def >= index && def > 0) store.updateConfig { it.copy(defaultPage = def - 1) }
    }

    fun removePageIfEmpty(pageId: String) {
        val ws = workspace.value
        val page = ws.pages.firstOrNull { it.id == pageId } ?: return
        if (page.items.isEmpty() && ws.pages.size > 1) removePage(ws.pages.indexOf(page))
    }

    fun movePage(index: Int, dir: Int): Int {
        val ws = workspace.value
        val target = index + dir
        if (index !in ws.pages.indices || target !in ws.pages.indices) return index
        store.updateWorkspace { w ->
            val list = w.pages.toMutableList()
            val p = list.removeAt(index)
            list.add(target, p)
            w.copy(pages = list)
        }
        val def = config.value.defaultPage
        when (def) {
            index -> store.updateConfig { it.copy(defaultPage = target) }
            target -> store.updateConfig { it.copy(defaultPage = index) }
        }
        return target
    }

    fun setDefaultPage(index: Int) = store.updateConfig { it.copy(defaultPage = index) }

    // ---------------------------------------------------------------- kategorie

    fun addCategory(name: String, emoji: String): String {
        val c = Category(name = name.trim(), emoji = emoji.trim(), color = palette[workspace.value.categories.size % palette.size])
        store.updateWorkspace { it.copy(categories = it.categories + c) }
        return c.id
    }

    fun updateCategory(id: String, name: String, emoji: String) = store.updateWorkspace { ws ->
        ws.copy(categories = ws.categories.map { if (it.id == id) it.copy(name = name.trim(), emoji = emoji.trim()) else it })
    }

    fun deleteCategory(id: String) = store.updateWorkspace { ws ->
        ws.copy(
            categories = ws.categories.filterNot { it.id == id },
            assignments = ws.assignments.mapValues { (_, v) -> v - id }.filterValues { it.isNotEmpty() },
        )
    }

    fun moveCategory(id: String, dir: Int) = store.updateWorkspace { ws ->
        val i = ws.categories.indexOfFirst { it.id == id }
        val j = i + dir
        if (i < 0 || j !in ws.categories.indices) return@updateWorkspace ws
        val list = ws.categories.toMutableList()
        val c = list.removeAt(i)
        list.add(j, c)
        ws.copy(categories = list)
    }

    fun setAppCategories(key: String, ids: List<String>) = store.updateWorkspace { ws ->
        ws.copy(assignments = if (ids.isEmpty()) ws.assignments - key else ws.assignments + (key to ids.distinct()))
    }

    fun setCategoryApps(categoryId: String, keys: Set<String>) = store.updateWorkspace { ws ->
        val all = ws.assignments.keys + keys
        val map = all.associateWith { k ->
            val cur = ws.assignments[k].orEmpty()
            if (k in keys) (cur + categoryId).distinct() else cur - categoryId
        }.filterValues { it.isNotEmpty() }
        ws.copy(assignments = map)
    }

    fun autoCategorize(replace: Boolean) {
        val (cats, assign) = AutoCategories.generate(apps.value)
        applyCategories(cats, assign, replace)
        toast("Utworzono ${cats.size} kategorii")
    }

    private fun applyCategories(cats: List<Category>, assign: Map<String, List<String>>, replace: Boolean) {
        store.updateWorkspace { ws ->
            if (replace) return@updateWorkspace ws.copy(categories = cats, assignments = assign)
            // Scalanie: kategorie o tej samej nazwie łączymy
            val byName = ws.categories.associateBy { it.name.lowercase() }
            val idMap = HashMap<String, String>()
            val added = ArrayList<Category>()
            for (c in cats) {
                val existing = byName[c.name.lowercase()]
                if (existing != null) idMap[c.id] = existing.id else { idMap[c.id] = c.id; added += c }
            }
            val merged = HashMap(ws.assignments)
            for ((k, ids) in assign) merged[k] = (merged[k].orEmpty() + ids.map { idMap[it] ?: it }).distinct()
            ws.copy(categories = ws.categories + added, assignments = merged)
        }
    }

    // ---------------------------------------------------------------- AI

    fun loadModels(force: Boolean = false) {
        if (modelsLoading.value || (models.value.isNotEmpty() && !force)) return
        viewModelScope.launch {
            modelsLoading.value = true
            modelsError.value = null
            runCatching { OpenRouter.models() }
                .onSuccess { models.value = it }
                .onFailure { modelsError.value = it.message ?: "Nie udało się pobrać modeli" }
            modelsLoading.value = false
        }
    }

    fun testKey(onResult: (String) -> Unit) {
        val key = config.value.openRouterKey
        if (key.isBlank()) return onResult("Wpisz klucz API")
        viewModelScope.launch {
            onResult(runCatching { OpenRouter.checkKey(key) }.getOrElse { "Błąd: ${it.message}" })
        }
    }

    val aiReady: Boolean get() = config.value.openRouterKey.isNotBlank() && config.value.aiModel.isNotBlank()

    private fun aiRefs(list: List<AppInfo> = apps.value) =
        list.distinctBy { it.packageName }.map { AiAppRef(it.packageName, labelOf(it)) }

    fun runAiCategorize(instructions: String, maxCategories: Int, multi: Boolean) {
        val cfg = config.value
        if (!aiReady) {
            aiJob.value = AiJob.Error("Ustaw klucz OpenRouter i wybierz model w ustawieniach AI")
            return
        }
        viewModelScope.launch {
            aiJob.value = AiJob.Running("Model ${cfg.aiModelName.ifBlank { cfg.aiModel }} układa kategorie…")
            runCatching {
                OpenRouter.categorize(
                    cfg.openRouterKey, cfg.aiModel, aiRefs(), instructions, maxCategories, multi,
                    workspace.value.categories.map { it.name },
                )
            }.onSuccess { aiJob.value = if (it.isEmpty()) AiJob.Error("Model nie zaproponował kategorii") else AiJob.Result(it) }
                .onFailure { aiJob.value = AiJob.Error(it.message ?: "Błąd AI") }
        }
    }

    fun applyAiResult(result: List<AiCategory>, replace: Boolean) {
        val byPackage = apps.value.groupBy { it.packageName }
        val cats = ArrayList<Category>()
        val assign = HashMap<String, MutableList<String>>()
        result.forEachIndexed { i, ai ->
            val c = Category(name = ai.name.trim(), emoji = ai.emoji.trim(), color = palette[i % palette.size], source = "ai")
            cats += c
            for (pkg in ai.apps) byPackage[pkg.trim()]?.forEach { app -> assign.getOrPut(app.key) { ArrayList() } += c.id }
        }
        applyCategories(cats, assign, replace)
        aiJob.value = AiJob.Idle
        toast("Zastosowano ${cats.size} kategorii od AI")
    }

    fun dismissAi() {
        aiJob.value = AiJob.Idle
    }

    fun suggestFolderName(labels: List<String>, onResult: (String) -> Unit) {
        val cfg = config.value
        if (!aiReady) return toast("Skonfiguruj AI w ustawieniach")
        viewModelScope.launch {
            runCatching { OpenRouter.folderName(cfg.openRouterKey, cfg.aiModel, labels) }
                .onSuccess { onResult(it) }
                .onFailure { toast("AI: ${it.message}") }
        }
    }

    fun aiFindApp(query: String) {
        val cfg = config.value
        if (!aiReady || query.isBlank()) return
        aiSearchJob?.cancel()
        aiSearchJob = viewModelScope.launch {
            aiSearchBusy.value = true
            runCatching { OpenRouter.findApp(cfg.openRouterKey, cfg.aiModel, aiRefs(), query) }
                .onSuccess { pkgs -> aiSearch.value = pkgs.mapNotNull { p -> apps.value.firstOrNull { it.packageName == p }?.key } }
                .onFailure { toast("AI: ${it.message}") }
            aiSearchBusy.value = false
        }
    }

    fun clearAiSearch() {
        aiSearchJob?.cancel()
        aiSearch.value = emptyList()
        aiSearchBusy.value = false
    }

    // ---------------------------------------------------------------- aktualizacje

    fun checkUpdates(silent: Boolean = false) {
        if (update.value is UpdateState.Checking || update.value is UpdateState.Downloading) return
        viewModelScope.launch {
            update.value = UpdateState.Checking
            runCatching { Updater.check(config.value.updateRepo) }
                .onSuccess { info ->
                    update.value = when {
                        info == null -> if (silent) UpdateState.Idle else UpdateState.UpToDate
                        silent && info.versionCode == config.value.skippedVersion -> UpdateState.Idle
                        else -> UpdateState.Available(info)
                    }
                }
                .onFailure { update.value = if (silent) UpdateState.Idle else UpdateState.Error(it.message ?: "Błąd sieci") }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        viewModelScope.launch {
            update.value = UpdateState.Downloading(info, 0f)
            runCatching {
                Updater.download(getApplication(), info) { p -> update.value = UpdateState.Downloading(info, p) }
            }.onSuccess { update.value = UpdateState.Ready(info, it) }
                .onFailure { update.value = UpdateState.Error(it.message ?: "Nie udało się pobrać") }
        }
    }

    fun skipUpdate(info: UpdateInfo) {
        store.updateConfig { it.copy(skippedVersion = info.versionCode) }
        update.value = UpdateState.Idle
    }

    fun dismissUpdate() {
        update.value = UpdateState.Idle
    }

    fun searchRepos(query: String) {
        viewModelScope.launch {
            runCatching { Updater.searchRepos(query) }
                .onSuccess { repoSearch.value = it }
                .onFailure { toast("GitHub: ${it.message}") }
        }
    }

    // ---------------------------------------------------------------- kopia zapasowa

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(store.exportBackup().toByteArray()) }
                }
            }.onSuccess { toast("Zapisano kopię układu") }.onFailure { toast("Błąd zapisu: ${it.message}") }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } ?: error("Nie można odczytać pliku")
                store.importBackup(text)
            }.onSuccess { toast("Przywrócono układ") }.onFailure { toast("Błąd importu: ${it.message}") }
        }
    }

    fun resetLayout() {
        workspace.value.pages.flatMap { it.items }.filter { it.type == ItemType.Widget }
            .forEach { runCatching { widgetHost.deleteAppWidgetId(it.widgetId) } }
        store.updateWorkspace { it.copy(initialized = false) }
        initializeDefaults()
    }

    // ---------------------------------------------------------------- Google Drive

    fun completeOnboarding() {
        store.updateConfig { it.copy(onboardingDone = true) }
    }

    fun connectGoogleDrive() {
        // TODO: Implement Google Drive connection flow
        // Będzie wymagało Activity Result API
        toast("Połączenie z Google Drive - wkrótce!")
        completeOnboarding()
    }

    fun disconnectDrive() {
        // TODO: Implement disconnect
        toast("Rozłączono z Google Drive")
    }

    fun syncNow() {
        viewModelScope.launch {
            val success = store.syncToCloud()
            if (success) toast("Zsynchronizowano z Google Drive")
            else toast("Błąd synchronizacji")
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            val success = store.createCloudBackup()
            if (success) toast("Utworzono backup w Google Drive")
            else toast("Błąd tworzenia backupu")
        }
    }

    // ---------------------------------------------------------------- pierwszy start

    private fun initializeDefaults() {
        val list = apps.value
        val pm = getApplication<Application>().packageManager
        fun pkgFor(intent: Intent): String? = runCatching {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }.getOrNull()?.takeIf { it != "android" }

        fun keyFor(pkg: String?) = pkg?.let { p -> list.firstOrNull { it.packageName == p && !it.isWork }?.key }

        val dock = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
        ).mapNotNull { keyFor(pkgFor(it)) }.distinct().take(config.value.dockCount)

        val c = cols
        val r = rows
        val items = mutableListOf(
            DesktopItem(type = ItemType.Clock, x = 0, y = 0, w = c, h = 2),
            DesktopItem(type = ItemType.Search, x = 0, y = 2, w = c, h = 1),
        )
        val favourites = listOf(
            "com.android.vending", "com.google.android.apps.maps", "com.google.android.youtube",
            "com.google.android.gm", "com.android.settings", "com.google.android.calendar",
            "com.spotify.music", "com.whatsapp", "com.google.android.apps.photos", "com.android.chrome",
        ).mapNotNull { keyFor(it) }.filter { it !in dock }.take(c)
        favourites.forEachIndexed { i, key -> items += DesktopItem(type = ItemType.App, x = i, y = r - 1, app = key) }

        val (cats, assign) = AutoCategories.generate(list)
        store.updateWorkspace {
            it.copy(
                pages = GridOps.normalize(listOf(Page(items = items), Page()), c, r, false),
                dock = dock, categories = cats, assignments = assign, initialized = true,
            )
        }
    }

    companion object {
        val palette = listOf(
            0xFF8AB4FF, 0xFFC58AFF, 0xFF7EE0C3, 0xFFFFD27E, 0xFFFF8A80, 0xFF80D8FF,
            0xFFB9F6CA, 0xFFFFAB91, 0xFFCFD8DC, 0xFFF48FB1, 0xFFA5D6A7, 0xFFFFE082,
        )
    }
}
