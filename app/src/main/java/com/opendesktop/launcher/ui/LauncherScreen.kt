package com.opendesktop.launcher.ui

import android.app.WallpaperManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.FlipToFront
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.opendesktop.launcher.ui.components.iconSizeFor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.appwidget.AppWidgetProviderInfo
import com.opendesktop.launcher.R
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.DrawerAnimation
import com.opendesktop.launcher.data.GestureAction
import com.opendesktop.launcher.data.ItemType
import com.opendesktop.launcher.data.PageTransition
import com.opendesktop.launcher.data.ShortcutEntry
import com.opendesktop.launcher.data.SystemAction
import com.opendesktop.launcher.data.SystemActions
import com.opendesktop.launcher.data.findActivity
import com.opendesktop.launcher.ui.categories.AssignCategoriesDialog
import com.opendesktop.launcher.ui.categories.CategoriesScreen
import com.opendesktop.launcher.ui.components.ActionMenu
import com.opendesktop.launcher.ui.components.icon
import com.opendesktop.launcher.ui.components.AppPickerDialog
import com.opendesktop.launcher.ui.components.MenuAction
import com.opendesktop.launcher.ui.components.MenuShortcut
import com.opendesktop.launcher.ui.components.TextInputDialog
import com.opendesktop.launcher.ui.desktop.DesktopCallbacks
import com.opendesktop.launcher.ui.desktop.DesktopGrid
import com.opendesktop.launcher.ui.desktop.Dock
import com.opendesktop.launcher.ui.desktop.DragController
import com.opendesktop.launcher.ui.desktop.DragPayload
import com.opendesktop.launcher.ui.desktop.DragPreview
import com.opendesktop.launcher.ui.desktop.DragSource
import com.opendesktop.launcher.ui.desktop.DropTarget
import com.opendesktop.launcher.ui.desktop.DropZone
import com.opendesktop.launcher.ui.desktop.FolderDialog
import com.opendesktop.launcher.ui.desktop.PageIndicator
import com.opendesktop.launcher.ui.desktop.SearchStyleDialog
import com.opendesktop.launcher.ui.desktop.WidgetPicker
import com.opendesktop.launcher.ui.desktop.pageTransition
import com.opendesktop.launcher.ui.drawer.AppDrawer
import com.opendesktop.launcher.ui.onboarding.OnboardingScreen
import com.opendesktop.launcher.ui.settings.SettingsScreen
import com.opendesktop.launcher.ui.settings.UpdateDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class Overlay { None, Settings, Categories }

/**
 * Przytrzymanie przy zablokowanym układzie: najpierw kłódka. [menu] – menu elementu dostępne
 * bez odblokowania (skróty, informacje), [selectId] – element zaznaczany po odblokowaniu.
 */
private data class LockPrompt(val title: String?, val menu: MenuTarget? = null, val selectId: String? = null)

private sealed interface MenuTarget {
    data class Desktop(val pageId: String, val itemId: String) : MenuTarget
    data class Dock(val key: String) : MenuTarget
    data class Drawer(val key: String) : MenuTarget
}

@Composable
fun LauncherScreen(
    vm: LauncherViewModel,
    homePressed: SharedFlow<Unit>,
    onAddWidget: (AppWidgetProviderInfo, Int) -> Unit,
) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val ws by vm.workspace.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val loaded by vm.appsLoaded.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()

    // Pokaż onboarding przy pierwszym uruchomieniu
    if (!cfg.onboardingDone) {
        OnboardingScreen(
            onComplete = { vm.completeOnboarding() },
            onConnectDrive = { vm.connectGoogleDrive() },
        )
        return
    }

    val appMap = remember(apps) { apps.associateBy { it.key } }
    val ctx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = cfg.defaultPage.coerceIn(0, (ws.pages.size - 1).coerceAtLeast(0))) { ws.pages.size }
    val drag = remember { DragController() }
    val drawer = remember { Animatable(0f) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var editMode by remember { mutableStateOf(false) }
    var lockPrompt by remember { mutableStateOf<LockPrompt?>(null) }
    var overlay by remember { mutableStateOf(Overlay.None) }
    var menu by remember { mutableStateOf<MenuTarget?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var openFolder by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Miejsce ikony folderu na ekranie — okienko otwiera się przy niej (Rect.Zero = na środku)
    var folderAnchor by remember { mutableStateOf(Rect.Zero) }
    // Kategorie otwarte z ustawień wracają do ustawień, otwarte z szuflady – na pulpit
    var categoriesFromSettings by remember { mutableStateOf(false) }
    fun closeCategories() {
        overlay = if (categoriesFromSettings) Overlay.Settings else Overlay.None
        categoriesFromSettings = false
    }
    var folderPicker by remember { mutableStateOf<Pair<String, String>?>(null) }
    var renameFolder by remember { mutableStateOf<Pair<String, String>?>(null) }
    var searchStyleFor by remember { mutableStateOf<Pair<String, String>?>(null) } // pageId to itemId
    var primaryAppPicker by remember { mutableStateOf<Pair<String, String>?>(null) } // pageId to itemId
    var assignFor by remember { mutableStateOf<AppInfo?>(null) }
    var renameApp by remember { mutableStateOf<AppInfo?>(null) }
    var dockPicker by remember { mutableStateOf(false) }
    var widgetPicker by remember { mutableStateOf(false) }
    var actionPicker by remember { mutableStateOf(false) }
    var confirmRemovePage by remember { mutableStateOf<Int?>(null) }
    var searchFocus by remember { mutableIntStateOf(0) }
    var showOnboarding by remember { mutableStateOf(false) }

    val labelOf: (AppInfo) -> String = { ws.renamed[it.key] ?: it.label }
    val heightPx = rootSize.height.toFloat().coerceAtLeast(1f)

    fun vibrate() {
        if (cfg.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun openDrawer(focus: Boolean = false) {
        menu = null
        scope.launch { drawer.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 420f)) }
        if (focus || cfg.searchAutoFocus) searchFocus++
    }

    fun closeDrawer() {
        menu = null
        scope.launch { drawer.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 480f)) }
    }

    /**
     * Wejście w tryb edycji (przytrzymanie elementu, pustego miejsca albo gest). Zablokowany układ
     * pokazuje najpierw kłódkę – edycja otwiera się dopiero po odblokowaniu.
     */
    fun requestEdit(menuTarget: MenuTarget? = null, select: String? = null, title: String? = null) {
        vibrate()
        if (cfg.layoutLocked) {
            menu = null
            lockPrompt = LockPrompt(title, menuTarget, select)
            return
        }
        editMode = true
        selected = select
        menu = menuTarget
    }

    fun runGesture(a: GestureAction) {
        when (a) {
            GestureAction.None -> Unit
            GestureAction.Drawer -> openDrawer()
            GestureAction.Search -> openDrawer(focus = true)
            GestureAction.Notifications -> SystemActions.expandNotifications(ctx)
            GestureAction.QuickSettings -> SystemActions.openQuickSettings(ctx)
            GestureAction.EditMode -> requestEdit()
            GestureAction.Settings -> overlay = Overlay.Settings
        }
    }

    fun runAction(a: SystemAction) {
        when (a) {
            SystemAction.OpenDrawer -> openDrawer()
            SystemAction.OpenSearch -> openDrawer(focus = true)
            SystemAction.OpenNotifications -> SystemActions.expandNotifications(ctx)
            SystemAction.OpenQuickSettings -> SystemActions.openQuickSettings(ctx)
            SystemAction.OpenSettings -> overlay = Overlay.Settings
        }
    }

    fun launch(app: AppInfo, bounds: Rect) {
        vm.launch(app, view, bounds)
        if (drawer.value > 0f) scope.launch { delay(350); drawer.snapTo(0f) }
    }

    LaunchedEffect(loaded) {
        if (loaded && !cfg.onboardingDone && !SystemActions.isDefaultLauncher(ctx)) showOnboarding = true
    }

    // Przycisk Home: zamyka wszystko i wraca na ekran główny
    LaunchedEffect(Unit) {
        homePressed.collect {
            val wasIdle = drawer.value == 0f && !editMode && overlay == Overlay.None && openFolder == null && lockPrompt == null
            menu = null; openFolder = null; editMode = false; selected = null; overlay = Overlay.None; lockPrompt = null
            closeDrawer()
            val home = cfg.defaultPage.coerceIn(0, (ws.pages.size - 1).coerceAtLeast(0))
            if (wasIdle || pagerState.currentPage != home) pagerState.animateScrollToPage(home)
        }
    }

    // Przewijanie tapety razem z ekranami
    // Restart także przy zmianie rozmiaru okna (składany telefon) – system przelicza wtedy tapetę
    // i trzeba mu od razu podać bieżące położenie, inaczej tapeta „odskakuje” do złego miejsca
    LaunchedEffect(cfg.wallpaperScroll, ws.pages.size, rootSize) {
        val wm = WallpaperManager.getInstance(ctx)
        val token = view.windowToken ?: return@LaunchedEffect
        val n = ws.pages.size
        if (!cfg.wallpaperScroll || n <= 1) {
            // Wyłączone (albo jeden ekran) – tapeta na środku, a nie tam, gdzie ostatnio była
            runCatching { wm.setWallpaperOffsetSteps(0f, 0f); wm.setWallpaperOffsets(token, 0.5f, 0.5f) }
            return@LaunchedEffect
        }
        // Pełna szerokość tapety dopiero przy 5 ekranach; przy mniejszej liczbie tapeta przesuwa się
        // proporcjonalnie mniej, wokół środka (2 ekrany – 25% szerokości, 3 – 50%)
        val span = ((n - 1) / 4f).coerceAtMost(1f)
        val start = 0.5f - span / 2
        runCatching { wm.setWallpaperOffsetSteps(span / (n - 1), 0f) }
        snapshotFlow { (pagerState.currentPage + pagerState.currentPageOffsetFraction) / (n - 1) }
            .collect { x -> runCatching { wm.setWallpaperOffsets(token, start + span * x.coerceIn(0f, 1f), 0.5f) } }
    }

    // ------------------------------------------------------------------ cel upuszczenia
    val dropTarget by remember {
        derivedStateOf {
            val p = drag.payload ?: return@derivedStateOf null
            val ptr = drag.pointer
            drag.zoneBounds.entries.firstOrNull { it.value.contains(ptr) }?.let { return@derivedStateOf DropTarget.Zone(it.key) }
            if (cfg.dockEnabled && drag.dockBounds.contains(ptr) && (p.item.type == ItemType.App || p.item.type == ItemType.Action)) {
                val slot = drag.dockBounds.width / cfg.dockCount
                return@derivedStateOf DropTarget.Dock(((ptr.x - drag.dockBounds.left) / slot).toInt().coerceIn(0, cfg.dockCount - 1))
            }
            val page = ws.pages.getOrNull(pagerState.currentPage) ?: return@derivedStateOf null
            val b = drag.gridBounds[page.id] ?: return@derivedStateOf null
            if (!b.contains(ptr)) return@derivedStateOf null
            val cw = b.width / cfg.columns
            val ch = b.height / cfg.rows
            val topLeft = ptr - drag.grab
            val x = ((topLeft.x - b.left) / cw).roundToInt()
            val y = ((topLeft.y - b.top) / ch).roundToInt()
            val px = ((ptr.x - b.left) / cw).toInt().coerceIn(0, cfg.columns - 1)
            val py = ((ptr.y - b.top) / ch).toInt().coerceIn(0, cfg.rows - 1)
            vm.cellTarget(page, p.item, x, y, px, py)
        }
    }

    fun startDrag(source: DragSource, item: DesktopItem, fromPageId: String?, bounds: Rect) {
        if (cfg.layoutLocked) {
            // Z pulpitu i doku kłódka jest już na ekranie (pokazało ją przytrzymanie)
            if (source == DragSource.Drawer) {
                menu = null
                vm.toast("Układ jest zablokowany – przytrzymaj pulpit, aby go odblokować")
            }
            return
        }
        menu = null
        vibrate()
        drag.zoneBounds.clear()
        val size: Size
        if (source == DragSource.Desktop) {
            size = bounds.size
            drag.grab = drag.pointer - bounds.topLeft
        } else {
            val page = ws.pages.getOrNull(pagerState.currentPage)
            val b = page?.let { drag.gridBounds[it.id] }
            size = if (b != null) Size(b.width / cfg.columns, b.height / cfg.rows) else bounds.size
            drag.grab = Offset(size.width / 2, size.height / 2)
        }
        drag.payload = DragPayload(source, item, fromPageId, size)
        if (source == DragSource.Drawer) closeDrawer()
    }

    fun endDrag() {
        val p = drag.payload ?: return
        val target = dropTarget
        vm.drop(p, target)
        drag.payload = null
        drag.autoPageId?.let { id -> vm.removePageIfEmpty(id) }
        drag.autoPageId = null
    }

    // Przytrzymanie przy krawędzi przenosi na sąsiedni ekran (lub tworzy nowy)
    LaunchedEffect(drag.payload != null) {
        if (drag.payload == null) return@LaunchedEffect
        val edge = with(density) { 26.dp.toPx() }
        var since = 0L
        while (drag.payload != null) {
            delay(60)
            val x = drag.pointer.x
            val w = rootSize.width.toFloat()
            val dir = when {
                x < edge -> -1
                x > w - edge -> 1
                else -> 0
            }
            if (dir == 0) {
                since = 0L
                continue
            }
            val now = System.currentTimeMillis()
            if (since == 0L) since = now
            if (now - since > 520) {
                val target = pagerState.currentPage + dir
                if (target >= ws.pages.size && drag.autoPageId == null && drag.payload?.item?.type != null) {
                    drag.autoPageId = vm.addPageAtEnd()
                    delay(32)
                }
                if (target in 0 until ws.pages.size) {
                    vibrate()
                    pagerState.animateScrollToPage(target)
                }
                since = System.currentTimeMillis() + 300
            }
        }
    }

    BackHandler(enabled = drawer.targetValue > 0f || drawer.value > 0f || editMode || overlay != Overlay.None || openFolder != null || menu != null || lockPrompt != null) {
        when {
            lockPrompt != null -> lockPrompt = null
            menu != null -> menu = null
            openFolder != null -> openFolder = null
            overlay == Overlay.Categories -> closeCategories()
            overlay != Overlay.None -> overlay = Overlay.None
            drawer.value > 0f -> closeDrawer()
            editMode -> { editMode = false; selected = null }
        }
    }

    // ------------------------------------------------------------------ menu kontekstowe
    @Composable
    fun AppMenu(app: AppInfo, extra: List<MenuAction>) {
        val shortcuts by produceState(emptyList<ShortcutEntry>(), app.key) {
            value = withContext(Dispatchers.IO) { vm.shortcuts(app) }
        }
        ActionMenu(
            title = labelOf(app),
            onDismiss = { menu = null },
            shortcuts = shortcuts.map { s -> MenuShortcut(s.label, s.icon) { vm.startShortcut(s) } },
            // Zablokowany układ: tylko skróty, informacje i odinstalowanie – nic, co zmienia układ
            actions = (if (cfg.layoutLocked) emptyList() else extra) + listOfNotNull(
                if (cfg.layoutLocked) null else MenuAction("Kategorie…", Icons.Rounded.Category) { assignFor = app },
                if (cfg.layoutLocked) null else MenuAction("Zmień nazwę", Icons.Rounded.Edit) { renameApp = app },
                MenuAction("Informacje o aplikacji", Icons.Rounded.Info) { vm.openDetails(app) },
                if (!app.isSystem) MenuAction("Odinstaluj", Icons.Rounded.DeleteForever, danger = true) { vm.uninstall(app) } else null,
            ),
        )
    }

    val desktopMenu: @Composable (DesktopItem) -> Unit = { item ->
        val target = menu as? MenuTarget.Desktop
        if (target != null) {
            val pageId = target.pageId
            // Przy zablokowanym układzie znikają akcje, które go zmieniają (przenoszenie, warstwy, usuwanie)
            val locked = cfg.layoutLocked
            val resize = if (locked) null else MenuAction("Przenieś / zmień rozmiar", Icons.Rounded.Transform) { editMode = true; selected = item.id }
            val remove = if (locked) null else MenuAction("Usuń z pulpitu", Icons.Rounded.DeleteOutline) { vm.removeItem(pageId, item.id) }
            // Warstwy: przydatne, gdy elementy na siebie zachodzą
            val page = ws.pages.firstOrNull { it.id == pageId }
            val layers = if (locked) emptyList() else listOfNotNull(
                if (page != null && page.items.lastOrNull()?.id != item.id) MenuAction("Na wierzch", Icons.Rounded.FlipToFront) { vm.moveLayer(pageId, item.id, toFront = true) } else null,
                if (page != null && page.items.firstOrNull()?.id != item.id) MenuAction("Na spód", Icons.Rounded.FlipToBack) { vm.moveLayer(pageId, item.id, toFront = false) } else null,
            )
            when (item.type) {
                ItemType.App -> appMap[item.app]?.let { AppMenu(it, listOfNotNull(resize) + layers + listOfNotNull(remove)) }
                ItemType.Folder -> ActionMenu(
                    title = item.label.ifBlank { "Folder" },
                    onDismiss = { menu = null },
                    actions = listOfNotNull(
                        MenuAction("Otwórz folder", Icons.Rounded.FolderOpen) { folderAnchor = Rect.Zero; openFolder = pageId to item.id },
                        if (locked) null else MenuAction("Zmień nazwę", Icons.Rounded.Edit) { renameFolder = pageId to item.id },
                        if (locked) null else MenuAction("Dodaj aplikacje", Icons.Rounded.Add) { folderPicker = pageId to item.id },
                        if (locked) null else MenuAction(
                            if (item.primaryApp != null) "Zmień główną aplikację" else "Ustaw główną aplikację",
                            Icons.Rounded.Settings
                        ) { primaryAppPicker = pageId to item.id },
                        resize,
                    ) + layers + if (locked) emptyList() else listOf(
                        MenuAction("Rozgrupuj", Icons.Rounded.CallSplit) { vm.ungroup(pageId, item.id) },
                        MenuAction("Usuń folder", Icons.Rounded.Delete, danger = true) { vm.removeItem(pageId, item.id) },
                    ),
                )
                else -> ActionMenu(
                    title = when (item.type) {
                        ItemType.Clock -> "Zegar"
                        ItemType.Search -> "Wyszukiwarka"
                        ItemType.Action -> item.label.ifBlank { item.systemAction?.label ?: "Akcja" }
                        else -> "Widżet"
                    },
                    onDismiss = { menu = null },
                    actions = listOfNotNull(
                        resize,
                        // Wygląd nie zmienia układu – dostępny także przy blokadzie
                        if (item.type == ItemType.Search) MenuAction("Wygląd…", Icons.Rounded.Palette) { searchStyleFor = pageId to item.id } else null,
                        if (item.type == ItemType.Widget && vm.widgetHasConfiguration(item)) {
                            MenuAction("Skonfiguruj", Icons.Rounded.Settings) { ctx.findActivity()?.let { vm.configureWidget(it, item) } }
                        } else null,
                        if (item.type == ItemType.Widget && !locked) {
                            MenuAction(if (item.stretch) "Bez rozciągania" else "Rozciągnij na cały kafelek", Icons.Rounded.OpenInFull) {
                                vm.setWidgetStretch(pageId, item.id, !item.stretch)
                            }
                        } else null,
                    ) + layers + listOfNotNull(if (locked) null else MenuAction("Usuń", Icons.Rounded.Delete, danger = true) { vm.removeItem(pageId, item.id) }),
                )
            }
        }
    }

    val dockMenu: @Composable (AppInfo) -> Unit = { app ->
        AppMenu(app, listOf(MenuAction("Usuń z doku", Icons.Rounded.DeleteOutline) { vm.removeFromDock(app.key) }))
    }

    val drawerMenu: @Composable (AppInfo) -> Unit = { app ->
        val hidden = app.key in ws.hidden
        AppMenu(
            app,
            listOf(
                MenuAction("Dodaj na pulpit", Icons.Rounded.AddToHomeScreen) { vm.addAppToDesktop(app.key, pagerState.currentPage) },
                MenuAction("Dodaj do doku", Icons.Rounded.Dock) { vm.addToDock(app.key) },
                MenuAction(if (hidden) "Pokaż w szufladzie" else "Ukryj w szufladzie", if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff) {
                    vm.setHidden(app.key, !hidden)
                },
            ),
        )
    }

    val callbacks = object : DesktopCallbacks {
        override fun onItemTap(pageId: String, item: DesktopItem, bounds: Rect) {
            menu = null
            if (editMode) {
                selected = if (selected == item.id) null else item.id
                return
            }
            when (item.type) {
                ItemType.App -> appMap[item.app]?.let { launch(it, bounds) }
                ItemType.Folder -> {
                    // Jeśli folder ma primaryApp, uruchom ją, inaczej otwórz folder
                    if (item.primaryApp != null) {
                        appMap[item.primaryApp]?.let { launch(it, bounds) }
                    } else {
                        folderAnchor = bounds
                        openFolder = pageId to item.id
                    }
                }
                ItemType.Clock -> SystemActions.openClock(ctx)
                ItemType.Search -> openDrawer(focus = true)
                ItemType.Widget -> Unit
                ItemType.Action -> item.systemAction?.let(::runAction)
            }
        }

        override fun onItemLongPress(pageId: String, item: DesktopItem) {
            // Widżety, foldery itp. od razu z ramką rozmiaru; ikony aplikacji i akcji tylko z menu
            val select = item.id.takeIf { item.type != ItemType.App && item.type != ItemType.Action }
            val title = when (item.type) {
                ItemType.App -> appMap[item.app]?.let(labelOf)
                ItemType.Folder -> item.label.ifBlank { "Folder" }
                ItemType.Clock -> "Zegar"
                ItemType.Search -> "Wyszukiwarka"
                ItemType.Action -> item.label.ifBlank { item.systemAction?.label }
                ItemType.Widget -> "Widżet"
            }
            requestEdit(MenuTarget.Desktop(pageId, item.id), select, title)
        }

        override fun onOpenFolder(pageId: String, item: DesktopItem, bounds: Rect) {
            menu = null
            folderAnchor = bounds
            openFolder = pageId to item.id
        }

        override fun onDragStart(pageId: String, item: DesktopItem, bounds: Rect) {
            selected = null
            startDrag(DragSource.Desktop, item, pageId, bounds)
        }

        override fun onDragEnd() = endDrag()

        override fun onResize(pageId: String, item: DesktopItem, x: Int, y: Int, w: Int, h: Int) {
            if (!cfg.layoutLocked) vm.resizeItem(pageId, item.id, x, y, w, h)
        }

        override fun onEmptyLongPress() = requestEdit()

        override fun onEmptyDoubleTap() {
            if (!editMode) runGesture(cfg.doubleTap)
        }

        override fun onEmptyTap() {
            menu = null
            selected = null
        }
    }

    // Zamknięcie szuflady przeciągnięciem listy w dół
    val closeConnection = remember(heightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && drawer.value < 1f && drawer.value > 0f) {
                    scope.launch { drawer.snapTo((drawer.value - available.y / heightPx).coerceIn(0f, 1f)) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                @Suppress("DEPRECATION")
                if (available.y > 0 && source == NestedScrollSource.Drag) {
                    scope.launch { drawer.snapTo((drawer.value - available.y / heightPx).coerceIn(0f, 1f)) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (drawer.value < 1f && drawer.value > 0f) {
                    if (drawer.value < 0.75f || available.y > 1800f) closeDrawer() else drawer.animateTo(1f, spring(dampingRatio = 0.85f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val headerDrag = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { d -> scope.launch { drawer.snapTo((drawer.value - d / heightPx).coerceIn(0f, 1f)) } },
        onDragStopped = { v -> if (drawer.value < 0.8f || v > 1500f) closeDrawer() else drawer.animateTo(1f, spring(dampingRatio = 0.85f)) },
    )

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        e.changes.firstOrNull()?.let { drag.pointer = it.position }
                    }
                }
            },
    ) {
        // Przyciemnienie tła pod szufladą (tapeta prześwituje)
        val progress = drawer.value
        val blurOn = cfg.blurBehindDrawer && Build.VERSION.SDK_INT >= 31 && progress > 0.01f

        // ------------------------------------------------------------- pulpit
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 1f - 0.06f * progress
                    scaleX = s; scaleY = s
                    alpha = 1f - 0.65f * progress
                }
                .then(if (blurOn) Modifier.blur((progress * 22).dp) else Modifier)
                .pointerInput(editMode, cfg.swipeUp, cfg.swipeDown) {
                    var total = 0f
                    var openingDrawer = false
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f; openingDrawer = false },
                        onDragEnd = {
                            when {
                                openingDrawer -> if (drawer.value > 0.25f) openDrawer() else closeDrawer()
                                total > 120f -> runGesture(cfg.swipeDown)
                                total < -120f -> runGesture(cfg.swipeUp)
                            }
                        },
                        onDragCancel = { if (openingDrawer) closeDrawer() },
                    ) { change, dy ->
                        if (editMode || drag.active) return@detectVerticalDragGestures
                        total += dy
                        if (total < 0 && cfg.swipeUp == GestureAction.Drawer) {
                            openingDrawer = true
                            change.consume()
                            scope.launch { drawer.snapTo((-total / (heightPx * 0.6f)).coerceIn(0f, 1f)) }
                        }
                    }
                },
        ) {
            // Postęp wejścia w edycję (0–1): pomniejszenie, karty ekranów, siatka i przyciemnienie
            val edit by animateFloatAsState(if (editMode) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = 420f), label = "edit")
            // Paski edycji podczas przeciągania tylko znikają (bez zmiany układu) – siatka nie skacze pod palcem
            val barsAlpha by animateFloatAsState(if (drag.active) 0f else 1f, label = "editBars")
            if (edit > 0.001f) Box(Modifier.fillMaxSize().graphicsLayer { alpha = edit }.background(Color.Black.copy(alpha = 0.3f)))
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                AnimatedVisibility(
                    editMode,
                    modifier = Modifier.graphicsLayer { alpha = barsAlpha },
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    EditTopBar(
                        page = pagerState.currentPage,
                        count = ws.pages.size,
                        isHome = cfg.defaultPage == pagerState.currentPage,
                        onMoveLeft = { val t = vm.movePage(pagerState.currentPage, -1); scope.launch { pagerState.scrollToPage(t) } },
                        onMoveRight = { val t = vm.movePage(pagerState.currentPage, 1); scope.launch { pagerState.scrollToPage(t) } },
                        onHome = { vm.setDefaultPage(pagerState.currentPage) },
                        onDone = { editMode = false; selected = null },
                    )
                }
                // Lekkie pomniejszenie – sąsiednie ekrany ledwie wystają zza krawędzi
                val editScale = 1f - 0.05f * edit
                // Przy „Losowe” nowy efekt po każdym zatrzymaniu strony, stały w trakcie przesuwania
                var transition by remember { mutableStateOf(cfg.transition) }
                LaunchedEffect(pagerState.settledPage, cfg.transition) {
                    transition = if (cfg.transition == PageTransition.Random) {
                        PageTransition.entries.filter { it != PageTransition.Random && it != transition }.random()
                    } else cfg.transition
                }
                Box(
                    Modifier
                        .weight(1f)
                        // Gesty dwoma palcami: uszczypnięcie, rozsunięcie, przesunięcie w górę/dół.
                        // Faza Initial – przy dwóch palcach ani ikony, ani przewijanie ekranów nie reagują
                        .pointerInput(editMode, cfg.pinchIn, cfg.pinchOut, cfg.twoFingerUp, cfg.twoFingerDown) {
                            val swipe = 90.dp.toPx()
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var startDist = 0f
                                var startCenter = Offset.Zero
                                var tracking = false
                                var fired = false
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                                    val down = ev.changes.filter { it.pressed }
                                    if (down.isEmpty()) break
                                    if (down.size < 2 || editMode || drag.active) continue
                                    val a = down[0].position
                                    val b = down[1].position
                                    val dist = (a - b).getDistance()
                                    val center = (a + b) / 2f
                                    if (!tracking) {
                                        tracking = true; startDist = dist.coerceAtLeast(1f); startCenter = center
                                    } else if (!fired) {
                                        val ratio = dist / startDist
                                        val dy = center.y - startCenter.y
                                        val action = when {
                                            ratio < 0.7f -> cfg.pinchIn
                                            ratio > 1.4f -> cfg.pinchOut
                                            abs(ratio - 1f) < 0.2f && dy < -swipe -> cfg.twoFingerUp
                                            abs(ratio - 1f) < 0.2f && dy > swipe -> cfg.twoFingerDown
                                            else -> null
                                        }
                                        if (action != null) {
                                            fired = true
                                            if (action != GestureAction.None) { vibrate(); runGesture(action) }
                                        }
                                    }
                                    ev.changes.forEach { it.consume() }
                                }
                            }
                        },
                ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = editScale; scaleY = editScale },
                    beyondViewportPageCount = ws.pages.size,
                    userScrollEnabled = !drag.active,
                    key = { i -> ws.pages.getOrNull(i)?.id ?: i },
                ) { i ->
                    val page = ws.pages.getOrNull(i) ?: return@HorizontalPager
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pageTransition(i, pagerState, transition)
                            .then(
                                if (edit > 0.001f) Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.White.copy(alpha = 0.07f * edit))
                                    .border(
                                        if (cfg.defaultPage == i) 1.5.dp else 1.dp,
                                        Color.White.copy(alpha = edit * if (cfg.defaultPage == i) 0.55f else 0.18f),
                                        RoundedCornerShape(28.dp),
                                    )
                                else Modifier
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        DesktopGrid(
                            page = page,
                            cfg = cfg,
                            apps = appMap,
                            labelOf = labelOf,
                            editMode = editMode,
                            editProgress = edit,
                            selectedId = selected,
                            menuItemId = (menu as? MenuTarget.Desktop)?.takeIf { it.pageId == page.id }?.itemId,
                            drag = drag,
                            dropHighlight = dropTarget as? DropTarget.Cell,
                            host = vm.widgetHost,
                            aiEnabled = vm.aiReady,
                            callbacks = callbacks,
                            menu = desktopMenu,
                            onCellSize = { vm.cellSizePx = it },
                        )
                    }
                }
                // Wskaźnik ekranów: nakładka u góry, bez zajmowania miejsca; widać go tylko
                // w trakcie przewijania ekranów i chwilę po nim
                if (cfg.showPageIndicator && ws.pages.size > 1) {
                    var indicatorShown by remember { mutableStateOf(false) }
                    LaunchedEffect(pagerState.isScrollInProgress) {
                        if (pagerState.isScrollInProgress) indicatorShown = true
                        else { delay(700); indicatorShown = false }
                    }
                    val indicatorAlpha by animateFloatAsState(if (indicatorShown || cfg.pageIndicatorAlways) 1f else 0f, label = "pageIndicator")
                    PageIndicator(
                        count = ws.pages.size,
                        current = pagerState.currentPage,
                        fraction = pagerState.currentPageOffsetFraction,
                        homeIndex = cfg.defaultPage,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .graphicsLayer { alpha = indicatorAlpha },
                    )
                }
                EditHint(
                    visible = editMode && !drag.active,
                    hasSelection = selected != null,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                )
                }
                // Dok przesunięty z ustawień – może zejść aż na pasek nawigacji (rysowany nad nim)
                if (cfg.dockEnabled) Box(Modifier.offset(y = cfg.dockOffset.dp).zIndex(1f)) {
                    Dock(
                        keys = ws.dock,
                        apps = appMap,
                        labelOf = labelOf,
                        cfg = cfg,
                        editMode = editMode,
                        drag = drag,
                        menuKey = (menu as? MenuTarget.Dock)?.key,
                        onTap = { app, b -> if (!editMode) launch(app, b) },
                        onLongPress = { app -> requestEdit(MenuTarget.Dock(app.key), title = labelOf(app)) },
                        onDragStart = { app, b -> startDrag(DragSource.Dock, DesktopItem(type = ItemType.App, x = 0, y = 0, app = app.key), null, b) },
                        onDragEnd = { endDrag() },
                        onAdd = { dockPicker = true },
                        menu = dockMenu,
                        onActionTap = ::runAction,
                        onActionLongPress = { a -> requestEdit(MenuTarget.Dock(a.dockKey), title = a.label) },
                        actionMenu = { a ->
                            ActionMenu(
                                title = a.label,
                                onDismiss = { menu = null },
                                actions = if (cfg.layoutLocked) emptyList() else listOf(MenuAction("Usuń z doku", Icons.Rounded.DeleteOutline) { vm.removeFromDock(a.dockKey) }),
                            )
                        },
                    )
                }
                AnimatedVisibility(
                    editMode,
                    modifier = Modifier.graphicsLayer { alpha = barsAlpha },
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                ) {
                    EditToolbar(
                        onWidgets = { widgetPicker = true },
                        onActions = { actionPicker = true },
                        onWallpaper = { SystemActions.openWallpaperPicker(ctx) },
                        onAddPage = { val idx = vm.addPage(pagerState.currentPage); scope.launch { delay(50); pagerState.animateScrollToPage(idx) } },
                        onRemovePage = {
                            val p = ws.pages.getOrNull(pagerState.currentPage)
                            if (p != null && p.items.isEmpty()) vm.removePage(pagerState.currentPage) else confirmRemovePage = pagerState.currentPage
                        },
                        onSettings = { overlay = Overlay.Settings },
                        // Zablokowanie kończy edycję – kolejne przytrzymanie pokaże kłódkę
                        onLock = {
                            vm.updateConfig { it.copy(layoutLocked = true) }
                            menu = null; selected = null; editMode = false
                            vm.toast("Układ zablokowany")
                        },
                    )
                }
                // Dok schodzi nisko – zostaje tylko część paska nawigacji (gest/przyciski nie zachodzą
                // na ikony); w edycji i bez doku pełny odstęp
                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                Spacer(Modifier.height(if (cfg.dockEnabled && !editMode) navBottom * 0.3f else navBottom))
            }
        }

        // ------------------------------------------------------------- szuflada
        val keepDrawer = drag.payload?.source == DragSource.Drawer
        if (progress > 0.001f || keepDrawer) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .graphicsLayer {
                        when (cfg.drawerAnimation) {
                            DrawerAnimation.Slide -> {
                                translationY = (1f - progress) * size.height * 0.9f
                                alpha = (progress * 1.6f).coerceAtMost(1f)
                            }
                            DrawerAnimation.Fade -> {
                                alpha = progress
                                val s = 0.94f + 0.06f * progress
                                scaleX = s; scaleY = s
                            }
                            DrawerAnimation.Zoom -> {
                                alpha = progress
                                val s = 1.18f - 0.18f * progress
                                scaleX = s; scaleY = s
                            }
                        }
                    }
                    .then(
                        if (cfg.drawerBackground) Modifier.background(Color.Black.copy(alpha = cfg.drawerOpacity))
                        else Modifier
                    ),
            ) {
                AppDrawer(
                    vm = vm,
                    cfg = cfg,
                    ws = ws,
                    apps = apps,
                    loaded = loaded,
                    focusRequest = searchFocus,
                    menuKey = (menu as? MenuTarget.Drawer)?.key,
                    onMenuKey = { k -> if (k != null) vibrate(); menu = k?.let { MenuTarget.Drawer(it) } },
                    onLaunch = { app, b -> launch(app, b) },
                    onDragStart = { app, b -> startDrag(DragSource.Drawer, DesktopItem(type = ItemType.App, x = 0, y = 0, app = app.key), null, b) },
                    onDragEnd = { endDrag() },
                    onOpenSettings = { overlay = Overlay.Settings },
                    onOpenCategories = { categoriesFromSettings = false; overlay = Overlay.Categories },
                    closeConnection = closeConnection,
                    headerModifier = headerDrag,
                    menu = drawerMenu,
                )
            }
        }

        // ------------------------------------------------------------- strefy upuszczania i podgląd
        if (drag.active) {
            Box(Modifier.fillMaxWidth().statusBarsPadding().zIndex(15f)) {
                DropZones(drag, dropTarget, drag.payload?.item?.type == ItemType.App)
            }
        }
        drag.payload?.let { p ->
            Box(Modifier.fillMaxSize().zIndex(20f)) { DragPreview(p, drag, appMap, cfg) }
        }

        // ------------------------------------------------------------- kłódka zablokowanego układu
        lockPrompt?.let { lp ->
            Box(Modifier.fillMaxSize().zIndex(25f)) {
                LockPromptOverlay(
                    title = lp.title,
                    hasMenu = lp.menu != null,
                    onDismiss = { lockPrompt = null },
                    onShowMenu = { lockPrompt = null; menu = lp.menu },
                    onUnlock = {
                        vm.updateConfig { it.copy(layoutLocked = false) }
                        lockPrompt = null
                        editMode = true
                        selected = lp.selectId
                    },
                )
            }
        }

        // ------------------------------------------------------------- nakładki
        when (overlay) {
            Overlay.Settings -> Box(Modifier.fillMaxSize().zIndex(30f)) {
                SettingsScreen(vm, onClose = { overlay = Overlay.None }, onOpenCategories = { categoriesFromSettings = true; overlay = Overlay.Categories })
            }
            Overlay.Categories -> Box(Modifier.fillMaxSize().zIndex(30f)) {
                CategoriesScreen(vm, onClose = { closeCategories() })
            }
            Overlay.None -> Unit
        }
    }

    // ------------------------------------------------------------------ dialogi
    openFolder?.let { (pageId, folderId) ->
        val folder = ws.pages.firstOrNull { it.id == pageId }?.items?.firstOrNull { it.id == folderId }
        if (folder == null || folder.type != ItemType.Folder) {
            openFolder = null
        } else {
            FolderDialog(
                folder = folder,
                anchor = folderAnchor,
                apps = appMap,
                labelOf = labelOf,
                cfg = cfg,
                iconSize = with(LocalDensity.current) {
                    // Ten sam rozmiar co ikona aplikacji na pulpicie
                    iconSizeFor(vm.cellSizePx.width.toDp(), vm.cellSizePx.height.toDp(), cfg.desktopIconScale, cfg.showLabels, desktopMode = true)
                },
                cellW = with(LocalDensity.current) { vm.cellSizePx.width.toDp() },
                cellH = with(LocalDensity.current) { vm.cellSizePx.height.toDp() },
                aiEnabled = vm.aiReady,
                onDismiss = { openFolder = null },
                onLaunch = { app, b -> launch(app, b) },
                onRename = { name -> if (name != folder.label) vm.renameItem(pageId, folderId, name) },
                onSuggestName = { set -> vm.suggestFolderName(folder.apps.mapNotNull { appMap[it]?.let(labelOf) }, set) },
                onRemoveApp = { key, toDesktop -> vm.removeFromFolder(pageId, folderId, key, toDesktop) },
                onAddApps = { folderPicker = pageId to folderId },
                onInfo = { vm.openDetails(it) },
            )
        }
    }

    folderPicker?.let { (pageId, folderId) ->
        val folder = ws.pages.firstOrNull { it.id == pageId }?.items?.firstOrNull { it.id == folderId }
        if (folder != null) {
            AppPickerDialog(
                title = "Aplikacje w folderze",
                apps = apps.filter { it.key !in ws.hidden || it.key in folder.apps },
                labelOf = labelOf,
                initial = folder.apps.toSet(),
                shape = cfg.iconShape,
                onDismiss = { folderPicker = null },
                onConfirm = { keys ->
                    if (keys.isEmpty()) vm.removeItem(pageId, folderId)
                    else vm.setFolderApps(pageId, folderId, folder.apps.filter { it in keys } + keys.filter { it !in folder.apps })
                    folderPicker = null
                },
            )
        } else folderPicker = null
    }

    primaryAppPicker?.let { (pageId, folderId) ->
        val folder = ws.pages.firstOrNull { it.id == pageId }?.items?.firstOrNull { it.id == folderId }
        if (folder != null) {
            AppPickerDialog(
                title = "Główna aplikacja folderu",
                apps = folder.apps.mapNotNull { appMap[it] },
                labelOf = labelOf,
                initial = setOfNotNull(folder.primaryApp),
                shape = cfg.iconShape,
                single = true,
                onDismiss = { primaryAppPicker = null },
                onConfirm = { keys ->
                    vm.setFolderPrimaryApp(pageId, folderId, keys.firstOrNull())
                    primaryAppPicker = null
                },
            )
        } else primaryAppPicker = null
    }

    renameFolder?.let { (pageId, folderId) ->
        val folder = ws.pages.firstOrNull { it.id == pageId }?.items?.firstOrNull { it.id == folderId }
        TextInputDialog(
            title = "Nazwa folderu",
            initial = folder?.label.orEmpty(),
            onDismiss = { renameFolder = null },
            extra = if (vm.aiReady && folder != null) { set ->
                TextButton(onClick = { vm.suggestFolderName(folder.apps.mapNotNull { appMap[it]?.let(labelOf) }, set) }) {
                    Text("✨ Zaproponuj nazwę (AI)")
                }
            } else null,
            onConfirm = { name, _ -> vm.renameItem(pageId, folderId, name); renameFolder = null },
        )
    }

    searchStyleFor?.let { (pageId, itemId) ->
        val item = ws.pages.firstOrNull { it.id == pageId }?.items?.firstOrNull { it.id == itemId }
        if (item != null) {
            SearchStyleDialog(
                initial = item.searchStyle,
                aiEnabled = vm.aiReady,
                onDismiss = { searchStyleFor = null },
                onSave = { st -> vm.setSearchStyle(pageId, itemId, st); searchStyleFor = null },
            )
        } else searchStyleFor = null
    }

    renameApp?.let { app ->
        TextInputDialog(
            title = "Nazwa aplikacji",
            initial = labelOf(app),
            onDismiss = { renameApp = null },
            extra = { set -> TextButton(onClick = { set(app.label) }) { Text("Przywróć oryginalną: ${app.label}") } },
            onConfirm = { name, _ -> vm.renameApp(app.key, name); renameApp = null },
        )
    }

    assignFor?.let { app ->
        AssignCategoriesDialog(vm, app, ws, onDismiss = { assignFor = null })
    }

    if (dockPicker) {
        AppPickerDialog(
            title = "Dok (max ${cfg.dockCount})",
            apps = apps,
            labelOf = labelOf,
            initial = ws.dock.toSet(),
            shape = cfg.iconShape,
            actions = SystemAction.entries,
            onDismiss = { dockPicker = false },
            onConfirm = { keys ->
                val ordered = ws.dock.filter { it in keys } + keys.filter { it !in ws.dock }
                vm.store.updateWorkspace { it.copy(dock = ordered.take(cfg.dockCount)) }
                if (ordered.size > cfg.dockCount) vm.toast("Dok mieści ${cfg.dockCount} elementów")
                dockPicker = false
            },
        )
    }

    if (actionPicker) {
        AlertDialog(
            onDismissRequest = { actionPicker = false },
            title = { Text("Skróty akcji") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SystemAction.entries.forEach { a ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(a.icon, null, Modifier.size(22.dp))
                            Text(a.label, Modifier.weight(1f).padding(start = 12.dp))
                            TextButton(onClick = { vm.addActionToDesktop(a, pagerState.currentPage); actionPicker = false }) { Text("Pulpit") }
                            TextButton(onClick = { vm.addToDock(a.dockKey); actionPicker = false }) { Text("Dok") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { actionPicker = false }) { Text("Zamknij") } },
        )
    }

    if (widgetPicker) {
        val providers = remember { vm.providers() }
        WidgetPicker(
            providers = providers,
            spanOf = { vm.widgetSpan(it) },
            onDismiss = { widgetPicker = false },
            onPickBuiltin = { id ->
                vm.addBuiltinWidget(if (id == "clock") ItemType.Clock else ItemType.Search, pagerState.currentPage)
                widgetPicker = false
            },
            onPick = { info ->
                widgetPicker = false
                onAddWidget(info, pagerState.currentPage)
            },
        )
    }

    confirmRemovePage?.let { index ->
        AlertDialog(
            onDismissRequest = { confirmRemovePage = null },
            title = { Text("Usunąć ekran ${index + 1}?") },
            text = { Text("Wszystkie ikony, foldery i widżety z tego ekranu zostaną usunięte z pulpitu. Aplikacje pozostaną zainstalowane.") },
            confirmButton = { TextButton(onClick = { vm.removePage(index); confirmRemovePage = null }) { Text("Usuń", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmRemovePage = null }) { Text("Anuluj") } },
        )
    }

    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = { showOnboarding = false; vm.updateConfig { it.copy(onboardingDone = true) } },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.opendesktop_mark),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Witaj w OpenDesktop")
                }
            },
            text = {
                Text(
                    "Ustaw OpenDesktop jako domyślny ekran główny, aby działały przycisk Home, skróty aplikacji i widżety.\n\n" +
                        "Wskazówki: przesuń w górę – szuflada; przytrzymaj puste miejsce – tryb edycji; przytrzymaj ikonę i przeciągnij – przenoszenie; upuść ikonę na ikonę – folder.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showOnboarding = false
                    vm.updateConfig { it.copy(onboardingDone = true) }
                    ctx.findActivity()?.let { SystemActions.requestDefaultLauncher(it) }
                }) { Text("Ustaw jako domyślny") }
            },
            dismissButton = { TextButton(onClick = { showOnboarding = false; vm.updateConfig { it.copy(onboardingDone = true) } }) { Text("Później") } },
        )
    }

    if (overlay != Overlay.Settings) UpdateDialog(vm, update, onlyAuto = true)
}

@Composable
private fun DropZones(drag: DragController, target: DropTarget?, isApp: Boolean) {
    val source = drag.payload?.source
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val zones = buildList {
            add(DropZone.Remove)
            if (isApp) { add(DropZone.Info); add(DropZone.Uninstall) }
        }
        zones.forEach { z ->
            val active = (target as? DropTarget.Zone)?.zone == z
            val label = if (z == DropZone.Remove && source == DragSource.Drawer) "Ukryj" else z.label
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .onGloballyPositioned { drag.zoneBounds[z] = it.boundsInRoot() }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            active && z == DropZone.Uninstall -> Color(0xCCD32F2F)
                            active -> Color.White.copy(alpha = 0.35f)
                            else -> Color.Black.copy(alpha = 0.35f)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (z) {
                            DropZone.Remove -> if (source == DragSource.Drawer) Icons.Rounded.VisibilityOff else Icons.Rounded.DeleteOutline
                            DropZone.Info -> Icons.Rounded.Info
                            DropZone.Uninstall -> Icons.Rounded.DeleteForever
                        },
                        null, tint = Color.White, modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun EditTopBar(
    page: Int,
    count: Int,
    isHome: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onHome: () -> Unit,
    onDone: () -> Unit,
) {
    // Kompaktowe pigułki zamiast belki na całą szerokość: po lewej numer ekranu z przesuwaniem
    // jego pozycji, po prawej „Główny” i białe „Gotowe”
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.height(44.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Przesuń ekran w lewo", enabled = page > 0, onClick = onMoveLeft)
            Column(Modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ekran", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("${page + 1} / $count", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            TopBarIcon(Icons.AutoMirrored.Rounded.ArrowForward, "Przesuń ekran w prawo", enabled = page < count - 1, onClick = onMoveRight)
        }
        Spacer(Modifier.weight(1f))
        TopBarPill(Icons.Rounded.Home, if (isHome) "Główny" else "Ustaw główny", highlighted = isHome, onClick = onHome)
        TopBarPill(Icons.Rounded.Check, "Gotowe", highlighted = true, onClick = onDone)
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.pointerInput(onClick) { detectTapGestures { onClick() } } else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.3f },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun TopBarPill(icon: ImageVector, label: String, highlighted: Boolean, onClick: () -> Unit) {
    val fg = if (highlighted) Color.Black else Color.White
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) Color.White else Color.Black.copy(alpha = 0.55f))
            .pointerInput(onClick) { detectTapGestures { onClick() } }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun EditToolbar(
    onWidgets: () -> Unit,
    onActions: () -> Unit,
    onWallpaper: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
) {
    // Jedna wspólna belka, przyciski dzielą szerokość po równo – bez przewijania
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        BigToolButton(Icons.Rounded.Widgets, "Widżety", onWidgets)
        BigToolButton(Icons.Rounded.TouchApp, "Akcje", onActions)
        BigToolButton(Icons.Rounded.Wallpaper, "Tapeta", onWallpaper)
        BigToolButton(Icons.Rounded.Add, "Nowy ekran", onAddPage)
        BigToolButton(Icons.Rounded.Delete, "Usuń ekran", onRemovePage)
        BigToolButton(Icons.Rounded.Lock, "Zablokuj", onLock)
        BigToolButton(Icons.Rounded.Settings, "Ustawienia", onSettings)
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, enabled: Boolean = true, highlighted: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.3f))
            .then(if (enabled) Modifier.pointerInput(Unit) { detectTapGestures { onClick() } } else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(20.dp))
        Text(label, color = if (highlighted) MaterialTheme.colorScheme.onPrimary else Color.White, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun RowScope.BigToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    val fg = if (highlighted) Color.Black else Color.White
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) Color.White else Color.Transparent)
            .then(if (enabled) Modifier.pointerInput(onClick) { detectTapGestures { onClick() } } else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = fg, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** Krótka podpowiedź na dole pulpitu w trybie edycji; po kilku sekundach znika. */
@Composable
private fun EditHint(visible: Boolean, hasSelection: Boolean, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible, hasSelection) {
        shown = visible
        if (visible) { delay(3500); shown = false }
    }
    AnimatedVisibility(shown, modifier = modifier, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Text(
            if (hasSelection) "Przeciągnij kropki na krawędziach, aby zmienić rozmiar"
            else "Przytrzymaj i przeciągnij, aby przenieść · stuknij widżet, aby zmienić rozmiar",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/**
 * Kłódka po przytrzymaniu przy zablokowanym układzie. „Odblokuj” otwiera kłódkę (krótka animacja)
 * i dopiero wtedy włącza tryb edycji. Stuknięcie obok zamyka bez zmian.
 */
@Composable
private fun LockPromptOverlay(
    title: String?,
    hasMenu: Boolean,
    onDismiss: () -> Unit,
    onShowMenu: () -> Unit,
    onUnlock: () -> Unit,
) {
    var unlocking by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    LaunchedEffect(unlocking) {
        if (unlocking) { delay(380); onUnlock() }
    }
    val enter by animateFloatAsState(if (appeared) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "lockIn")
    val bounce by animateFloatAsState(if (unlocking) 1.12f else 1f, spring(dampingRatio = 0.35f, stiffness = 600f), label = "lockBounce")
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = enter.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures { if (!unlocking) onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer { val s = 0.85f + 0.15f * enter; scaleX = s; scaleY = s }
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xF21C1C1C))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                // Dotknięcia karty nie zamykają nakładki
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .graphicsLayer { scaleX = bounce; scaleY = bounce }
                    .clip(CircleShape)
                    .background(if (unlocking) Color.White else Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    unlocking,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
                    label = "lockIcon",
                ) { open ->
                    Icon(
                        if (open) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        null,
                        tint = if (open) Color.Black else Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(if (unlocking) "Odblokowano" else "Układ zablokowany", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (title != null) "Odblokuj, aby przenieść lub zmienić „$title”."
                else "Odblokuj, aby przenosić, zmieniać rozmiar i usuwać elementy pulpitu.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasMenu) TopBarPill(Icons.Rounded.MoreHoriz, "Menu", highlighted = false) { if (!unlocking) onShowMenu() }
                TopBarPill(Icons.Rounded.LockOpen, "Odblokuj", highlighted = true) {
                    if (!unlocking) unlocking = true
                }
            }
        }
    }
}
